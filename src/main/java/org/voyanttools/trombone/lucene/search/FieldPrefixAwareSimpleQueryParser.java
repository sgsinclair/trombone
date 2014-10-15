/**
 * 
 */
package org.voyanttools.trombone.lucene.search;


import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.TermRangeQuery;
import org.voyanttools.trombone.model.TokenType;

/**
 * @author sgs
 *
 */
public class FieldPrefixAwareSimpleQueryParser extends SimpleQueryParser {
	
	private static String PREFIX_SEPARATOR = ":";
	private static Pattern RANGE_PATTERN = Pattern.compile("^\\[([\\p{L}0-9]+)-([\\p{L}0-9]+)\\]$");
	
//	private TokenType tokenType = null;

	public FieldPrefixAwareSimpleQueryParser(Analyzer analyzer) {
		this(analyzer, TokenType.lexical);
	}
	
	private FieldPrefixAwareSimpleQueryParser(Analyzer analyzer, TokenType tokenType) {
		super(analyzer,  Collections.singletonMap(tokenType.name(), 1.0F));
	}
	
	public FieldPrefixAwareSimpleQueryParser(Analyzer analyzer,
			Map<String, Float> weights) {
		super(analyzer, weights);
	}

	@Override
  public Query parse(String queryText) {
		// hack to support prefixes in phrases – put the prefix within the quotes
		String modifiedQueryText = queryText.replaceAll("\\b(\\w+):\"","\"$1:");
		return super.parse(modifiedQueryText);
  }
	
	@Override
	protected Query newDefaultQuery(String text) {
		int pos = text.indexOf(PREFIX_SEPARATOR);
		if (pos==-1) {
			Matcher matcher = RANGE_PATTERN.matcher(text); // check to see if we have a range [\w+-\w+]
			return matcher.find() ? newRangeQuery(matcher) : super.newDefaultQuery(text);
		}
		else {
			Matcher matcher = RANGE_PATTERN.matcher(text.substring(pos + 1)); // check to see if we have a range [\w+-\w+]
			return matcher.find() ? newRangeQuery(matcher) : this.createBooleanQuery(text.substring(0, pos), text.substring(pos + 1), Occur.SHOULD);
		}
	}

	@Override
	protected Query newFuzzyQuery(String text, int fuzziness) {
		int pos = text.indexOf(PREFIX_SEPARATOR);
		if (pos==-1) {return super.newFuzzyQuery(text, fuzziness);}
		else {return new FuzzyQuery(new Term(text.substring(0, pos), text.substring(pos + 1)), fuzziness);}
	}

	@Override
	protected Query newPhraseQuery(String text, int slop) {
		int pos = text.indexOf(PREFIX_SEPARATOR);
		if (pos==-1) {return super.newPhraseQuery(text, slop);}
		else {return createPhraseQuery(text.substring(0, pos), text.substring(pos + 1), slop);}
	}

	@Override
	protected Query newPrefixQuery(String text) {
		int pos = text.indexOf(PREFIX_SEPARATOR);
		if (pos==-1) {return super.newPrefixQuery(text);}
		else {return new PrefixQuery(new Term(text.substring(0, pos), text.substring(pos + 1)));}
	}
	
	private Query newRangeQuery(Matcher matcher) {
	    BooleanQuery bq = new BooleanQuery(true);
	    for (Map.Entry<String,Float> entry : weights.entrySet()) {
	    	Query trq = newRangeQuery(entry.getKey(), matcher);
	    	trq.setBoost(entry.getValue());
	    	bq.add(trq, BooleanClause.Occur.SHOULD);
	    }
	    return simplify(bq);
	}
	
	private Query newRangeQuery(String field, Matcher matcher) {
		String start = matcher.group(1);
		String end = matcher.group(2);
		return TermRangeQuery.newStringRange(field, start, end, true, true);
	}

}

/**
 * 
 */
package org.voyanttools.trombone.lucene.search;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.voyanttools.trombone.model.TokenType;

/**
 * @author sgs
 *
 */
public class FieldPrefixAwareSimpleQueryParser extends SimpleQueryParser {
	
	private static String PREFIX_SEPARATOR = ":";
	
	private TokenType tokenType;

	public FieldPrefixAwareSimpleQueryParser(Analyzer analyzer) {
		this(analyzer, TokenType.lexical);
	}
	
	private FieldPrefixAwareSimpleQueryParser(Analyzer analyzer, TokenType tokenType) {
		super(analyzer, tokenType.name());
		this.tokenType = tokenType;
	}
	
	@Override
  public Query parse(String queryText) {
		// hack to support prefixes in phrases – put the prefix within the quotes
		String modifiedQueryText = queryText.replaceAll("\\b(\\w+):\"","\"$1:");
		return super.parse(modifiedQueryText);
  }
	
	@Override
	protected Query newDefaultQuery(String text) {
		return newTermQuery(getTerm(text));
	}

	@Override
	protected Query newFuzzyQuery(String text, int fuzziness) {
		return new FuzzyQuery(getTerm(text), fuzziness);
	}

	@Override
	protected Query newPhraseQuery(String text, int slop) {
		Term term = getTerm(text);
		return this.createPhraseQuery(term.field(), term.text(), slop);
	}

	@Override
	protected Query newPrefixQuery(String text) {
		return new PrefixQuery(getTerm(text));
	}
	
	private Term getTerm(String text) {
		int pos = text.indexOf(PREFIX_SEPARATOR);
		return pos > 0 ? new Term(text.substring(0, pos), text.substring(pos+1)) :  new Term(tokenType.name(), text);
	}

}

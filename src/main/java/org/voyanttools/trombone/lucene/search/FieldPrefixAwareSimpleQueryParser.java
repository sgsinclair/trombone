/**
 * 
 */
package org.voyanttools.trombone.lucene.search;


import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.voyanttools.trombone.model.TokenType;

/**
 * @author sgs
 *
 */
public class FieldPrefixAwareSimpleQueryParser extends SimpleQueryParser {
	
	private static String PREFIX_SEPARATOR = ":";
	private static Pattern RANGE_PATTERN = Pattern.compile("^\\[([\\p{L}0-9]+)-([\\p{L}0-9]+)\\]$");
	protected static TokenType DEFAULT_TOKENTYPE = TokenType.lexical;
	protected IndexReader reader;

	
//	private TokenType tokenType = null;

	public FieldPrefixAwareSimpleQueryParser(IndexReader reader, Analyzer analyzer) {
		this(reader, analyzer, DEFAULT_TOKENTYPE);
	}
	
	public FieldPrefixAwareSimpleQueryParser(IndexReader reader, Analyzer analyzer, TokenType tokenType) {
		super(analyzer,  Collections.singletonMap(tokenType.name(), 1.0F));
		this.reader = reader;
	}
	
	public FieldPrefixAwareSimpleQueryParser(IndexReader reader, Analyzer analyzer, Map<String, Float> weights) {
		super(analyzer, weights);
	}
	
	
	public Map<String, Query> getQueriesMap(String[] queries) {
		Map<String, Query> map = new HashMap<String, Query>();
		for (String query : queries) {
			if (query.trim().isEmpty()) {continue;}
			map.put(query, parse(query));
		}
		return map;
	}

	public Map<String, Query> getQueriesMap(String[] queries, boolean isQueryExpand) throws IOException {
		Map<String, Query> map = new HashMap<String, Query>();
		for (String queryString : queries) {
			if (queryString.trim().isEmpty()) {continue;}
			boolean isReallyQueryExpand = isQueryExpand;
			if (queryString.startsWith("^")) {
				isReallyQueryExpand = true;
				queryString = queryString.substring(1);
			}
			Query query = parse(queryString);
			if (isReallyQueryExpand && query instanceof TermQuery == false) {
				boolean isPrefixNotQuery = query instanceof BooleanQuery && ((BooleanQuery) query).clauses().size()==2 && ((BooleanQuery) query).clauses().get(0).getQuery() instanceof PrefixQuery && ((BooleanQuery) query).clauses().get(1).getQuery() instanceof MatchAllDocsQuery;				if (isPrefixNotQuery) {
					query = ((BooleanQuery) query).clauses().get(0).getQuery();
				}
				if (query instanceof PrefixQuery) {
					// SpanMultiTermQueryWrapper's rewrite method extracts terms properly (PrefixQuery no longer does) 
					SpanOrQuery spanOrQuery = (SpanOrQuery) new SpanMultiTermQueryWrapper<PrefixQuery>((PrefixQuery) query).rewrite(reader);
					for (SpanQuery sq : spanOrQuery.getClauses()) {
						if (isPrefixNotQuery) {
							BooleanQuery bq = new BooleanQuery();
							bq.add(new TermQuery(((SpanTermQuery) sq).getTerm()), Occur.MUST_NOT);
							bq.add(new MatchAllDocsQuery(), Occur.MUST);
							map.put("-"+sq.toString(DEFAULT_TOKENTYPE.name()), bq);
						}
						else {
							map.put(sq.toString(DEFAULT_TOKENTYPE.name()), new TermQuery(((SpanTermQuery) sq).getTerm()));
						}
					}
				}
				else if (query instanceof BooleanQuery) {
					for (BooleanClause bc : ((BooleanQuery) query).getClauses()) {
						map.put(bc.getQuery().toString(DEFAULT_TOKENTYPE.name()), bc.getQuery());
					}
				}
			}
			else {
				map.put(queryString, (Query) query);
			}
			
		}
		return map;
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
	
	protected Query newRangeQuery(Matcher matcher) {
	    BooleanQuery bq = new BooleanQuery(true);
	    for (Map.Entry<String,Float> entry : weights.entrySet()) {
	    	Query trq = newRangeQuery(entry.getKey(), matcher);
	    	trq.setBoost(entry.getValue());
	    	bq.add(trq, BooleanClause.Occur.SHOULD);
	    }
	    return simplify(bq);
	}
	
	protected Query newRangeQuery(String field, Matcher matcher) {
		String start = matcher.group(1);
		String end = matcher.group(2);
		return TermRangeQuery.newStringRange(field, start, end, true, true);
	}

}

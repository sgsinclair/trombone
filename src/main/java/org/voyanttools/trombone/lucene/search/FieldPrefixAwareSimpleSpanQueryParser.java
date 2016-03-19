/**
 * 
 */
package org.voyanttools.trombone.lucene.search;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

/**
 * @author sgs
 *
 */
public class FieldPrefixAwareSimpleSpanQueryParser extends
		FieldPrefixAwareSimpleQueryParser {
	
	/**
	 * @param analyzer
	 * @param weights
	 */
	public FieldPrefixAwareSimpleSpanQueryParser(IndexReader indexReader, Analyzer analyzer) {
		super(indexReader, analyzer);
	}
	
	/**
	 * @param analyzer
	 * @param weights
	 */
	public FieldPrefixAwareSimpleSpanQueryParser(IndexReader indexReader, Analyzer analyzer, String defaultPrefix) {
		super(indexReader, analyzer, defaultPrefix);
	}
	
	@Override
	public Query parse(String queryText) {
		Query query = super.parse(queryText);
		if (query instanceof SpanQuery) {
			return query;
		}
		else if (query instanceof BooleanQuery) {
			SpanOrQuery spanOrQuery = new SpanOrQuery();
			for (BooleanClause bq : ((BooleanQuery) query).clauses()) {
				spanOrQuery.addClause((SpanQuery) bq.getQuery()); 
			}
			return spanOrQuery;
		}
		else {
			throw new IllegalStateException("Cannot convert to SpanQuery: "+query);
		}
	}

	
	public Map<String, SpanQuery> getSpanQueriesMap(String[] queries, boolean isQueryExpand) {
		Map<String, SpanQuery> map = new HashMap<String, SpanQuery>();
		for (String query : queries) {
			if (query.trim().isEmpty()) {continue;}
			boolean isReallyQueryExpand = isQueryExpand;
			if (query.startsWith("^")) {
				isReallyQueryExpand = true;
				query = query.substring(1);
			}
			Query q = parse(query);
			if (isReallyQueryExpand && q instanceof SpanTermQuery == false) {
				if (q instanceof SpanOrQuery) {
					for (SpanQuery spanQuery : ((SpanOrQuery) q).getClauses()) {
						map.put(spanQuery.toString(defaultPrefix), spanQuery);
					}
				}
			}
			else {
				map.put(query, (SpanQuery) q);
			}
			
		}
		return map;
	}
	
	@Override
	protected Query newDefaultQuery(String text) {
		Query query = super.newDefaultQuery(text);
		if (query instanceof BooleanQuery) {
			SpanOrQuery spanOrQuery = new SpanOrQuery();
			for (BooleanClause clause : ((BooleanQuery) query).clauses()) {
				SpanQuery spanQuery = getSpanTermQuery(clause.getQuery());
				spanOrQuery.addClause(spanQuery);
			}
			return spanOrQuery;
		}
		else {
			return getSpanTermQuery(query);
		}
	}
	
	private SpanQuery getSpanTermQuery(Query query) {
		if (query instanceof TermQuery) {
			return new SpanTermQuery(((TermQuery) query).getTerm());
		}
		else if (query instanceof SpanQuery) {
			return (SpanQuery) query;
		}
		else {
			throw new IllegalStateException("Unexpected query type: "+query.getClass().getCanonicalName());
		}
	}

	@Override
	protected Query newFuzzyQuery(String text, int fuzziness) {
		Query query = super.newFuzzyQuery(text, fuzziness);
		if (query instanceof BooleanQuery) {
			SpanOrQuery spanOrQuery = new SpanOrQuery();
			for (BooleanClause clause : ((BooleanQuery) query).clauses()) {
				SpanQuery spanQuery = new SpanMultiTermQueryWrapper<FuzzyQuery>((FuzzyQuery) clause.getQuery());
				spanOrQuery.addClause(spanQuery);
			}
			return spanOrQuery;
		}
		else {
			return new SpanMultiTermQueryWrapper<FuzzyQuery>((FuzzyQuery) query);
		}
	}

	@Override
	protected Query newPhraseQuery(String text, int slop) {
		Query query = super.newPhraseQuery(text, slop);
		if (query instanceof BooleanQuery) {
			SpanOrQuery spanOrQuery = new SpanOrQuery();
			for (BooleanClause clause : ((BooleanQuery) query).clauses()) {
				SpanQuery spanQuery = getSpanNearQuery((PhraseQuery) clause.getQuery());
				spanOrQuery.addClause(spanQuery);
			}
			return spanOrQuery;
		}
		else {
			return getSpanNearQuery((PhraseQuery) query);
		}
	}
	
	private SpanQuery getSpanNearQuery(PhraseQuery query) {
		Term[] terms = query.getTerms();
		SpanQuery[] queries = new SpanQuery[terms.length];
		for (int i=0, len=terms.length; i<len; i++) {
			queries[i] = new SpanTermQuery(terms[i]);
		}
		return new SpanNearQuery(queries, query.getSlop(), false);
	}

	@Override
	protected Query newPrefixQuery(String text) {
		Query query = super.newPrefixQuery(text);
		if (query instanceof BooleanQuery) {
			SpanOrQuery spanOrQuery = new SpanOrQuery();
			for (BooleanClause clause : ((BooleanQuery) query).clauses()) {
				SpanQuery spanQuery =getQuery((PrefixQuery) clause.getQuery());
				spanOrQuery.addClause(spanQuery);
			}
			return spanOrQuery;
		}
		else {
			return getQuery((PrefixQuery) query);
		}
	}
	
	private SpanQuery getQuery(PrefixQuery query) {
		try {
			return (SpanQuery) new SpanMultiTermQueryWrapper<PrefixQuery>((PrefixQuery) query).rewrite(reader);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to expand queries from Lucene index for query: "+query.toString());
		}
	}

	protected Query newRangeQuery(Matcher matcher) {
		Query query = super.newRangeQuery(matcher);
		if (query instanceof BooleanQuery) {
			SpanOrQuery spanOrQuery = new SpanOrQuery();
			for (BooleanClause clause : ((BooleanQuery) query).clauses()) {
				SpanQuery spanQuery = new SpanMultiTermQueryWrapper<TermRangeQuery>((TermRangeQuery) clause.getQuery());
				spanOrQuery.addClause(spanQuery);
			}
			return spanOrQuery;
		}
		else {
			return new SpanMultiTermQueryWrapper<TermRangeQuery>((TermRangeQuery) query);
		}
	}
}

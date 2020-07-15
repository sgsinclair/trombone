/**
 * 
 */
package org.voyanttools.trombone.lucene.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
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
			if (query instanceof SpanNearQuery) {
				// if the original query doesn't specify a slop, make sure words are in order
				if (queryText.contains("~")==false) {
					query = new SpanNearQuery(((SpanNearQuery) query).getClauses(), ((SpanNearQuery) query).getSlop(), true);
				}
				SpanQuery[] spanQueries = ((SpanNearQuery) query).getClauses();
				if (spanQueries.length==1) {return spanQueries[0];}
			}
			if (query instanceof SpanOrQuery) {
				SpanQuery[] spanQueries = ((SpanOrQuery) query).getClauses();
				if (spanQueries.length==1) {return spanQueries[0];}
			}
			return query;
		}
		else if (query instanceof BooleanQuery) {
			return convertBooleanQuerytoSpanQuery((BooleanQuery) query, queryText);
		}
		else {
			throw new IllegalStateException("Cannot convert to SpanQuery: "+query);
		}
	}

	private SpanQuery convertBooleanQuerytoSpanQuery(BooleanQuery query, String queryText) {
		List<SpanQuery> spanQueries = new ArrayList<SpanQuery>();
		List<SpanQuery> notQueries = new ArrayList<SpanQuery>();
		boolean hasMatchAllDocs = false;
		for (BooleanClause bq : ((BooleanQuery) query).clauses()) {
			Query q = bq.getQuery();
			if (q instanceof SpanQuery) {
				if (((Object) q instanceof SpanOrQuery || (Object) q instanceof SpanTermQuery) && bq.getOccur()==BooleanClause.Occur.MUST_NOT) {
					notQueries.add((SpanQuery) q);
				} else {
					spanQueries.add((SpanQuery) bq.getQuery());
				}
			} else if (q instanceof MatchAllDocsQuery) {
				hasMatchAllDocs = true;
			} else if (q instanceof BooleanQuery) {
				SpanQuery sq = convertBooleanQuerytoSpanQuery((BooleanQuery) q, queryText);
				
				spanQueries.add(convertBooleanQuerytoSpanQuery((BooleanQuery) q, queryText)); 
			} else {
				throw new IllegalArgumentException("Unable to parse query: "+queryText+", unanticipated query type: "+q.getClass().getName());
			}
		}
		
		SpanQuery combined = new SpanOrQuery(spanQueries.toArray(new SpanQuery[spanQueries.size()]));
		if (notQueries.isEmpty()) {
			List<SpanQuery> ors = new ArrayList<SpanQuery>();
			List<SpanNotQuery> nots = new ArrayList<SpanNotQuery>();
			for (SpanQuery q : ((SpanOrQuery) combined).getClauses()) {
				if (q instanceof SpanNotQuery) {nots.add((SpanNotQuery) q);}
				else {ors.add(q);}
			}
			if (nots.isEmpty()) {return combined;}
			else {
				if (ors.size()==1) {
					// FIXME: this only uses one not
					return new SpanNotQuery(ors.get(0), nots.get(0).getExclude());
				} else {
					// FIXME: this only uses one not
					return new SpanNotQuery(new SpanOrQuery(ors.toArray(new SpanQuery[ors.size()])), nots.get(0).getExclude());
				}
			}
		} else {
			return new SpanNotQuery(combined, notQueries.get(0));
		}
	}
	
	public Map<String, SpanQuery> getSpanQueriesMap(String[] queries, boolean isQueryExpand) throws IOException {
		Map<String, SpanQuery> map = new HashMap<String, SpanQuery>();
		for (String query : queries) {
			if (query.trim().isEmpty()) {continue;}
			if (containsAlphaNumeric(query)==false) {continue;}
			boolean isReallyQueryExpand = isQueryExpand;
			if (query.startsWith("^") && query.startsWith("^@")==false) {
				isReallyQueryExpand = true;
				query = query.substring(1);
			}
			Query q = parse(query);
			if (isReallyQueryExpand && q instanceof SpanTermQuery == false) {
				if (q instanceof SpanOrQuery) {
					IndexSearcher searcher = new IndexSearcher(reader);
					int count = 0;
					for (SpanQuery spanQuery : ((SpanOrQuery) q).getClauses()) {
						// we need to double-check that this term is in the corpus (the query rewrite method includes all terms)
						if (searcher.search(spanQuery, 1).totalHits.value>0) {
							map.put(spanQuery.toString(defaultPrefix), spanQuery);
							count++;
						}
					}
					if (count==0) {
						map.put(query, (SpanOrQuery) q);
					}
				}
			}
			else {
				if (q instanceof SpanOrQuery) {
					SpanOrQuery orq = (SpanOrQuery) q;
					
					if (orq.getClauses().length>0) {
						// check if it looks like an and query: +this +that
						if (StringUtils.countMatches(query,"+") == orq.getClauses().length) {
							// create an AND query by having a huge slop TODO: is this an important performance hit?
							q = new SpanNearQuery(orq.getClauses(), Integer.MAX_VALUE, false);
						}
						
						// check to see if we have a bare phrase (no quotes, no or operator but still SpanOr)
						else if (query.indexOf(" ")>-1 && query.indexOf("|")==-1 && query.indexOf("\"")==-1) {
							q = new SpanNearQuery(orq.getClauses(), 0, true);
							query = "\""+query+"\"";
						}
					}
				}
				map.put(query, (SpanQuery) q);
			}
			
		}
		return map;
	}
	
	@Override
	protected Query newDefaultQuery(String text) {
		Query query = super.newDefaultQuery(text);
		if (query instanceof BooleanQuery) {
			List<SpanQuery> spanQueries = new ArrayList<SpanQuery>();
			for (BooleanClause bq : ((BooleanQuery) query).clauses()) {
				spanQueries.add(getSpanTermQuery(bq.getQuery()));
			}
			return new SpanOrQuery(spanQueries.toArray(new SpanQuery[spanQueries.size()]));
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
			List<SpanQuery> spanQueries = new ArrayList<SpanQuery>();
			for (BooleanClause bq : ((BooleanQuery) query).clauses()) {
				SpanQuery spanQuery = new SpanMultiTermQueryWrapper<FuzzyQuery>((FuzzyQuery) bq.getQuery());
				spanQueries.add(spanQuery);
			}
			return new SpanOrQuery(spanQueries.toArray(new SpanQuery[spanQueries.size()]));
		}
		else {
			return new SpanMultiTermQueryWrapper<FuzzyQuery>((FuzzyQuery) query);
		}
	}

	@Override
	protected Query newPhraseQuery(String text, int slop) {
		Query query = super.newPhraseQuery(text, slop);
		if (query instanceof BooleanQuery) {
			List<SpanQuery> spanQueries = new ArrayList<SpanQuery>();
			for (BooleanClause bq : ((BooleanQuery) query).clauses()) {
				SpanQuery spanQuery = getSpanNearQuery((PhraseQuery) bq.getQuery());
				spanQueries.add(spanQuery);
			}
			return new SpanOrQuery(spanQueries.toArray(new SpanQuery[spanQueries.size()]));
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
			List<SpanQuery> spanQueries = new ArrayList<SpanQuery>();
			for (BooleanClause bq : ((BooleanQuery) query).clauses()) {
				SpanQuery spanQuery = getQuery((PrefixQuery) bq.getQuery());
				spanQueries.add(spanQuery);
			}
			return new SpanOrQuery(spanQueries.toArray(new SpanQuery[spanQueries.size()]));
		} else if (query instanceof SpanOrQuery) {
			return query;
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

	protected Query newRegexQuery(String text) throws IOException {
		Query query = super.newRegexQuery(text);
		if (query instanceof BooleanQuery) {
			List<SpanQuery> spanQueries = new ArrayList<SpanQuery>();
			for (BooleanClause bq : ((BooleanQuery) query).clauses()) {
				SpanQuery spanQuery = new SpanMultiTermQueryWrapper<RegexpQuery>((RegexpQuery) bq.getQuery());
				spanQueries.add(spanQuery);
			}
			return new SpanOrQuery(spanQueries.toArray(new SpanQuery[spanQueries.size()]));
		}
		else {
			Query spanRegexQuery = new SpanMultiTermQueryWrapper<RegexpQuery>((RegexpQuery) query);
			return spanRegexQuery.rewrite(reader);
		}
	}
	
	protected Query newRangeQuery(String text) throws IOException {
		Query query = super.newRangeQuery(text);
		if (query instanceof BooleanQuery) {
			List<SpanQuery> spanQueries = new ArrayList<SpanQuery>();
			for (BooleanClause bq : ((BooleanQuery) query).clauses()) {
				SpanQuery spanQuery = new SpanMultiTermQueryWrapper<TermRangeQuery>((TermRangeQuery) bq.getQuery());
				Query q = spanQuery.rewrite(reader);
				spanQueries.add((SpanQuery) q);
			}
			return new SpanOrQuery(spanQueries.toArray(new SpanQuery[spanQueries.size()]));
		}
		else {
			Query rangeQuery = new SpanMultiTermQueryWrapper<TermRangeQuery>((TermRangeQuery) query);
			return rangeQuery.rewrite(reader);
		}
	}
}

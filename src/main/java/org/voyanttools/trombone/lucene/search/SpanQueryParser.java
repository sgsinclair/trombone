package org.voyanttools.trombone.lucene.search;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.voyanttools.trombone.model.TokenType;

public class SpanQueryParser extends AbstractQueryParser {
		
	public SpanQueryParser(IndexReader indexReader, Analyzer analyzer) {
		super(indexReader, analyzer);
	}

	public Map<String, SpanQuery> getSpanQueriesMap(String[] queries, TokenType tokenType, boolean collapse) throws IOException {
		Map<String, SpanQuery> spanQueriesMap = new HashMap<String, SpanQuery>();
		for(Map.Entry<String, Query> entries : getQueriesMap(queries, tokenType, collapse).entrySet()) {
			spanQueriesMap.put(entries.getKey(), (SpanQuery) entries.getValue());
		}
		return spanQueriesMap;
	}

	
	@Override
	protected Query getNearQuery(Query[] queries, int slop, boolean inOrder) {
		SpanQuery[] spanQueries = new SpanQuery[queries.length];
		for (int i=0, len=queries.length; i<len; i++) {spanQueries[i]=(SpanQuery) queries[i];}
		return new SpanNearQuery(spanQueries, slop, inOrder);
	}


	@Override
	protected Query getTermQuery(Term term) {
		return new SpanTermQuery(term);
	}

	@Override
	protected Query getBooleanQuery(Map<String, Query> queriesMap) throws IOException {
		// note that we don't support and queries for spans
		SpanOrQuery spanOrQuery = new SpanOrQuery();
		for (Query query : queriesMap.values()) {
			spanOrQuery.addClause((SpanQuery) query);
		}
		return spanOrQuery;
	}

	@Override
	protected Query getWildCardQuery(Term term) throws IOException {
		WildcardQuery wildCardQuery = new WildcardQuery(term);
		Query query = new SpanMultiTermQueryWrapper<WildcardQuery>(wildCardQuery);
		return query.rewrite(indexReader);
	}

}

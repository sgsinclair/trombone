/**
 * 
 */
package org.voyanttools.trombone.lucene.search;


import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

/**
 * @author sgs
 *
 */
public class FlexibleQueryParser extends AbstractQueryParser {

	/**
	 * @param analyzer
	 */
	public FlexibleQueryParser(IndexReader indexReader, Analyzer analyzer) {
		super(indexReader, analyzer);
	}

	@Override
	protected Query getNearQuery(Query[] queries, int slop, boolean inOrder) {
		PhraseQuery phraseQuery = new PhraseQuery();
		phraseQuery.setSlop(slop);
		for(Query query : queries) {
			Set<Term> terms = new HashSet<Term>();
			query.extractTerms(terms);
			for(Term term : terms) {phraseQuery.add(term);}
		}
		return phraseQuery;
	}

	@Override
	protected Query getTermQuery(Term term) {
		return new TermQuery(term);
	}

	@Override
	protected Query getBooleanQuery(Map<String, Query> queriesMap) throws IOException {
		BooleanQuery query = new BooleanQuery();
		for (Map.Entry<String, Query> entry : queriesMap.entrySet()) {
			String key = entry.getKey();
			Query q = entry.getValue();
			if (key.startsWith("+")) {query.add(q, Occur.MUST);}
			else if (key.startsWith("-")) {query.add(q, Occur.MUST_NOT);}
			else {query.add(q, Occur.SHOULD);}
		}
		return query;
	}

	@Override
	protected Query getWildCardQuery(Term term) throws IOException {
		//Not sure why the code below doesn't work, but we'll use the span query rewrite mechanism because that does work
		/*
		MultiTermQuery query = new WildcardQuery(term);
		query.setRewriteMethod(MultiTermQuery.CONSTANT_SCORE_FILTER_REWRITE);
		Query q = query.rewrite(indexReader);
		Set<Term> terms = new HashSet<Term>();
		q.extractTerms(terms);
		return query.rewrite(indexReader);
		*/
		SpanQueryParser spanQueryParser = new SpanQueryParser(indexReader, analyzer);
		Query spanQuery = spanQueryParser.getWildCardQuery(term);
		Set<Term> terms = new HashSet<Term>();
		spanQuery.extractTerms(terms);
		BooleanQuery booleanQuery = new BooleanQuery();
		for (Term t : terms) {
			if (terms.size()==1) {return new TermQuery(t);}
			booleanQuery.add(new TermQuery(t), Occur.SHOULD);
		}
		return booleanQuery;
	}


}

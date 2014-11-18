package org.voyanttools.trombone.lucene.queries;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.voyanttools.trombone.model.Corpus;

public class CorpusFilter extends Filter {
	
	private Filter filter;

	public CorpusFilter(Corpus corpus) {
		Term term = new Term("corpus", corpus.getId());
		TermFilter termFilter = new TermFilter(term);
		filter = new CachingWrapperFilter(termFilter);
	}

	@Override
	public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs)
			throws IOException {
		return filter.getDocIdSet(context, acceptDocs);
	}

}

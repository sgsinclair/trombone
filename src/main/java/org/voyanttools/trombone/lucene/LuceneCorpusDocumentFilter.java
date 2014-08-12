/**
 * 
 */
package org.voyanttools.trombone.lucene;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.BitsFilteredDocIdSet;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.OpenBitSet;
import org.voyanttools.trombone.model.Corpus;

/**
 * @author sgs
 *
 */
public class LuceneCorpusDocumentFilter extends Filter {
	
	private OpenBitSet openBitSet;

	
	/**
	 * 
	 */
	public LuceneCorpusDocumentFilter(OpenBitSet openBitSet) {
		this.openBitSet = openBitSet;
	}

	/* (non-Javadoc)
	 * @see org.apache.lucene.search.Filter#getDocIdSet(org.apache.lucene.index.AtomicReaderContext, org.apache.lucene.util.Bits)
	 */
	@Override
	public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
		return BitsFilteredDocIdSet.wrap(openBitSet, new Bits.MatchAllBits(openBitSet.length()));
	}

}

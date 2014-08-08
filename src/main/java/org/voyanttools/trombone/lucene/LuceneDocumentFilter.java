/**
 * 
 */
package org.voyanttools.trombone.lucene;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;

/**
 * @author sgs
 *
 */
public class LuceneDocumentFilter extends Filter {

	/**
	 * 
	 */
	public LuceneDocumentFilter() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.apache.lucene.search.Filter#getDocIdSet(org.apache.lucene.index.AtomicReaderContext, org.apache.lucene.util.Bits)
	 */
	@Override
	public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}

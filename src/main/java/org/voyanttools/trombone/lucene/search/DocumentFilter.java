/**
 * 
 */
package org.voyanttools.trombone.lucene.search;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.Bits;
import org.voyanttools.trombone.lucene.CorpusMapper;

/**
 * @author sgs
 *
 */
@Deprecated
public class DocumentFilter  {
	
	private DocIdSet docIdSet;
	private String id;
	private int length;

	/**
	 * @param bitSet 
	 * @throws IOException 
	 * 
	 */
	public DocumentFilter(CorpusMapper corpusMapper) throws IOException {
		docIdSet = new BitDocIdSet(corpusMapper.getBitSet());
		id = corpusMapper.getCorpus().getId();
		length = corpusMapper.getCorpus().size();
	}

	/* (non-Javadoc)
	 * @see org.apache.lucene.search.Filter#getDocIdSet(org.apache.lucene.index.LeafReaderContext, org.apache.lucene.util.Bits)
	 */
//	@Override
//	public DocIdSet getDocIdSet(LeafReaderContext context, Bits acceptDocs) throws IOException {
//		return docIdSet;
//	}

	/* (non-Javadoc)
	 * @see org.apache.lucene.search.Query#toString(java.lang.String)
	 */
//	@Override
//	public String toString(String field) {
//		return "filter for corpus "+id+" ("+ String.valueOf(length)+" documents)";
//	}

}

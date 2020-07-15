/**
 * 
 */
package org.voyanttools.trombone.lucene.search;

import java.io.IOException;

import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.FixedBitSet;

/**
 * @author sgs
 *
 */
public class DocumentsFacetsCollector extends FacetsCollector {
	
	DocIdSet documentBits;

	public DocumentsFacetsCollector(DocIdSet bitSet) {
		super(true);
		documentBits = bitSet;
	}

//	@Override
//	  protected Docs createDocs(final int maxDoc) {
//		    return new Docs() {
//		      private final FixedBitSet bits = new FixedBitSet(maxDoc);
//		      
//		      @Override
//		      public void addDoc(int docId) throws IOException {
//		    	  if (documentBits.get(docId)) {
//				        bits.set(docId);
//		    	  }
//		      }
//		      
//		      @Override
//		      public DocIdSet getDocIdSet() {
//		        return new BitDocIdSet(bits);
//		      }
//		    };
//		  }
}

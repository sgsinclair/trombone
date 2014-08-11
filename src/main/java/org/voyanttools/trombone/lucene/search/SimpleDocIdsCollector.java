/**
 * 
 */
package org.voyanttools.trombone.lucene.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.TotalHitCountCollector;

/**
 * @author sgs
 *
 */
public class SimpleDocIdsCollector extends TotalHitCountCollector {
	
	List<Integer> docIds = new ArrayList<Integer>();

	/**
	 * 
	 */
	public SimpleDocIdsCollector() {
		// TODO Auto-generated constructor stub
	}
	
	public void collect(int doc) {
		docIds.add(doc);
		super.collect(doc);
	}
	
	public List<Integer> getDocIds() {
		return docIds;
	}

}

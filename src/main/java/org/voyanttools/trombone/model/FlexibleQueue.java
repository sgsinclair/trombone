/**
 * 
 */
package org.voyanttools.trombone.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.PriorityQueue;

/**
 * @author sgs
 *
 */
public class FlexibleQueue<T> {
	
	Comparator<T> comparator;
	
	LuceneQueue<T> luceneQueue = null;
	
	List<T> list = null;

	/**
	 * 
	 */
	public FlexibleQueue(Comparator<T> comparator) {
		this(comparator, ArrayUtil.MAX_ARRAY_LENGTH);
	}
	
	/**
	 * 
	 */
	public FlexibleQueue(Comparator<T> comparator, int size) {
		this.comparator = comparator;
		if (size < ArrayUtil.MAX_ARRAY_LENGTH) {luceneQueue = new LuceneQueue(comparator, size);}
		else {list = new ArrayList<T>();}
	}

	public void offer(T element) {
		if (list!=null) {list.add(element);}
		else if (luceneQueue!=null) {luceneQueue.insertWithOverflow(element);}
	}

	public List<T> getList() {
		
		if (list!=null) {
			Collections.sort(list, comparator);
		}
		
		if (luceneQueue!=null) {
			list = new ArrayList<T>();
			for (int i=0, len=luceneQueue.size(); i<len; i++) {
				list.add(luceneQueue.pop());
			}
			Collections.reverse(list);
		}
		return list;
	}
	
	private class LuceneQueue<T> extends PriorityQueue<T> {
		
		Comparator<T> comparator;

		private LuceneQueue(Comparator<T> comparator, int maxSize) {
			super(maxSize);
			this.comparator = comparator;
		}
		
		@Override
		protected boolean lessThan(T a, T b) {
			return comparator.compare(b, a) < 0;
		}
		
	}

}

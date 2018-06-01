/**
 * 
 */
package org.voyanttools.trombone.util;

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
		if (size>0 && size<100000) { // a bit arbitrary, a queue is faster, but all values need to be initialized, so a memory hog
			luceneQueue = new LuceneQueue(comparator, size);
		}
		else {
			list = new ArrayList<T>();
		}
	}

	public void offer(T element) {
		if (list!=null) {list.add(element);}
		else if (luceneQueue!=null) {luceneQueue.insertWithOverflow(element);}
	}

	public List<T> getOrderedList() {
		return getOrderedList(0);
	}
	
	public List<T> getOrderedList(int start) {
		
		if (list!=null) {
			if (start>=list.size()) { // nothing beyond start, clear and return
				list.clear();
				return list;
			}
			Collections.sort(list, comparator);
		}
		
		if (luceneQueue!=null) {
			list = new ArrayList<T>();
			if (start>=luceneQueue.size()) { // nothing beyond start, return empty
				return list;
			}
			for (int i=0, len=luceneQueue.size(); i<len; i++) {
				
				list.add(luceneQueue.pop());
			}
			Collections.reverse(list);
		}
		// provide a sublist if need be
		return start>0 ? new ArrayList<T>(list.subList(start, list.size())) : list;
	}
	
	public List<T> getUnorderedList() {
		
		if (luceneQueue!=null) {
			list = new ArrayList<T>();
			for (Object o : luceneQueue.getHeap()) {
				list.add((T) o);
			}
		}
		return list;
	}
	
	private class LuceneQueue<T> extends PriorityQueue<T> {
		
		Comparator<T> comparator;

		private LuceneQueue(Comparator<T> comparator, int maxSize) {
			super(maxSize);
			this.comparator = comparator;
		}
		
		private Object[] getHeap() {
			return getHeapArray();
		}

		@Override
		protected boolean lessThan(T a, T b) {
			return comparator.compare(b, a) < 0;
		}
		
	}

}

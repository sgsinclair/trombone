package org.voyanttools.trombone.tool.analysis;

import java.util.Comparator;

import org.voyanttools.trombone.model.CorpusTerm;

/**
 * This is essentially a priority queue for {@link CorpusTerm}. This is somewhat
 * optimized for memory and performance when a size is given. Don't specify an arbitrarily
 * large size, either use the constructor without a size or specify {@link Integer#MAX_VALUE}.
 * 
 * @author Stéfan Sinclair
 * @since 4.0
 */
public class CorpusTermsQueue {
	
	// used when a size is given – use the Lucene implementation for better memory management (only top items are kept)
	private org.apache.lucene.util.PriorityQueue<CorpusTerm> limitedSizeQueue = null;
	
	// use the Java implementation to allow the queue to grow arbitrarily big
	private java.util.PriorityQueue<CorpusTerm> unlimitedSizeQueue = null;
	

	public CorpusTermsQueue(CorpusTerm.Sort sort) {
		this(Integer.MAX_VALUE, sort);
	}

	public CorpusTermsQueue(int size, CorpusTerm.Sort sort) {
		Comparator<CorpusTerm> comparator = CorpusTerm.getComparator(sort);
		if (size==Integer.MAX_VALUE) {
			unlimitedSizeQueue = new java.util.PriorityQueue<CorpusTerm>(11, comparator);
		}
		else {
			limitedSizeQueue = new LimitedSizeQueue<CorpusTerm>(size, comparator);
		}
	}

	private class LimitedSizeQueue<CorpusTerm> extends org.apache.lucene.util.PriorityQueue<CorpusTerm> {

		Comparator<CorpusTerm> comparator;
		
		public LimitedSizeQueue(int maxSize, Comparator<CorpusTerm> comparator) {
			super(maxSize);
			this.comparator = comparator;
		}

		@Override
		protected boolean lessThan(CorpusTerm a, CorpusTerm b) {
			return comparator.compare(a, b) < 0;
		}
		
	}

	public void offer(CorpusTerm corpusTerm) {
		if (limitedSizeQueue!=null) {limitedSizeQueue.insertWithOverflow(corpusTerm);}
		else if (unlimitedSizeQueue!=null) {unlimitedSizeQueue.offer(corpusTerm);}
	}

	public int size() {
		if (limitedSizeQueue!=null) {return limitedSizeQueue.size();}
		else if (unlimitedSizeQueue!=null) {return unlimitedSizeQueue.size();}
		return 0;
	}

	public CorpusTerm poll() {
		if (limitedSizeQueue!=null) {return limitedSizeQueue.pop();}
		else if (unlimitedSizeQueue!=null) {return unlimitedSizeQueue.poll();}
		return null;
	}
}

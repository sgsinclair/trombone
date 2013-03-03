package org.voyanttools.trombone.tool.analysis.corpus;

import java.util.Comparator;

/**
 * This is essentially a priority queue for {@link CorpusTermFrequencyStats}. This is somewhat
 * optimized for memory and performance when a size is given. Don't specify an arbitrarily
 * large size, either use the constructor without a size or specify {@link Integer#MAX_VALUE}.
 * 
 * @author Stéfan Sinclair
 * @since 4.0
 */
public class CorpusTermFrequencyStatsQueue {
	
	// used when a size is given – use the Lucene implementation for better memory management (only top items are kept)
	private org.apache.lucene.util.PriorityQueue<CorpusTermFrequencyStats> limitedSizeQueue = null;
	
	// use the Java implementation to allow the queue to grow arbitrarily big
	private java.util.PriorityQueue<CorpusTermFrequencyStats> unlimitedSizeQueue = null;
	

	public CorpusTermFrequencyStatsQueue(CorpusTermFrequencyStatsSort sort) {
		this(Integer.MAX_VALUE, sort);
	}

	public CorpusTermFrequencyStatsQueue(int size, CorpusTermFrequencyStatsSort sort) {
		Comparator<CorpusTermFrequencyStats> comparator = CorpusTermFrequencyStats.getComparator(sort);
		if (size==Integer.MAX_VALUE) {
			unlimitedSizeQueue = new java.util.PriorityQueue<CorpusTermFrequencyStats>(11, comparator);
		}
		else {
			limitedSizeQueue = new LimitedSizeQueue<CorpusTermFrequencyStats>(size, comparator);
		}
	}

	private class LimitedSizeQueue<CorpusTermFrequencyStats> extends org.apache.lucene.util.PriorityQueue<CorpusTermFrequencyStats> {

		Comparator<CorpusTermFrequencyStats> comparator;
		
		public LimitedSizeQueue(int maxSize, Comparator<CorpusTermFrequencyStats> comparator) {
			super(maxSize);
			this.comparator = comparator;
		}

		@Override
		protected boolean lessThan(CorpusTermFrequencyStats a,
				CorpusTermFrequencyStats b) {
			return comparator.compare(a, b) < 0;
		}
		
	}

	public void offer(CorpusTermFrequencyStats corpusTermFrequencyStats) {
		if (limitedSizeQueue!=null) {limitedSizeQueue.insertWithOverflow(corpusTermFrequencyStats);}
		else if (unlimitedSizeQueue!=null) {unlimitedSizeQueue.offer(corpusTermFrequencyStats);}
	}

	public int size() {
		if (limitedSizeQueue!=null) {return limitedSizeQueue.size();}
		else if (unlimitedSizeQueue!=null) {return unlimitedSizeQueue.size();}
		return 0;
	}

	public CorpusTermFrequencyStats poll() {
		if (limitedSizeQueue!=null) {return limitedSizeQueue.pop();}
		else if (unlimitedSizeQueue!=null) {return unlimitedSizeQueue.poll();}
		return null;
	}
}

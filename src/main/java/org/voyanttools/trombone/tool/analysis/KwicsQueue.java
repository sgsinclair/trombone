/*******************************************************************************
 * Trombone is a flexible text processing and analysis library used
 * primarily by Voyant Tools (voyant-tools.org).
 * 
 * Copyright (©) 2007-2012 Stéfan Sinclair & Geoffrey Rockwell
 * 
 * This file is part of Trombone.
 * 
 * Trombone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.tool.analysis;

import java.util.Comparator;

import org.voyanttools.trombone.model.Kwic;
import org.voyanttools.trombone.model.Kwic.Sort;

/**
 * @author sgs
 *
 */
public class KwicsQueue {

	// used when a size is given – use the Lucene implementation for better memory management (only top items are kept)
	private org.apache.lucene.util.PriorityQueue<Kwic> limitedSizeQueue = null;
	
	// use the Java implementation to allow the queue to grow arbitrarily big
	private java.util.PriorityQueue<Kwic> unlimitedSizeQueue = null;

	/**
	 * 
	 */
	public KwicsQueue(Kwic.Sort sort) {
		this(Integer.MAX_VALUE, sort);
	}
	
	

	public KwicsQueue(int size, Sort sort) {
		Comparator<Kwic> comparator = Kwic.getComparator(sort);
		if (size==Integer.MAX_VALUE) {
			unlimitedSizeQueue = new java.util.PriorityQueue<Kwic>(11, comparator);
		}
		else {
			limitedSizeQueue = new LimitedSizeQueue<Kwic>(size, comparator);
		}
	}



	private class LimitedSizeQueue<Kwic> extends org.apache.lucene.util.PriorityQueue<Kwic> {

		Comparator<Kwic> comparator;
		
		public LimitedSizeQueue(int maxSize, Comparator<Kwic> comparator) {
			super(maxSize);
			this.comparator = comparator;
		}

		@Override
		protected boolean lessThan(Kwic a, Kwic b) {
			return comparator.compare(a, b) < 0;
		}
		
	}

	public void offer(Kwic kwic) {
		if (limitedSizeQueue!=null) {limitedSizeQueue.insertWithOverflow(kwic);}
		else if (unlimitedSizeQueue!=null) {unlimitedSizeQueue.offer(kwic);}
	}

	public int size() {
		if (limitedSizeQueue!=null) {return limitedSizeQueue.size();}
		else if (unlimitedSizeQueue!=null) {return unlimitedSizeQueue.size();}
		return 0;
	}

	public Kwic poll() {
		if (limitedSizeQueue!=null) {return limitedSizeQueue.pop();}
		else if (unlimitedSizeQueue!=null) {return unlimitedSizeQueue.poll();}
		return null;
	}

}

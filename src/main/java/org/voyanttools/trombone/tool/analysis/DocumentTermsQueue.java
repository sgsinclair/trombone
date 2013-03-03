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

import org.voyanttools.trombone.model.DocumentTerm;

/**
 * @author sgs
 *
 */
public class DocumentTermsQueue {
	
	// used when a size is given – use the Lucene implementation for better memory management (only top items are kept)
	private org.apache.lucene.util.PriorityQueue<DocumentTerm> limitedSizeQueue = null;
	
	// use the Java implementation to allow the queue to grow arbitrarily big
	private java.util.PriorityQueue<DocumentTerm> unlimitedSizeQueue = null;

	
	public DocumentTermsQueue(DocumentTerm.Sort sort) {
		this(Integer.MAX_VALUE, sort);
	}
	
	public DocumentTermsQueue(int size, DocumentTerm.Sort sort) {
		Comparator<DocumentTerm> comparator = DocumentTerm.getComparator(sort);
		if (size==Integer.MAX_VALUE) {
			unlimitedSizeQueue = new java.util.PriorityQueue<DocumentTerm>(11, comparator);
		}
		else {
			limitedSizeQueue = new LimitedSizeQueue<DocumentTerm>(size, comparator);
		}
	}
	
	private class LimitedSizeQueue<DocumentTerm> extends org.apache.lucene.util.PriorityQueue<DocumentTerm> {

		Comparator<DocumentTerm> comparator;
		
		public LimitedSizeQueue(int maxSize, Comparator<DocumentTerm> comparator) {
			super(maxSize);
			this.comparator = comparator;
		}

		@Override
		protected boolean lessThan(DocumentTerm a, DocumentTerm b) {
			return comparator.compare(a, b) < 0;
		}
		
	}

	public void offer(DocumentTerm documentTerm) {
		if (limitedSizeQueue!=null) {limitedSizeQueue.insertWithOverflow(documentTerm);}
		else if (unlimitedSizeQueue!=null) {unlimitedSizeQueue.offer(documentTerm);}
	}

	public int size() {
		if (limitedSizeQueue!=null) {return limitedSizeQueue.size();}
		else if (unlimitedSizeQueue!=null) {return unlimitedSizeQueue.size();}
		return 0;
	}

	public DocumentTerm poll() {
		if (limitedSizeQueue!=null) {return limitedSizeQueue.pop();}
		else if (unlimitedSizeQueue!=null) {return unlimitedSizeQueue.poll();}
		return null;
	}

//	@Override
//	protected boolean lessThan(DocumentTerm a,
//			DocumentTerm b) {
//		int ai, bi;
//		float af, bf;
//		String ab, bb;
//		switch(sort) {
//			case rawFrequencyAsc:
//				ai = a.getRawFrequency();
//				bi = b.getRawFrequency();
//				if (ai==bi) {
//					ab = a.getNormalizedTerm();
//					bb = b.getNormalizedTerm();
//					return ab.compareTo(bb) > 0;
//				}
//				else {return ai>bi;}
//			case rawFrequencyDesc:
//				ai = a.getRawFrequency();
//				bi = b.getRawFrequency();
//				if (ai==bi) {
//					ab = a.getNormalizedTerm();
//					bb = b.getNormalizedTerm();
//					return ab.compareTo(bb) > 0;
//				}
//				else {return ai<bi;}
//			case relativeFrequencyAsc:
//				af = a.getRelativeFrequency();
//				bf = b.getRelativeFrequency();
//				if (af==bf) {
//					ab = a.getNormalizedTerm();
//					bb = b.getNormalizedTerm();
//					return ab.compareTo(bb) > 0;
//				}
//				else {return af>bf;}
//			case termAsc:
//				ab = a.getNormalizedTerm();
//				bb = b.getNormalizedTerm();
//				if (ab.equals(bb)) {
//					ai = a.getRawFrequency();
//					bi = b.getRawFrequency();
//					return ai<bi;
//				}
//				else {
//					return ab.compareTo(bb) > 0;
//				}
//			case termDesc:
//				ab = a.getNormalizedTerm();
//				bb = b.getNormalizedTerm();
//				if (ab.equals(bb)) {
//					ai = a.getRawFrequency();
//					bi = b.getRawFrequency();
//					return ai<bi;
//				}
//				else {
//					return ab.compareTo(bb) < 0;
//				}
//			default: // relativeFrequencyDesc
//				af = a.getRelativeFrequency();
//				bf = b.getRelativeFrequency();
//				if (af==bf) {
//					ab = a.getNormalizedTerm();
//					bb = b.getNormalizedTerm();
//					return ab.compareTo(bb) > 0;
//				}
//				else {return af<bf;}
//		}
//	}
//	
	
}

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
package org.voyanttools.trombone.tool.analysis.document;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.PriorityQueue;
import org.voyanttools.trombone.model.DocumentTerm;

/**
 * @author sgs
 *
 */
public class DocumentTermsQueue extends PriorityQueue<DocumentTerm> {
	
	public enum Sort {
		rawFrequencyAsc, rawFrequencyDesc, relativeFrequencyAsc, relativeFrequencyDesc, termAsc, termDesc;
	}
	
	private Sort sort;

	public DocumentTermsQueue(int size, Sort sort) {
		super(size, false);
		this.sort = sort;
	}
	
	@Override
	protected boolean lessThan(DocumentTerm a,
			DocumentTerm b) {
		int ai, bi;
		float af, bf;
		String ab, bb;
		switch(sort) {
			case rawFrequencyAsc:
				ai = a.getRawFrequency();
				bi = b.getRawFrequency();
				if (ai==bi) {
					ab = a.getNormalizedTerm();
					bb = b.getNormalizedTerm();
					return ab.compareTo(bb) > 0;
				}
				else {return ai>bi;}
			case rawFrequencyDesc:
				ai = a.getRawFrequency();
				bi = b.getRawFrequency();
				if (ai==bi) {
					ab = a.getNormalizedTerm();
					bb = b.getNormalizedTerm();
					return ab.compareTo(bb) > 0;
				}
				else {return ai<bi;}
			case relativeFrequencyAsc:
				af = a.getRelativeFrequency();
				bf = b.getRelativeFrequency();
				if (af==bf) {
					ab = a.getNormalizedTerm();
					bb = b.getNormalizedTerm();
					return ab.compareTo(bb) > 0;
				}
				else {return af>bf;}
			case termAsc:
				ab = a.getNormalizedTerm();
				bb = b.getNormalizedTerm();
				if (ab.equals(bb)) {
					ai = a.getRawFrequency();
					bi = b.getRawFrequency();
					return ai<bi;
				}
				else {
					return ab.compareTo(bb) > 0;
				}
			case termDesc:
				ab = a.getNormalizedTerm();
				bb = b.getNormalizedTerm();
				if (ab.equals(bb)) {
					ai = a.getRawFrequency();
					bi = b.getRawFrequency();
					return ai<bi;
				}
				else {
					return ab.compareTo(bb) < 0;
				}
			default: // relativeFrequencyDesc
				af = a.getRelativeFrequency();
				bf = b.getRelativeFrequency();
				if (af==bf) {
					ab = a.getNormalizedTerm();
					bb = b.getNormalizedTerm();
					return ab.compareTo(bb) > 0;
				}
				else {return af<bf;}
		}
	}
	
	
}

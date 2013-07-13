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
package org.voyanttools.trombone.model;

import java.text.Normalizer;
import java.util.Comparator;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public class Kwic {
	
	public enum Sort {
		termAsc, termDesc, positionAsc, positionDesc;

		public static Sort valueOfForgivingly(String string) {
			string = string.toLowerCase();
			for (Sort t : values()) {
				if (t.name().equals(string)) return t;
			}
			return termAsc;
		}
	}

	int docIndex;
	
	String query;
	
	String term;
	
	@XStreamOmitField
	String normalizedAnalyzedMiddle = null;
	
	int position;
	
	String left;
	
	String middle;
	
	String right;
	
	public Kwic(int corpusDocumentIndex, String queryString,
			String term, int position, String left,
			String middle, String right) {
		this.docIndex = corpusDocumentIndex;
		this.query = queryString;
		this.term = term;
		this.position = position;
		this.left = left;
		this.middle = middle;
		this.right = right;
	}
	
	private String getNormalizedTerm() {
		if (normalizedAnalyzedMiddle==null) {
			normalizedAnalyzedMiddle = Normalizer.normalize(term, Normalizer.Form.NFD);
		}
		return normalizedAnalyzedMiddle;
	}

	public static Comparator<Kwic> getComparator(Sort sort) {
		switch(sort) {
		case termDesc:
			return TermDescendingComparator;
		case positionAsc: // FIXME
		case positionDesc: // FIXME
		default: // termAsc
			return TermAscendingComparator;
		}
	}

	private static Comparator<Kwic> TermAscendingComparator = new Comparator<Kwic>() {
		@Override
		public int compare(Kwic kwic1, Kwic kwic2) {
			int i = kwic2.getNormalizedTerm().compareTo(kwic1.getNormalizedTerm());
			if (i==0) {
				return kwic2.position - kwic1.position;
			}
			return i;
		}
	};

	private static Comparator<Kwic> TermDescendingComparator = new Comparator<Kwic>() {
		@Override
		public int compare(Kwic kwic1, Kwic kwic2) {
			int i = kwic2.getNormalizedTerm().compareTo(kwic1.getNormalizedTerm());
			if (i==0) {
				return kwic2.position - kwic1.position;
			}
			return i;
		}
	};
	
	public String toString() {
		return new StringBuilder(String.valueOf(docIndex)).append(".").append(position).append(" (").append(term).append("): ").append(left).append(" ***").append(middle).append("*** ").append(right).toString().replaceAll("\\s+", " ").trim();
	}
}

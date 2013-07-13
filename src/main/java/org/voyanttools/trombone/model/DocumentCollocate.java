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

import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public class DocumentCollocate {

	private int docIndex;
	private String keyword;
	private String term;
	@XStreamOmitField
	private String normalizedString = null;
	@XStreamOmitField
	private String normalizedTerm;
	private int raw;
	private float rel;
	private int docRaw;
	private float docRel;
	private float contextDocRelDiff;
	
	public enum Sort {
		termAsc,
		termDesc,
		relDesc,
		relAsc,
		rawDesc,
		rawAsc,
		docRelDesc,
		docRelAsc,
		docRawDesc,
		docRawAsc,
		contextDocRelDiffDesc,
		contextDocRelDiffAsc;

		public static Sort valueOfForgivingly(FlexibleParameters parameters) {
			if (parameters.containsKey("sort")) return valueOfForgivingly(parameters.getParameterValue("sort"));
			if (parameters.containsKey("sortBy")) return valueOfForgivingly(parameters.getParameterValue("sortBy"), parameters.getParameterValue("sortDirection"));
			return valueOfForgivingly(""); // use default
		}
		public static Sort valueOfForgivingly(String sortBy, String sortDirection) {
			if (sortBy==null) return valueOfForgivingly(""); // direction doesn't matter if not sortBy provided
			if (sortDirection!=null && sortDirection.toLowerCase().startsWith("asc")) {
				return valueOfForgivingly(sortBy+"Asc");
			}
			else {
				return valueOfForgivingly(sortBy+"Desc");
			}
		}
		public static Sort valueOfForgivingly(String string) {
			if (string!=null) {
				String compareString = string.toLowerCase();
				for (Sort t : values()) {
					if (t.name().toLowerCase().equals(compareString)) return t;
				}
			}
			return contextDocRelDiffDesc;
		}
	}
	
	public DocumentCollocate(int corpusDocumentIndex, String keyword, String term,
			int contextTermRawFrequency, float contextTermRelativeFrequency, int documentTermRawFrequency,
			float documentTermRelativeFrequency) {
		this.docIndex = corpusDocumentIndex;
		this.keyword = keyword;
		this.term = term;
		this.raw = contextTermRawFrequency;
		this.rel = contextTermRelativeFrequency;
		this.docRaw = documentTermRawFrequency;
		this.docRel = documentTermRelativeFrequency;
		contextDocRelDiff = contextTermRelativeFrequency-documentTermRelativeFrequency;
		
	}

	private String getNormalizedTerm() {
		if (normalizedTerm==null) {
			normalizedTerm =  Normalizer.normalize(term, Normalizer.Form.NFD);
		}
		return normalizedTerm;
	}
	public static Comparator<DocumentCollocate> getComparator(Sort sort) {
		switch (sort) {
		case termAsc: return TermAscendingComparator;
		case termDesc: return TermDescendingComparator;
		case relDesc: return ContextRelativeFrequencyDescendingComparator;
		case relAsc: return ContextRelativeFrequencyAscendingComparator;
		case rawDesc: return ContextRawFrequencyDescendingComparator;
		case rawAsc: return ContextRawFrequencyAscendingComparator;
		case docRelDesc: return DocumentRelativeFrequencyDescendingComparator;
		case docRelAsc: return DocumentRelativeFrequencyAscendingComparator;
		case docRawDesc: return DocumentRawFrequencyDescendingComparator;
		case docRawAsc: return DocumentRawFrequencyAscendingComparator;
		case contextDocRelDiffAsc: return ContextDocumentRelativeDifferenceAscendingComparator;
		default: // contextDocumentRelativeDifferenceDescending
			return ContextDocumentRelativeDifferenceAscendingComparator;
		}
	}

	private static Comparator<DocumentCollocate> ContextDocumentRelativeDifferenceAscendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			
			if (documentCollocate1.contextDocRelDiff==documentCollocate2.contextDocRelDiff) {
				return documentCollocate2.getNormalizedTerm().compareTo(documentCollocate1.getNormalizedTerm());
			}
			else {
				return documentCollocate1.contextDocRelDiff > documentCollocate2.contextDocRelDiff ? 1 : -1;
			}
		}
		
	};

	private static Comparator<DocumentCollocate> ContextDocumentRelativeDifferenceDescendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			
			if (documentCollocate1.contextDocRelDiff==documentCollocate2.contextDocRelDiff) {
				return documentCollocate2.getNormalizedTerm().compareTo(documentCollocate1.getNormalizedTerm());
			}
			else {
				return documentCollocate2.contextDocRelDiff > documentCollocate1.contextDocRelDiff ? 1 : -1;
			}
		}
		
	};

	private static Comparator<DocumentCollocate> ContextRawFrequencyDescendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1.raw==documentCollocate2.raw) {
				return documentCollocate2.getNormalizedTerm().compareTo(documentCollocate1.getNormalizedTerm());
			}
			else {
				return documentCollocate1.raw - documentCollocate2.raw;
			}
		}
		
	};
	
	private static Comparator<DocumentCollocate> ContextRawFrequencyAscendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1.raw==documentCollocate2.raw) {
				return documentCollocate2.getNormalizedTerm().compareTo(documentCollocate1.getNormalizedTerm());
			}
			else {
				return documentCollocate2.raw - documentCollocate1.raw;
			}
		}
		
	};

	private static Comparator<DocumentCollocate> TermAscendingComparator = new Comparator<DocumentCollocate>() {
		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {			
			if (documentCollocate1.term.equals(documentCollocate2.term)) {
				return documentCollocate1.contextDocRelDiff > documentCollocate2.contextDocRelDiff ? 1 : -1;
			}
			else {
				return documentCollocate2.getNormalizedTerm().compareTo(documentCollocate1.getNormalizedTerm());
			}
		}
	};

	private static Comparator<DocumentCollocate> TermDescendingComparator = new Comparator<DocumentCollocate>() {
		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {			
			if (documentCollocate1.term.equals(documentCollocate2.term)) {
				return documentCollocate1.contextDocRelDiff > documentCollocate2.contextDocRelDiff ? 1 : -1;
			}
			else {
				return documentCollocate1.getNormalizedTerm().compareTo(documentCollocate2.getNormalizedTerm());
			}
		}
	};

	private static Comparator<DocumentCollocate> ContextRelativeFrequencyDescendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1.rel==documentCollocate2.rel) {
				return documentCollocate2.getNormalizedTerm().compareTo(documentCollocate1.getNormalizedTerm());
			}
			else {
				return documentCollocate1.rel > documentCollocate2.rel ? 1 : -1;
			}
		}
		
	};
	
	private static Comparator<DocumentCollocate> ContextRelativeFrequencyAscendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1.rel==documentCollocate2.rel) {
				return documentCollocate2.getNormalizedTerm().compareTo(documentCollocate1.getNormalizedTerm());
			}
			else {
				return documentCollocate2.rel > documentCollocate1.rel ? 1 : -1;
			}
		}
		
	};

	private static Comparator<DocumentCollocate> DocumentRelativeFrequencyDescendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1.docRel==documentCollocate2.docRel) {
				return documentCollocate2.getNormalizedTerm().compareTo(documentCollocate1.getNormalizedTerm());
			}
			else {
				return Float.compare(documentCollocate1.docRel, documentCollocate2.docRel);
			}
		}
		
	};
	
	private static Comparator<DocumentCollocate> DocumentRelativeFrequencyAscendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1.docRel==documentCollocate2.docRel) {
				return documentCollocate2.getNormalizedTerm().compareTo(documentCollocate1.getNormalizedTerm());
			}
			else {
				return Float.compare(documentCollocate2.docRel, documentCollocate1.docRel);
			}
		}
		
	};

	private static Comparator<DocumentCollocate> DocumentRawFrequencyDescendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1.docRaw==documentCollocate2.docRaw) {
				return documentCollocate2.getNormalizedTerm().compareTo(documentCollocate1.getNormalizedTerm());
			}
			else {
				return documentCollocate1.docRaw - documentCollocate2.docRaw;
			}
		}
		
	};
	
	private static Comparator<DocumentCollocate> DocumentRawFrequencyAscendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1.docRel==documentCollocate2.docRel) {
				return documentCollocate2.getNormalizedTerm().compareTo(documentCollocate1.getNormalizedTerm());
			}
			else {
				return documentCollocate2.docRaw - documentCollocate1.docRaw;
			}
		}
		
	};
	public String toString() {
		return "("+keyword+") "+term+": "+raw+" ("+rel+") / "+docRaw+" ("+docRel+"); difference: "+contextDocRelDiff;
	}

	public String getTerm() {
		return term;
	}
	
	public int getContextRawFrequency() {
		return raw;
	}
	public float getContextRelativeFrequency() {
		return rel;
	}
	public int getDocumentRawFrequency() {
		return docRaw;
	}
	public float getDocumentRelativeFrequency() {
		return docRel;
	}
}

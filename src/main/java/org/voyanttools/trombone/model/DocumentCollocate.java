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

import java.io.Serializable;
import java.text.Normalizer;
import java.util.Comparator;

import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public class DocumentCollocate  implements Serializable {

	private int docIndex;
	private String keyword;
	private int keywordContextRawFrequency;
	private String term;
	@XStreamOmitField
	private String normalizedString = null;
	@XStreamOmitField
	private String normalizedTerm;
	private int termContextRawFrequency;
	private float termContextRelativeFrequency;
	private int termDocumentRawFrequency;
	private float termDocumentRelativeFrequency;
	private float termContextDocumentRelativeFrequencyDifference;
	
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
			return rawDesc;
		}
	}
	
	public DocumentCollocate(int corpusDocumentIndex, String keyword, String term,
			int keywordContextRawFrequency, int termContextRawFrequency, int termDocumentRawFrequency,
			int contextTotalTokens, int documentTotalTokens) {
		this.docIndex = corpusDocumentIndex;
		this.keyword = keyword;
		this.keywordContextRawFrequency = keywordContextRawFrequency;
		this.term = term;
		this.termContextRawFrequency = termContextRawFrequency;
		this.termContextRelativeFrequency = termContextRawFrequency / contextTotalTokens;
		this.termDocumentRawFrequency = termDocumentRawFrequency;
		this.termDocumentRelativeFrequency = termDocumentRawFrequency / documentTotalTokens;
		termContextDocumentRelativeFrequencyDifference = termContextRelativeFrequency-termDocumentRelativeFrequency;
		
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
		case contextDocRelDiffDesc: return ContextDocumentRelativeDifferenceDescendingComparator;
		default: // contextDocumentRelativeDifferenceDescending
			return ContextRawFrequencyDescendingComparator;
		}
	}

	private static Comparator<DocumentCollocate> ContextDocumentRelativeDifferenceAscendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1==null) {return 1;} // not sure of this
			if (documentCollocate2==null) {return -1;}
			if (documentCollocate1.termContextDocumentRelativeFrequencyDifference==documentCollocate2.termContextDocumentRelativeFrequencyDifference) {
				return documentCollocate1.getNormalizedTerm().compareTo(documentCollocate2.getNormalizedTerm());
			}
			else {
				return documentCollocate1.termContextDocumentRelativeFrequencyDifference > documentCollocate2.termContextDocumentRelativeFrequencyDifference ? -1 : 1;
			}
		}
		
	};

	private static Comparator<DocumentCollocate> ContextDocumentRelativeDifferenceDescendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			
			if (documentCollocate1==null) {return 1;} // not sure of this
			if (documentCollocate2==null) {return -1;}
			if (documentCollocate1.termContextDocumentRelativeFrequencyDifference==documentCollocate2.termContextDocumentRelativeFrequencyDifference) {
				return documentCollocate1.getNormalizedTerm().compareTo(documentCollocate2.getNormalizedTerm());
			}
			else {
				return documentCollocate2.termContextDocumentRelativeFrequencyDifference > documentCollocate1.termContextDocumentRelativeFrequencyDifference ? 1 : -1;
			}
		}
		
	};

	private static Comparator<DocumentCollocate> ContextRawFrequencyDescendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1==null) {return 1;} // not sure of this
			if (documentCollocate2==null) {return -1;}
			if (documentCollocate1.termContextRawFrequency==documentCollocate2.termContextRawFrequency) {
				return documentCollocate1.getNormalizedTerm().compareTo(documentCollocate2.getNormalizedTerm());
			}
			else {
				return documentCollocate2.termContextRawFrequency - documentCollocate1.termContextRawFrequency;
			}
		}
		
	};
	
	private static Comparator<DocumentCollocate> ContextRawFrequencyAscendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1==null) {return 1;} // not sure of this
			if (documentCollocate2==null) {return -1;}
			if (documentCollocate1.termContextRawFrequency==documentCollocate2.termContextRawFrequency) {
				return documentCollocate1.getNormalizedTerm().compareTo(documentCollocate2.getNormalizedTerm());
			}
			else {
				return documentCollocate1.termContextRawFrequency - documentCollocate2.termContextRawFrequency;
			}
		}
		
	};

	private static Comparator<DocumentCollocate> TermAscendingComparator = new Comparator<DocumentCollocate>() {
		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {			
			if (documentCollocate1==null) {return 1;} // not sure of this
			if (documentCollocate2==null) {return -1;}
			if (documentCollocate1.term.equals(documentCollocate2.term)) {
				return documentCollocate1.termContextDocumentRelativeFrequencyDifference > documentCollocate2.termContextDocumentRelativeFrequencyDifference ? 1 : -1;
			}
			else {
				return documentCollocate1.getNormalizedTerm().compareTo(documentCollocate2.getNormalizedTerm());
			}
		}
	};

	private static Comparator<DocumentCollocate> TermDescendingComparator = new Comparator<DocumentCollocate>() {
		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {			
			if (documentCollocate1==null) {return 1;} // not sure of this
			if (documentCollocate2==null) {return -1;}
			if (documentCollocate1.term.equals(documentCollocate2.term)) {
				return documentCollocate1.termContextDocumentRelativeFrequencyDifference > documentCollocate2.termContextDocumentRelativeFrequencyDifference ? 1 : -1;
			}
			else {
				return documentCollocate2.getNormalizedTerm().compareTo(documentCollocate1.getNormalizedTerm());
			}
		}
	};

	private static Comparator<DocumentCollocate> ContextRelativeFrequencyDescendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1==null) {return 1;} // not sure of this
			if (documentCollocate2==null) {return -1;}
			if (documentCollocate1.termContextRelativeFrequency==documentCollocate2.termContextRelativeFrequency) {
				return documentCollocate1.getNormalizedTerm().compareTo(documentCollocate2.getNormalizedTerm());
			}
			else {
				return documentCollocate1.termContextRelativeFrequency > documentCollocate2.termContextRelativeFrequency ? -1 : 1;
			}
		}
		
	};
	
	private static Comparator<DocumentCollocate> ContextRelativeFrequencyAscendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1==null) {return 1;} // not sure of this
			if (documentCollocate2==null) {return -1;}
			if (documentCollocate1.termContextRelativeFrequency==documentCollocate2.termContextRelativeFrequency) {
				return documentCollocate1.getNormalizedTerm().compareTo(documentCollocate2.getNormalizedTerm());
			}
			else {
				return documentCollocate1.termContextRelativeFrequency > documentCollocate2.termContextRelativeFrequency ? 1 : -1;
			}
		}
		
	};

	private static Comparator<DocumentCollocate> DocumentRelativeFrequencyDescendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1==null) {return 1;} // not sure of this
			if (documentCollocate2==null) {return -1;}
			if (documentCollocate1.termDocumentRelativeFrequency==documentCollocate2.termDocumentRelativeFrequency) {
				return documentCollocate1.getNormalizedTerm().compareTo(documentCollocate2.getNormalizedTerm());
			}
			else {
				return Float.compare(documentCollocate2.termDocumentRelativeFrequency, documentCollocate1.termDocumentRelativeFrequency);
			}
		}
		
	};
	
	private static Comparator<DocumentCollocate> DocumentRelativeFrequencyAscendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1==null) {return 1;} // not sure of this
			if (documentCollocate2==null) {return -1;}
			if (documentCollocate1.termDocumentRelativeFrequency==documentCollocate2.termDocumentRelativeFrequency) {
				return documentCollocate1.getNormalizedTerm().compareTo(documentCollocate2.getNormalizedTerm());
			}
			else {
				return Float.compare(documentCollocate1.termDocumentRelativeFrequency, documentCollocate2.termDocumentRelativeFrequency);
			}
		}
		
	};

	private static Comparator<DocumentCollocate> DocumentRawFrequencyDescendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1==null) {return 1;} // not sure of this
			if (documentCollocate2==null) {return -1;}
			if (documentCollocate1.termDocumentRawFrequency==documentCollocate2.termDocumentRawFrequency) {
				return documentCollocate1.getNormalizedTerm().compareTo(documentCollocate2.getNormalizedTerm());
			}
			else {
				return documentCollocate2.termDocumentRawFrequency - documentCollocate1.termDocumentRawFrequency;
			}
		}
		
	};
	
	private static Comparator<DocumentCollocate> DocumentRawFrequencyAscendingComparator = new Comparator<DocumentCollocate>() {

		@Override
		public int compare(DocumentCollocate documentCollocate1, DocumentCollocate documentCollocate2) {
			if (documentCollocate1==null) {return 1;} // not sure of this
			if (documentCollocate2==null) {return -1;}
			if (documentCollocate1.termDocumentRelativeFrequency==documentCollocate2.termDocumentRelativeFrequency) {
				return documentCollocate1.getNormalizedTerm().compareTo(documentCollocate2.getNormalizedTerm());
			}
			else {
				return documentCollocate2.termDocumentRawFrequency - documentCollocate1.termDocumentRawFrequency;
			}
		}
		
	};
	public String toString() {
		return "("+keyword+") "+term+": "+termContextRawFrequency+" ("+termContextRelativeFrequency+") / "+termDocumentRawFrequency+" ("+termDocumentRelativeFrequency+"); difference: "+termContextDocumentRelativeFrequencyDifference;
	}

	public String getTerm() {
		return term;
	}
	
	public String getKeyword() {
		return keyword;
	}
	
	public int getKeywordContextRawFrequency() {
		return keywordContextRawFrequency;
	}
	
	public int getContextRawFrequency() {
		return termContextRawFrequency;
	}
	public float getTermContextRelativeFrequency() {
		return termContextRelativeFrequency;
	}
	public int getTermDocumentRawFrequency() {
		return termDocumentRawFrequency;
	}
	public float getTermDocumentRelativeFrequency() {
		return termDocumentRelativeFrequency;
	}

	public int getDocIndex() {
		return docIndex;
	}

}

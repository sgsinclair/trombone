/**
 * 
 */
package org.voyanttools.trombone.model;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;

import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public class DocumentNgram implements Comparable<DocumentNgram> {

	public enum Sort {
		RAWFREQASC, RAWFREQDESC, TERMASC, TERMDESC, LENGTHASC, LENGTHDESC;

		public static Sort getForgivingly(FlexibleParameters parameters) {
			String sort = parameters.getParameterValue("sort", "").toUpperCase();
			String sortPrefix = "RAWFREQ"; // default
			if (sort.startsWith("TERM")) {sortPrefix = "TERM";}
			else if (sort.startsWith("LENGTH")) {sortPrefix = "LENGTH";}
			String dir = parameters.getParameterValue("dir", "").toUpperCase();
			String dirSuffix = "DESC";
			if (dir.endsWith("ASC")) {dirSuffix="ASC";}
			return valueOf(sortPrefix+dirSuffix);
		}

	}
	
	private int docIndex;
	private String term;
	private int rawFreq;
	private int length;
	private List<int[]> positions;
	
	@XStreamOmitField
	private transient String normalizedString = null;

	public DocumentNgram(int corpusDocumentIndex, String term, List<int[]> positions, int length) {
		this.docIndex = corpusDocumentIndex;
		this.term = term;
		this.length = length;
		this.rawFreq = positions.size();
		this.positions = positions;
	}
	public int getCorpusDocumentIndex() {
		return docIndex;
	}
	public String getTerm() {
		return term;
	}
	public int getLength() {
		return length;
	}
	public List<int[]> getPositions() {
		return positions;
	}
	public String toString() {
		return "("+docIndex+") "+term+": "+positions.size()+" ("+length+")";
	}
	@Override
	public int compareTo(DocumentNgram ngram) {
		if (length==ngram.length && positions.size()>0 && ngram.positions.size()>0) {
			// sort by first position if same length
			int a = positions.get(0)[0];
			int b = ngram.positions.get(0)[0];
			return a > b ? 1 : a < b ? -1 : 0;
		}
		return length > ngram.length ? -1 : length < ngram.length ? 1 : 0;
	}
	private String getNormalizedTerm() {
		if (normalizedString==null) {normalizedString = Normalizer.normalize(term, Normalizer.Form.NFD);}
		return normalizedString;
	}

	public static Comparator<DocumentNgram> getComparator(Sort sort) {
		switch (sort) {
		case RAWFREQASC:
			return RawFrequencyAscendingComparator;
		case TERMASC:
			return TermAscendingComparator;
		case TERMDESC:
			return TermDescendingComparator;
		case LENGTHASC:
			return LengthAscendingComparator;
		case LENGTHDESC:
			return LengthDescendingComparator;
		default: // rawFrequencyDesc
			return RawFrequencyDescendingComparator;
		}	
	}
	
	private static Comparator<DocumentNgram> TermAscendingComparator = new Comparator<DocumentNgram>() {
		@Override
		public int compare(DocumentNgram term1, DocumentNgram term2) {
			int i = term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
			if (i==0) {
				return term1.rawFreq - term2.rawFreq;
			}
			return i;
		}
	};

	private static Comparator<DocumentNgram> TermDescendingComparator = new Comparator<DocumentNgram>() {
		@Override
		public int compare(DocumentNgram term1, DocumentNgram term2) {
			int i = term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			if (i==0) {
				return term1.rawFreq - term2.rawFreq;
			}
			return i;
		}
	};

	private static Comparator<DocumentNgram> RawFrequencyDescendingComparator = new Comparator<DocumentNgram>() {

		@Override
		public int compare(DocumentNgram term1, DocumentNgram term2) {
			if (term1.rawFreq==term2.rawFreq) {
				return term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			}
			else {
				return term2.rawFreq - term1.rawFreq;
			}
		}
		
	};

	private static Comparator<DocumentNgram> RawFrequencyAscendingComparator = new Comparator<DocumentNgram>() {

		@Override
		public int compare(DocumentNgram term1, DocumentNgram term2) {
			if (term1.rawFreq==term2.rawFreq) {
				return term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
			}
			else {
				return term1.rawFreq - term2.rawFreq;
			}
		}
		
	};

	private static Comparator<DocumentNgram> LengthDescendingComparator = new Comparator<DocumentNgram>() {

		@Override
		public int compare(DocumentNgram term1, DocumentNgram term2) {
			if (term1.length==term2.length) {
				return term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			}
			else {
				return term2.length - term1.length;
			}
		}
		
	};

	private static Comparator<DocumentNgram> LengthAscendingComparator = new Comparator<DocumentNgram>() {

		@Override
		public int compare(DocumentNgram term1, DocumentNgram term2) {
			if (term1.length==term2.length) {
				return term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
			}
			else {
				return term1.length - term2.length;
			}
		}
		
	};

}

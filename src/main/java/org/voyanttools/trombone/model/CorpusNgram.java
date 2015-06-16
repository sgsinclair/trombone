/**
 * 
 */
package org.voyanttools.trombone.model;

import java.text.Normalizer;
import java.util.Comparator;

import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public class CorpusNgram {

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

	private String term;
	private int rawFreq;
	private int length;
	private int[] rawFreqs;

	@XStreamOmitField
	private transient String normalizedString = null;

	/**
	 * @param rawFreqs 
	 * @param length 
	 * @param term 
	 * 
	 */
	public CorpusNgram(String term, int length, int[] rawFreqs) {
		this.term = term;
		this.length = length;
		this.rawFreqs = rawFreqs;
		this.rawFreq = 0;
		for (int i : rawFreqs) {
			this.rawFreq+=i;
		}
	}

	private String getNormalizedTerm() {
		if (normalizedString==null) {normalizedString = Normalizer.normalize(term, Normalizer.Form.NFD);}
		return normalizedString;
	}

	public static Comparator<CorpusNgram> getComparator(Sort sort) {
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
	
	private static Comparator<CorpusNgram> TermAscendingComparator = new Comparator<CorpusNgram>() {
		@Override
		public int compare(CorpusNgram term1, CorpusNgram term2) {
			int i = term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
			if (i==0) {
				return term1.rawFreq - term2.rawFreq;
			}
			return i;
		}
	};

	private static Comparator<CorpusNgram> TermDescendingComparator = new Comparator<CorpusNgram>() {
		@Override
		public int compare(CorpusNgram term1, CorpusNgram term2) {
			int i = term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			if (i==0) {
				return term1.rawFreq - term2.rawFreq;
			}
			return i;
		}
	};

	private static Comparator<CorpusNgram> RawFrequencyDescendingComparator = new Comparator<CorpusNgram>() {

		@Override
		public int compare(CorpusNgram term1, CorpusNgram term2) {
			if (term1.rawFreq==term2.rawFreq) {
				return term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			}
			else {
				return term2.rawFreq - term1.rawFreq;
			}
		}
		
	};

	private static Comparator<CorpusNgram> RawFrequencyAscendingComparator = new Comparator<CorpusNgram>() {

		@Override
		public int compare(CorpusNgram term1, CorpusNgram term2) {
			if (term1.rawFreq==term2.rawFreq) {
				return term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
			}
			else {
				return term1.rawFreq - term2.rawFreq;
			}
		}
		
	};

	private static Comparator<CorpusNgram> LengthDescendingComparator = new Comparator<CorpusNgram>() {

		@Override
		public int compare(CorpusNgram term1, CorpusNgram term2) {
			if (term1.length==term2.length) {
				return term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			}
			else {
				return term2.length - term1.length;
			}
		}
		
	};

	private static Comparator<CorpusNgram> LengthAscendingComparator = new Comparator<CorpusNgram>() {

		@Override
		public int compare(CorpusNgram term1, CorpusNgram term2) {
			if (term1.length==term2.length) {
				return term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
			}
			else {
				return term1.length - term2.length;
			}
		}
		
	};

	@Override
	public String toString() {
		return "{"+term+": "+rawFreq+" ("+length+")";
	}

}

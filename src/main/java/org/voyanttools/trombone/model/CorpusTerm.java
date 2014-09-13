package org.voyanttools.trombone.model;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.Comparator;

import org.voyanttools.trombone.model.Kwic.Sort;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

public class CorpusTerm implements Serializable {

	public enum Sort {
		RAWFREQASC, RAWFREQDESC, TERMASC, TERMDESC;

		public static Sort getForgivingly(FlexibleParameters parameters) {
			String sort = parameters.getParameterValue("sort", "").toUpperCase();
			String sortPrefix = "RAWFREQ"; // default
			if (sort.startsWith("TERM")) {sortPrefix = "TERM";}
			String dir = parameters.getParameterValue("dir", "").toUpperCase();
			String dirSuffix = "DESC";
			if (dir.endsWith("ASC")) {dirSuffix="ASC";}
			return valueOf(sortPrefix+dirSuffix);
		}
	}

	private String term;
	private int rawFreq;
	private float relativeFreq;
	private int[] rawFreqs;
	private float[] relativeFreqs;
	
	@XStreamOmitField
	private String normalizedString = null;
	
	
	public CorpusTerm(String termString, int termFreq, float relativeFreq,
			int[] rawFreqs, float[] relativeFreqs) {
		this.term = termString;
		this.rawFreq = termFreq;
		this.relativeFreq = relativeFreq;
		this.rawFreqs = rawFreqs;
		this.relativeFreqs = relativeFreqs;
	}

	public int getRawFreq() {
		return this.rawFreq;
	}
	
	public float getRelativeFreq() {
		return relativeFreq;
	}
	
	private String getNormalizedTerm() {
		if (normalizedString==null) {normalizedString = Normalizer.normalize(term, Normalizer.Form.NFD);}
		return normalizedString;
	}

	public String getTerm() {
		return term;
	}
	
	public int[] getRawDistributions(int bins) {
		if (rawFreqs==null) return new int[0];
		if (bins==0) {bins=rawFreqs.length;} // nothing set, so use corpus length
		int[] distributions = new int[bins];
		for(int position=0, len=rawFreqs.length; position<len; position++) {
			distributions[(int) (position*bins/len)]+=rawFreqs[position];
		}
		return distributions;
	}

	public float[] getRelativeDistributions(int bins) {
		if (relativeFreqs==null) return new float[0];
		if (bins==0) {bins=relativeFreqs.length;} // nothing set, so use corpus length
		float[] distributions = new float[bins];
		for(int position=0, len=relativeFreqs.length; position<len; position++) {
			distributions[(int) (position*bins/len)]+=relativeFreqs[position]; // TODO: this needs to be averaged?
		}
		return distributions;
	}
	
	public static Comparator<CorpusTerm> getComparator(Sort sort) {
		switch (sort) {
		case RAWFREQASC:
			return RawFrequencyAscendingComparator;
		case TERMASC:
			return TermAscendingComparator;
		case TERMDESC:
			return TermDescendingComparator;
		default: // rawFrequencyDesc
			return RawFrequencyDescendingComparator;
		}
	}
	
	private static Comparator<CorpusTerm> TermAscendingComparator = new Comparator<CorpusTerm>() {
		@Override
		public int compare(CorpusTerm term1, CorpusTerm term2) {
			int i = term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
			if (i==0) {
				return term1.rawFreq - term2.rawFreq;
			}
			return i;
		}
	};

	private static Comparator<CorpusTerm> TermDescendingComparator = new Comparator<CorpusTerm>() {
		@Override
		public int compare(CorpusTerm term1, CorpusTerm term2) {
			int i = term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			if (i==0) {
				return term1.rawFreq - term2.rawFreq;
			}
			return i;
		}
	};

	private static Comparator<CorpusTerm> RawFrequencyDescendingComparator = new Comparator<CorpusTerm>() {

		@Override
		public int compare(CorpusTerm term1, CorpusTerm term2) {
			if (term1.rawFreq==term2.rawFreq) {
				return term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
			}
			else {
				return term1.rawFreq - term2.rawFreq;
			}
		}
		
	};
	
	private static Comparator<CorpusTerm> RawFrequencyAscendingComparator = new Comparator<CorpusTerm>() {

		@Override
		public int compare(CorpusTerm term1, CorpusTerm term2) {
			if (term1.rawFreq==term2.rawFreq) {
				return term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
			}
			else {
				return term2.rawFreq - term1.rawFreq;
			}
		}
		
	};

}

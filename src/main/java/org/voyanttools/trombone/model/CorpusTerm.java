package org.voyanttools.trombone.model;

import java.text.Normalizer;
import java.util.Comparator;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

public class CorpusTerm {

	public enum Sort {
		rawFrequencyAsc, rawFrequencyDesc, termAsc, termDesc;
	}

	private String term;
	private int rawFreq;
	private int[] documentFreqs;
	
	@XStreamOmitField
	private String normalizedString = null;
	
	
	public CorpusTerm(String termString, int termFreq,
			int[] documentFreqs) {
		this.term = termString;
		this.rawFreq = termFreq;
		this.documentFreqs = documentFreqs;
	}

	public int getRawFrequency() {
		return this.rawFreq;
	}
	
	private String getNormalizedTerm() {
		if (normalizedString==null) {normalizedString = Normalizer.normalize(term, Normalizer.Form.NFD);}
		return normalizedString;
	}

	public String getTerm() {
		return term;
	}
	
	public static Comparator<CorpusTerm> getComparator(Sort sort) {
		switch (sort) {
		case rawFrequencyAsc:
			return RawFrequencyAscendingComparator;
		case termAsc:
			return TermAscendingComparator;
		case termDesc:
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

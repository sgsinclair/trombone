package org.voyanttools.trombone.model;

import java.text.Normalizer;
import java.util.Comparator;

import org.voyanttools.trombone.tool.analysis.corpus.CorpusTermsSort;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

public class CorpusTerm {

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
	
	public static Comparator<CorpusTerm> getComparator(CorpusTermsSort sort) {
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
		public int compare(CorpusTerm ctfs1, CorpusTerm ctfs2) {
			int i = ctfs2.getNormalizedTerm().compareTo(ctfs1.getNormalizedTerm());
			if (i==0) {
				return ctfs1.rawFreq - ctfs2.rawFreq;
			}
			return i;
		}
	};

	private static Comparator<CorpusTerm> TermDescendingComparator = new Comparator<CorpusTerm>() {
		@Override
		public int compare(CorpusTerm ctfs1, CorpusTerm ctfs2) {
			int i = ctfs1.getNormalizedTerm().compareTo(ctfs2.getNormalizedTerm());
			if (i==0) {
				return ctfs1.rawFreq - ctfs2.rawFreq;
			}
			return i;
		}
	};

	private static Comparator<CorpusTerm> RawFrequencyDescendingComparator = new Comparator<CorpusTerm>() {

		@Override
		public int compare(CorpusTerm ctfs1, CorpusTerm ctfs2) {
			if (ctfs1.rawFreq==ctfs2.rawFreq) {
				return ctfs2.getNormalizedTerm().compareTo(ctfs1.getNormalizedTerm());
			}
			else {
				return ctfs1.rawFreq - ctfs2.rawFreq;
			}
		}
		
	};
	
	private static Comparator<CorpusTerm> RawFrequencyAscendingComparator = new Comparator<CorpusTerm>() {

		@Override
		public int compare(CorpusTerm ctfs1, CorpusTerm ctfs2) {
			if (ctfs1.rawFreq==ctfs2.rawFreq) {
				return ctfs2.getNormalizedTerm().compareTo(ctfs1.getNormalizedTerm());
			}
			else {
				return ctfs2.rawFreq - ctfs1.rawFreq;
			}
		}
		
	};
}

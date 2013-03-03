package org.voyanttools.trombone.tool.analysis.corpus;

import java.text.Normalizer;
import java.util.Comparator;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

public class CorpusTermFrequencyStats {

	private String term;
	private int rawFreq;
	private int[] documentFreqs;
	
	@XStreamOmitField
	private String normalizedString = null;
	
	
	public CorpusTermFrequencyStats(String termString, int termFreq,
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
	
	public static Comparator<CorpusTermFrequencyStats> getComparator(CorpusTermFrequencyStatsSort sort) {
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
	
	private static Comparator<CorpusTermFrequencyStats> TermAscendingComparator = new Comparator<CorpusTermFrequencyStats>() {
		@Override
		public int compare(CorpusTermFrequencyStats ctfs1, CorpusTermFrequencyStats ctfs2) {
			int i = ctfs2.getNormalizedTerm().compareTo(ctfs1.getNormalizedTerm());
			if (i==0) {
				return ctfs1.rawFreq - ctfs2.rawFreq;
			}
			return i;
		}
	};

	private static Comparator<CorpusTermFrequencyStats> TermDescendingComparator = new Comparator<CorpusTermFrequencyStats>() {
		@Override
		public int compare(CorpusTermFrequencyStats ctfs1, CorpusTermFrequencyStats ctfs2) {
			int i = ctfs1.getNormalizedTerm().compareTo(ctfs2.getNormalizedTerm());
			if (i==0) {
				return ctfs1.rawFreq - ctfs2.rawFreq;
			}
			return i;
		}
	};

	private static Comparator<CorpusTermFrequencyStats> RawFrequencyDescendingComparator = new Comparator<CorpusTermFrequencyStats>() {

		@Override
		public int compare(CorpusTermFrequencyStats ctfs1, CorpusTermFrequencyStats ctfs2) {
			if (ctfs1.rawFreq==ctfs2.rawFreq) {
				return ctfs2.getNormalizedTerm().compareTo(ctfs1.getNormalizedTerm());
			}
			else {
				return ctfs1.rawFreq - ctfs2.rawFreq;
			}
		}
		
	};
	
	private static Comparator<CorpusTermFrequencyStats> RawFrequencyAscendingComparator = new Comparator<CorpusTermFrequencyStats>() {

		@Override
		public int compare(CorpusTermFrequencyStats ctfs1, CorpusTermFrequencyStats ctfs2) {
			if (ctfs1.rawFreq==ctfs2.rawFreq) {
				return ctfs2.getNormalizedTerm().compareTo(ctfs1.getNormalizedTerm());
			}
			else {
				return ctfs2.rawFreq - ctfs1.rawFreq;
			}
		}
		
	};
}

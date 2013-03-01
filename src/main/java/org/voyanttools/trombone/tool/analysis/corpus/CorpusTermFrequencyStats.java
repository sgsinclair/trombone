package org.voyanttools.trombone.tool.analysis.corpus;

import java.text.Normalizer;

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
	
	public String getNormalizedTerm() {
		if (normalizedString==null) {normalizedString = Normalizer.normalize(term, Normalizer.Form.NFD);}
		return normalizedString;
	}

	public Object getTerm() {
		return term;
	}
	

}

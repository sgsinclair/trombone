package org.voyanttools.trombone.tool.analysis.corpus;

import java.text.Normalizer;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

public class CorpusTermFrequencyStats {

	private String termString;
	private int termFreq;
	private int[] documentFreqs;
	
	@XStreamOmitField
	private String normalizedString = null;
	
	
	public CorpusTermFrequencyStats(String termString, int termFreq,
			int[] documentFreqs) {
		this.termString = termString;
		this.termFreq = termFreq;
		this.documentFreqs = documentFreqs;
	}

	public int getRawFrequency() {
		return this.termFreq;
	}
	
	public String getNormalizedTerm() {
		if (normalizedString==null) {normalizedString = Normalizer.normalize(termString, Normalizer.Form.NFD);}
		return normalizedString;
	}
	

}

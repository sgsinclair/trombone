package org.voyanttools.trombone.model;

import java.io.Serializable;

public class CorpusTermMinimal implements Serializable {

	private String term;
	private int rawFreq;
	private int inDocumentsCount;
	private int documentsCount;
	private float zscore;
	public CorpusTermMinimal(String term, int rawFreq, int inDocumentsCount, int documentsCount, float zscore) {
		this.term = term;
		this.rawFreq = rawFreq;
		this.inDocumentsCount = inDocumentsCount;
		this.documentsCount = documentsCount;
		this.zscore = zscore;
	}
	public int getRawFreq() {
		return rawFreq;
	}
	public void setZscore(float f) {
		this.zscore = f;
	}
	public String getTerm() {
		return term;
	}
	public int getInDocumentsCount() {
		return inDocumentsCount;
	}
	public int getDocumentsCount() {
		return documentsCount;
	}
	public float getZscore() {
		return zscore;
	}

}

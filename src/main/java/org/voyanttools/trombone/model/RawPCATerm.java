package org.voyanttools.trombone.model;

/**
 * @author Andrew MacDonald
 */
public class RawPCATerm extends RawAnalysisTerm {

	private int rawFreq;
	private double relativeFreq;
	
	public RawPCATerm(String term, int rawFrequency, double relativeFrequency) {
		super(term);
		this.rawFreq = rawFrequency;
		this.relativeFreq = relativeFrequency;
	}
	
	public RawPCATerm(String term, int rawFrequency, double relativeFrequency, double[] vector) {
		super(term, vector);
		this.rawFreq = rawFrequency;
		this.relativeFreq = relativeFrequency;
	}

	public int getRawFrequency() {
		return this.rawFreq;
	}
	public void setRawFrequency(int rawFreq) {
		this.rawFreq = rawFreq;
	}

	public double getRelativeFrequency() {
		return this.relativeFreq;
	}
	public void setRelativeFrequency(double relativeFreq) {
		this.relativeFreq = relativeFreq;
	}

}

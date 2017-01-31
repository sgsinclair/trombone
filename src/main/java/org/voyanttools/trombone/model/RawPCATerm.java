package org.voyanttools.trombone.model;

/**
 * @author Andrew MacDonald
 */
public class RawPCATerm extends RawAnalysisTerm {

	private final int rawFreq;
	private final double relativeFreq;
	
	public RawPCATerm(String term, int rawFrequency, double relativeFrequency, double[] vector) {
		super(term, vector);
		this.rawFreq = rawFrequency;
		this.relativeFreq = relativeFrequency;
	}

	public int getRawFrequency() {
		return this.rawFreq;
	}

	public double getRelativeFrequency() {
		return this.relativeFreq;
	}

}

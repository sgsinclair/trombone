package org.voyanttools.trombone.model;

/**
 * @author Andrew MacDonald
 */
public class RawPCAType extends RawAnalysisType {

	private final int rawFreq;
	private final double relativeFreq;
	
	public RawPCAType(String type, int rawFreq, double relativeFreq, double[] vector) {
		super(type, vector);
		this.rawFreq = rawFreq;
		this.relativeFreq = relativeFreq;
	}

	public int getRawFreq() {
		return this.rawFreq;
	}

	public double getRelativeFreq() {
		return this.relativeFreq;
	}

}

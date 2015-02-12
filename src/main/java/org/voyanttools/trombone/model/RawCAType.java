package org.voyanttools.trombone.model;

/**
 * @author Andrew MacDonald
 */
public class RawCAType extends RawPCAType {

	public static final int WORD = 0;
	public static final int PART = 1;
	
	private final int category;
	
	public RawCAType(String type, int rawFreq, double relativeFreq, double[] vector, int category) {
		super(type, rawFreq, relativeFreq, vector);
		if (category != WORD && category != PART) {
			category = WORD;
		}
		this.category = category;
	}
	
	public int getCategory() {
		return this.category;
	}

}
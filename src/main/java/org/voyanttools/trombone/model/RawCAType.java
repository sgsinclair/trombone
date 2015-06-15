package org.voyanttools.trombone.model;

/**
 * @author Andrew MacDonald
 */
public class RawCAType extends RawPCAType {

	public static final String WORD = "term";
	public static final String PART = "part";
	
	private final String category;
	
	public RawCAType(String type, int rawFreq, double relativeFreq, double[] vector, String category) {
		super(type, rawFreq, relativeFreq, vector);
		if (category != WORD && category != PART) {
			category = WORD;
		}
		this.category = category;
	}
	
	public String getCategory() {
		return this.category;
	}

}
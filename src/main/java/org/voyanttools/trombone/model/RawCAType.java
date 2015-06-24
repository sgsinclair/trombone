package org.voyanttools.trombone.model;

/**
 * @author Andrew MacDonald
 */
public class RawCAType extends RawPCAType {

	public static final String WORD = "term";
	public static final String PART = "part";
	
	private final String category;
	private final int docIndex;
	
	public RawCAType(String type, int rawFreq, double relativeFreq, double[] vector, String category, int docIndex) {
		super(type, rawFreq, relativeFreq, vector);
		if (category != WORD && category != PART) {
			category = WORD;
		}
		this.category = category;
		this.docIndex = docIndex;
	}
	
	public String getCategory() {
		return this.category;
	}
	
	public int getDocIndex() {
		return this.docIndex;
	}

}
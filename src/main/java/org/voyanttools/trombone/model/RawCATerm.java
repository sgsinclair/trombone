package org.voyanttools.trombone.model;

/**
 * @author Andrew MacDonald
 */
public class RawCATerm extends RawPCATerm {

	public static final String TERM = "term";
	public static final String DOC = "doc";
	public static final String BIN = "bin";
	
	private final String category;
	private final int docIndex;
	
	public RawCATerm(String term, int rawFrequency, double relativeFrequency, double[] vector, String category, int docIndex) {
		super(term, rawFrequency, relativeFrequency, vector);
		if (category != TERM && category != DOC && category != BIN) {
			category = TERM;
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

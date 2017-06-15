package org.voyanttools.trombone.model;

/**
 * @author Andrew MacDonald
 */
public class RawCATerm extends RawPCATerm {

	public enum CategoryType {
		TERM, DOCUMENT, BIN
	}
	
	private CategoryType category;
	private int docIndex;
	
	public RawCATerm(String term, int rawFrequency, double relativeFrequency, CategoryType category) {
		super(term, rawFrequency, relativeFrequency);
		this.category = category;
	}
	
	public RawCATerm(String term, int rawFrequency, double relativeFrequency, CategoryType category, int docIndex) {
		super(term, rawFrequency, relativeFrequency);
		this.category = category;
		this.docIndex = docIndex;
	}
	
	public RawCATerm(String term, int rawFrequency, double relativeFrequency, double[] vector) {
		super(term, rawFrequency, relativeFrequency, vector);
		
	}
	
	public RawCATerm(String term, int rawFrequency, double relativeFrequency, double[] vector, CategoryType category, int docIndex) {
		super(term, rawFrequency, relativeFrequency, vector);
		this.category = category;
		this.docIndex = docIndex;
	}
	
	public CategoryType getCategory() {
		return category;
	}
	public void setCategory(CategoryType category) {
		this.category = category;
	}
	
	public int getDocIndex() {
		return docIndex;
	}
	public void setDocIndex(int docIndex) {
		this.docIndex = docIndex;
	}

}

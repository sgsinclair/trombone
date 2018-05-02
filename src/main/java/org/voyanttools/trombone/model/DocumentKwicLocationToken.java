/**
 * 
 */
package org.voyanttools.trombone.model;

/**
 * @author sgs
 *
 */
public class DocumentKwicLocationToken extends DocumentLocationToken {
	
	private String left;
	private String right;

	/**
	 * @param docIndex
	 * @param term
	 * @param position
	 * @param confidences
	 * @param location
	 */
	public DocumentKwicLocationToken(int docIndex, String term, int position, Confidence[] confidences,
			Location location, String left, String right) {
		super(docIndex, term, position, confidences, location);
		this.left = left;
		this.right = right;
	}

	public DocumentKwicLocationToken(DocumentLocationToken token, String left, String right) {
		super(token.docIndex, token.term, token.position, token.confidences, token.location);
		this.left = left;
		this.right = right;
	}

	public String getLeft() {
		return left;
	}
	public String getRight() {
		return right;
	}
}

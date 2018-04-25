/**
 * 
 */
package org.voyanttools.trombone.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author sgs
 *
 */
public class DocumentLocationToken extends DocumentEntityToken implements Serializable {
	
	private Location location;

	/**
	 * @param docId
	 * @param docIndex
	 * @param term
	 * @param normalizedTerm
	 * @param tokenType
	 * @param entityType
	 * @param position
	 * @param startOffset
	 * @param endOffset
	 * @param confidences
	 */
	public DocumentLocationToken(int docIndex, String term, int position, Confidence[] confidences, Location location) {
		super(docIndex, term, EntityType.location, position, confidences);
		this.location = location;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(").append(docIndex).append(":").append(position).append(") ").append(term).append(" (").append(location.getBestName()).append("): ").append(Confidence.getConfidence(Arrays.asList(confidences)));
		return sb.toString();
	}

	public Location getLocation() {
		return location;
	}

	public int getPosition() {
		return position;
	}
	
	public float getConfidence() {
		return Confidence.getConfidence(confidences);
	}

	public Confidence[] getConfidences() {
		return confidences;
	}
}

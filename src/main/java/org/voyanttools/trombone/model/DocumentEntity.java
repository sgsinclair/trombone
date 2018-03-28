/**
 * 
 */
package org.voyanttools.trombone.model;

import java.io.Serializable;

/**
 * @author sgs
 *
 */
public class DocumentEntity implements Serializable, Comparable<DocumentEntity> {

	private int docIndex;
	private String term;
	private String normalized;
	private EntityType type;
	private int rawFreq;
	private int[] positions;
	private float[] confidences;

	public DocumentEntity(int docIndex, String term, String normalized, EntityType type, int rawFreq, int[] positions, float[] confidences) {
		this.docIndex = docIndex;
		this.term = term;
		this.normalized = normalized;
		this.type = type;
		this.rawFreq = rawFreq;
		this.positions = positions;
		this.confidences = confidences;
	}
	
	public int getDocIndex() {
		return docIndex;
	}
	
	public String getTerm() {
		return term;
	}

	@Override
	public int compareTo(DocumentEntity o) {
		return term.compareTo(o.term);
	}

	public int getRawFreq() {
		return rawFreq;
	}

	public EntityType getType() {
		return type;
	}

}

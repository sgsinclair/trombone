/**
 * 
 */
package org.voyanttools.trombone.model;

import java.io.Serializable;

/**
 * @author sgs
 *
 */
public class DocumentEntityToken implements Serializable {

	protected int docIndex;
	protected String term;
	protected EntityType entityType;
	protected int position;
	protected Confidence[] confidences;

	/**
	 * 
	 */
	public DocumentEntityToken(int docIndex, String term, EntityType entityType, int position, Confidence[] confidences) {
		this.docIndex = docIndex;
		this.term = term;
		this.entityType = entityType;
		this.position = position;
		this.confidences = confidences;
	}
	
	public String getTerm() {
		return term;
	}
	
	public int getDocIndex() {
		return docIndex;
	}


}

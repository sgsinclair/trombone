/**
 * 
 */
package org.voyanttools.trombone.model;

/**
 * @author sgs
 *
 */
public class LocatedDocumentEntity extends DocumentEntity {

	/**
	 * @param docIndex
	 * @param term
	 * @param normalized
	 * @param type
	 * @param rawFreq
	 * @param positions
	 * @param confidences
	 */
	public LocatedDocumentEntity(int docIndex, String term, String normalized, EntityType type, int rawFreq,
			int[] positions, float[] confidences) {
		super(docIndex, term, normalized, type, rawFreq, positions, confidences);
		// TODO Auto-generated constructor stub
	}

}

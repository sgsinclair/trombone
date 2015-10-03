/**
 * 
 */
package org.voyanttools.trombone.model;

import java.io.Serializable;

/**
 * @author sgs
 *
 */
public class CorpusEntity implements Serializable, Cloneable {

	private String term;
	private EntityType type;
	private int rawFreq;
	private int[] rawFreqs;
	
	/**
	 * 
	 */
	public CorpusEntity(String term, EntityType type, int rawFreq, int[] rawFreqs) {
		this.term = term;
		this.type = type;
		this.rawFreq = rawFreq;
		this.rawFreqs = rawFreqs;
	}

	public String getTerm() {
		return term;
	}

	public EntityType getType() {
		return type;
	}
	
	public CorpusEntity clone() {
		return new CorpusEntity(term, type, rawFreq, rawFreqs);
	}
 
}

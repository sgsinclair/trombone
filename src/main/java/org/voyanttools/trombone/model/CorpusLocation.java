/**
 * 
 */
package org.voyanttools.trombone.model;

import java.io.Serializable;

/**
 * @author sgs
 *
 */
public class CorpusLocation implements Serializable {
	
	private Location location;
	private String[] forms;
	private int rawFreq;

	/**
	 * 
	 */
	public CorpusLocation(Location location, String[] forms, int rawFreq) {
		this.location = location;
		this.forms = forms;
		this.rawFreq = rawFreq;
	}
	
	public String getId() {
		return location.getId();
	}
	
	public Location getLocation() {
		return location;
	}
	public String[] getForms() {
		return forms;
	}
	public int getRawFreq() {
		return rawFreq;
	}

}

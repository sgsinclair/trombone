/**
 * 
 */
package org.voyanttools.trombone.model;

import java.io.Serializable;

/**
 * @author sgs
 *
 */
public class CorpusLocationConnection implements Comparable<CorpusLocationConnection>, Serializable {
	
	private Location[] locations;
	private int rawFreq;

	/**
	 * 
	 */
	public CorpusLocationConnection(Location[] locations, int rawFreq) {
		this.locations = locations;
		this.rawFreq = rawFreq;
	}

	public Location[] getLocations() {
		return locations;
	}

	public int getRawFreq() {
		return rawFreq;
	}

	@Override
	public int compareTo(CorpusLocationConnection o) {
		return Integer.compare(o.rawFreq, rawFreq);
	}

}

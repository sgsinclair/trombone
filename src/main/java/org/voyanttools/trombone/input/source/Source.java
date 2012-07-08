/**
 * 
 */
package org.voyanttools.trombone.input.source;

/**
 * Defines the provenence of a document source or {@link #UNKNOWN}.
 * 
 * @author Stéfan Sinclair
 */
public enum Source {
	
	/**
	 * a source from a URI
	 */
	URI,
	
	/**
	 * a source from a local file
	 */
	FILE,
	
	/**
	 * a source form a string
	 */
	STRING,
	
	/**
	 * a transient stream (such as from a compressed or archived file)
	 */
	STREAM,
	
	/**
	 * an unknown source
	 */
	UNKNOWN
}

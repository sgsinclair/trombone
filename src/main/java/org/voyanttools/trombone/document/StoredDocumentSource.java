/**
 * 
 */
package org.voyanttools.trombone.document;

import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;


/**
 * This is a very light wrapper around the ID of a stored document source and
 * its associated metadata. It should be assumed that a StoredDocumentSource
 * has indeed been stored and is ready to be read (via the {@link StoredDocumentSourceStorage}).
 * 
 * @author Stéfan Sinclair
 */
public class StoredDocumentSource {

	/**
	 * the document's ID (to allow the storage to retrieve it)
	 */
	private String id;
	
	/**
	 * the document's known metadata
	 */
	private Metadata metadata;
	
	/**
	 * Create a new instance of this object with the specified Id and {@link Metadata}
	 * @param id the stored document source's ID
	 * @param metadata the stored document source's {@link Metadata}
	 */
	public StoredDocumentSource(String id, Metadata metadata) {
		this.id = id;
		this.metadata = metadata;
	}

	/**
	 * Get this stored document source's {@link Metadata}
	 * @return this stored document source's {@link Metadata}
	 */
	public Metadata getMetadata() {
		return metadata;
	}

	/**
	 * Get this stored document source's ID
	 * @return this stored document source's ID
	 */
	public String getId() {
		return id;
	}
}

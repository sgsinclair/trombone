/*******************************************************************************
 * Trombone is a flexible text processing and analysis library used
 * primarily by Voyant Tools (voyant-tools.org).
 * 
 * Copyright (©) 2007-2012 Stéfan Sinclair & Geoffrey Rockwell
 * 
 * This file is part of Trombone.
 * 
 * Trombone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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

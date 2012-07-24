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
 * Trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.input.expand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.voyanttools.trombone.document.DocumentFormat;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * This is the main stored document source expander that calls other expanders
 * as needed. When this class's {#link
 * {@link #getExpandedStoredDocumentSources(StoredDocumentSource)} is called
 * with a stored document source that doesn't need expansion, the same
 * StoredDocumentSource is return (but as part of a list).
 * 
 * @author Stéfan Sinclair
 */
public class StoredDocumentSourceExpander implements Expander {

	/**
	 * all parameters sent, only some of which may be relevant to some expanders
	 */
	private FlexibleParameters parameters;

	/**
	 * the storage strategy to use for storing document sources
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;

	/**
	 * the expander for compressed archives
	 */
	private Expander archiveExpander;

	/**
	 * the expander for compressed archives
	 */
	private Expander compressedExpander;

	/**
	 * the expander for XML documents
	 */
	private Expander xmlExpander;

	/**
	 * Create a new instance of this expander with the specified storage
	 * strategy.
	 * 
	 * @param storedDocumentSourceStorage
	 *            the storage handler for document sources
	 */
	public StoredDocumentSourceExpander(
			StoredDocumentSourceStorage storedDocumentSourceStorage) {
		this(storedDocumentSourceStorage, new FlexibleParameters());
	}

	/**
	 * Create a new instance of this expander with the specified storage
	 * strategy.
	 * 
	 * @param storedDocumentSourceStorage
	 *            the storage handler for document sources
	 * @param parameters
	 *            that may be relevant to the expanders
	 */
	public StoredDocumentSourceExpander(
			StoredDocumentSourceStorage storedDocumentSourceStorage,
			FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.archiveExpander = null;
		this.compressedExpander = null;
		this.xmlExpander = null;
		this.parameters = parameters;
	}

	public List<StoredDocumentSource> getExpandedStoredDocumentSources(
			StoredDocumentSource storedDocumentSource) throws IOException {

		List<StoredDocumentSource> storedDocumentSources = new ArrayList<StoredDocumentSource>();

		DocumentFormat format = storedDocumentSource.getMetadata()
				.getDocumentFormat();
		if (format == DocumentFormat.ARCHIVE) {
			storedDocumentSources.addAll(expandArchive(storedDocumentSource));
		} else if (format == DocumentFormat.COMPRESSED) {
			storedDocumentSources
					.addAll(expandCompressed(storedDocumentSource));
		} else if (format.isXml()) {
			storedDocumentSources.addAll(expandXml(storedDocumentSource));
		}

		// no expansion needed or known
		else {
			storedDocumentSources.add(storedDocumentSource);
		}

		return storedDocumentSources;
	}

	/**
	 * Expand the specified StoredDocumentSource archive and add it to the
	 * specified list of StoredDocumentSources.
	 * 
	 * @param storedDocumentSource
	 *            the stored document source to expand (or add as is)
	 * @return a list of expanded document sources
	 * @throws IOException
	 *             an IO Exception
	 */
	List<StoredDocumentSource> expandArchive(
			StoredDocumentSource storedDocumentSource) throws IOException {
		if (this.archiveExpander == null) {
			this.archiveExpander = new ArchiveExpander(
					storedDocumentSourceStorage, this);
		}
		return this.archiveExpander
				.getExpandedStoredDocumentSources(storedDocumentSource);
	}

	/**
	 * Expand the specified StoredDocumentSource archive and add it to the
	 * specified list of StoredDocumentSources.
	 * 
	 * @param storedDocumentSource
	 *            the stored document source to expand (or add as is)
	 * @return a list of expanded document sources
	 * @throws IOException
	 *             an IO Exception
	 */
	List<StoredDocumentSource> expandCompressed(
			StoredDocumentSource storedDocumentSource) throws IOException {
		if (this.compressedExpander == null) {
			this.compressedExpander = new CompressedExpander(
					storedDocumentSourceStorage, this);
		}
		return this.compressedExpander
				.getExpandedStoredDocumentSources(storedDocumentSource);
	}

	/**
	 * Expand the specified StoredDocumentSource archive and add it to the
	 * specified list of StoredDocumentSources.
	 * 
	 * @param storedDocumentSource
	 *            the stored document source to expand (or add as is)
	 * @return a list of expanded document sources
	 * @throws IOException
	 *             an IO Exception
	 */
	List<StoredDocumentSource> expandXml(
			StoredDocumentSource storedDocumentSource) throws IOException {
		if (this.xmlExpander == null) {
			this.xmlExpander = new XmlExpander(storedDocumentSourceStorage,
					parameters);
		}
		// this will deal fine when no expansion is needed
		return this.xmlExpander.getExpandedStoredDocumentSources(storedDocumentSource);
	}
}

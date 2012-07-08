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
package org.voyanttools.trombone.input;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.expand.Expander;
import org.voyanttools.trombone.input.expand.StoredDocumentSourceExpander;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.input.source.UriInputSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * This is a utility for getting a list of stored document sources based on
 * provided parameters or on a specific URI, String or File. Note that the list
 * of stored document sources are expanded automatically (compressed archives,
 * for instance).
 * 
 * @author Stéfan Sinclair
 */
public class ExpandedStoredDocumentSourcesBuilder {

	/**
	 * the storage strategy for stored document sources
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;

	/**
	 * the primary expander (actually {@link StoredDocumentSourceExpander})
	 */
	private Expander expander;

	/**
	 * the parameters for this builder
	 */
	private FlexibleParameters parameters;

	/**
	 * Create a new instance with the specified storage strategy (and no
	 * parameters).
	 * 
	 * @param storedDocumentSourceStorage
	 *            the storage strategy for stored document sources
	 */
	public ExpandedStoredDocumentSourcesBuilder(
			StoredDocumentSourceStorage storedDocumentSourceStorage) {
		this(storedDocumentSourceStorage, new FlexibleParameters());
	}

	/**
	 * Create a new instance with the specified storage strategy and specified
	 * parameters. The parameters are used to specify sources (like {@code file}
	 * , {@code string} and {@code uri} keys) as well as by some source
	 * expanders.
	 * 
	 * (like when expanding a single XML document into multiple documents).
	 * 
	 * @param storedDocumentSourceStorage
	 *            the storage strategy for stored document sources
	 * @param parameters
	 *            any relevant parameters that can be used by expanders
	 */
	public ExpandedStoredDocumentSourcesBuilder(
			StoredDocumentSourceStorage storedDocumentSourceStorage,
			FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.expander = new StoredDocumentSourceExpander(
				storedDocumentSourceStorage, parameters);
	}

	/**
	 * Get the expanded stored document sources as defined in the parameters
	 * (like {@code file}, {@code string} and {@code uri} keys).
	 * 
	 * @return a list of expanded and stored document sources defined from the
	 *         parameters
	 * @throws IOException
	 *             an IO exception that occurred while creating the stored
	 *             document sources
	 * @throws URISyntaxException
	 *             occurs when one of the parameters defines a bad URI
	 */
	public List<StoredDocumentSource> getStoredDocumentSources()
			throws IOException, URISyntaxException {
		List<StoredDocumentSource> storedDocumentSources = new ArrayList<StoredDocumentSource>();
		for (String file : parameters.getParameterValues("file")) {
			storedDocumentSources
					.addAll(getStoredDocumentSources(new File(file)));
		}

		for (String string : parameters.getParameterValues("string")) {
			storedDocumentSources.addAll(getStoredDocumentSources(string));
		}

		for (String uri : parameters.getParameterValues("uri")) {
			storedDocumentSources
					.addAll(getStoredDocumentSources(new URI(uri)));
		}

		return storedDocumentSources;
	}

	/**
	 * Get the expanded stored document sources defined by the specified URI.
	 * 
	 * @param uri
	 *            the URI from which to extract and create stored and expanded
	 *            documents
	 * @return a list of expanded and stored document sources
	 * @throws IOException
	 *             when an IO exception is encountered
	 * @throws URISyntaxException
	 *             when a bad URI is specified
	 */
	public List<StoredDocumentSource> getStoredDocumentSources(URI uri)
			throws IOException, URISyntaxException {
		InputSource inputSource = new UriInputSource(uri);
		StoredDocumentSource storedDocumentSource = this.storedDocumentSourceStorage
				.getStoredDocumentSourceId(inputSource);
		return this.expander
				.getExpandedStoredDocumentSources(storedDocumentSource);
	}

	/**
	 * Get the expanded stored document sources defined by the specified string.
	 * 
	 * @param string
	 *            the string (actual content)
	 * @return a list of expanded and stored document sources
	 * @throws IOException
	 *             when an IO exception is encountered
	 */
	public List<StoredDocumentSource> getStoredDocumentSources(String string)
			throws IOException {
		InputSource inputSource = new StringInputSource(string);
		StoredDocumentSource storedDocumentSource = this.storedDocumentSourceStorage
				.getStoredDocumentSourceId(inputSource);
		return this.expander
				.getExpandedStoredDocumentSources(storedDocumentSource);
	}

	/**
	 * Get the expanded stored document sources defined by the file. The file
	 * can be a single file or a directory that will be expanded.
	 * 
	 * @param file
	 *            the file (or directory) from which to extract and create
	 *            stored and expanded documents
	 * @return a list of expanded and stored document sources
	 * @throws IOException
	 *             when an IO exception is encountered
	 */
	public List<StoredDocumentSource> getStoredDocumentSources(File file)
			throws IOException {

		List<StoredDocumentSource> storedDocumentSources = new ArrayList<StoredDocumentSource>();

		// directories don't get cached, so handle them differently
		if (file.isDirectory()) {
			final File[] files = file.listFiles();
			Arrays.sort(files); // make sure files are in sorted order
			for (File f : files) {
				storedDocumentSources.addAll(getStoredDocumentSources(f));
			}
		} else {
			InputSource inputSource = new FileInputSource(file);
			StoredDocumentSource storedDocumentSource = this.storedDocumentSourceStorage
					.getStoredDocumentSourceId(inputSource);
			storedDocumentSources.addAll(this.expander
					.getExpandedStoredDocumentSources(storedDocumentSource));
		}

		return storedDocumentSources;
	}

}

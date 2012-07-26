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
package org.voyanttools.trombone.input.source;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;

import org.apache.commons.codec.digest.DigestUtils;
import org.voyanttools.trombone.document.DocumentFormat;
import org.voyanttools.trombone.document.Metadata;

/**
 * An {@link InputSource} associated with a URI.
 * 
 * @author Stéfan Sinclair
 */
public class UriInputSource implements InputSource {

	/**
	 * the URI for this input source
	 */
	private URI uri;

	/**
	 * the id (hash) for this input source
	 */
	private String id;

	/**
	 * the metadata for this input source
	 */
	private Metadata metadata;

	/**
	 * Create a new instance with the specified URI.
	 * 
	 * @param uri the URI associated with this input source
	 * @throws IOException
	 *             thrown when there's a problem creating or accessing header
	 *             information for the URI
	 * @throws MalformedURLException
	 *             thrown if the URI is malformed
	 */
	public UriInputSource(URI uri) throws IOException {
		this.uri = uri;
		this.metadata = new Metadata();
		this.metadata.setLocation(uri.toString());
		this.metadata.setSource(Source.URI);

		StringBuilder idBuilder = new StringBuilder(uri.toString());

		// establish connection to find other and default metadata
		URLConnection c = null;
		try {
			c = getURLConnection(uri, 15000, 10000);

			// last modified of file
			long modified = c.getLastModified();
			this.metadata.setModified(modified);
			idBuilder.append(modified);

			// try and get length for id
			int length = c.getContentLength();
			idBuilder.append(length);

			String format = c.getContentType();
			if (format != null && format.isEmpty() == false) {
				idBuilder.append(format);
				DocumentFormat docFormat = DocumentFormat
						.fromContentType(format);
				if (docFormat != DocumentFormat.UNKNOWN) {
					this.metadata.setDefaultFormat(docFormat);
				}
			}

		} finally {
			if (c != null && c instanceof HttpURLConnection) {
				((HttpURLConnection) c).disconnect();
			}
		}

		this.id = DigestUtils.md5Hex(idBuilder.toString());
	}
	
	private URLConnection getURLConnection(URI uri) throws IOException {
		return getURLConnection(uri, 60000, 15000);
	}
	
	private URLConnection getURLConnection(URI uri, int readTimeoutMilliseconds, int connectTimeoutMilliseconds) throws IOException {
		URLConnection c;
		try {
			c = uri.toURL().openConnection();
		}
		catch (MalformedURLException e) {
			throw new IllegalArgumentException("Attempt to use a malformed URL: "+uri, e);
		}
		c.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; Trombone)");
        c.setReadTimeout(readTimeoutMilliseconds); 
        c.setConnectTimeout(connectTimeoutMilliseconds);
		return c;
	}
	
	public InputStream getInputStream() throws MalformedURLException,
			IOException {
		// let's hope that the connection is close when the stream is closed
		URLConnection c = getURLConnection(uri);
		return c.getInputStream();
	}

	public Metadata getMetadata() {
		return this.metadata;
	}

	public String getUniqueId() {
		return this.id;
	}

}

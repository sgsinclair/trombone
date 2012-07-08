/**
 * 
 */
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
	public UriInputSource(URI uri) throws MalformedURLException, IOException {
		this.uri = uri;
		this.metadata = new Metadata();
		this.metadata.setLocation(uri.toString());
		this.metadata.setSource(Source.URI);

		StringBuilder idBuilder = new StringBuilder(uri.toString());

		// establish connection to find other and default metadata
		URLConnection c = null;
		try {
			c = uri.toURL().openConnection();

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

	public InputStream getInputStream() throws MalformedURLException,
			IOException {
		return uri.toURL().openStream();
	}

	public Metadata getMetadata() {
		return this.metadata;
	}

	public String getUniqueId() {
		return this.id;
	}

}

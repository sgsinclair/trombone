/**
 * 
 */
package org.voyanttools.trombone.input.source;

import java.io.IOException;
import java.io.InputStream;

import org.voyanttools.trombone.document.Metadata;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;

/**
 * This interface defines a flexible input source that can be from a variety of
 * {@link Source} locations (URI, File, String, Stream). Typically an {@link InputSource}
 * is created and then stored using a {@link StoredDocumentSourceStorage} (which
 * also returns a {@link StoredDocumentSource}).
 * 
 * @author Stéfan Sinclair
 */
public interface InputSource {

	/**
	 * Get the {@link InputStream} associated with the input source – callers should
	 * make sure to close the stream when finished.
	 * 
	 * @return the {@link InputStream} of this input source
	 * @throws IOException if an IO exception occurs
	 */
	public InputStream getInputStream() throws IOException;

	/**
	 * Get the {@link Metadata} associated with this input source.
	 * 
	 * @return the {@link Metadata} associated with this input source.
	 */
	public Metadata getMetadata();

	/**
	 * Get the unique ID associate with this input source. This should me a hash
	 * code that represents several characteristics of the input source and allows
	 * it to retrieve the same source (based on the characteristics) and differentiate
	 * with different input sources.
	 * 
	 * @return the unique ID associate with this input source
	 */
	public String getUniqueId();

}

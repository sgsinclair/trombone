/**
 * 
 */
package org.voyanttools.trombone.input.source;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.voyanttools.trombone.document.Metadata;

/**
 * An {@link InputSource} associated with a real, local {@link File} (not a directory).
 * 
 * @author Stéfan Sinclair
 */
public class FileInputSource implements InputSource {

	/**
	 * the file for this input source
	 */
	private File file;
	
	/**
	 * the id (hash) for this input source
	 */
	private String id;
	
	/**
	 * the metadata for this input source
	 */
	private Metadata metadata;

	/**
	 * Create a new instance with the specified {@link File}.
	 * 
	 * @param file
	 *            the {@link File} for this input source
	 * @throws IOException
	 *             thrown if the File is a directory or another IO problem is
	 *             encountered
	 */
	public FileInputSource(File file) throws IOException {

		if (file.isDirectory()) {
			throw new IOException(
					"Directories should be expanded before creating a FileInputSource: "
							+ file.toString());
		}
		this.file = file;

		this.metadata = new Metadata();
		this.metadata.setLocation(file.toString());
		this.metadata.setSource(Source.FILE);
		this.metadata.setModified(file.lastModified());
		String id = metadata.getLocation()
				+ String.valueOf(metadata.getModified())
				+ String.valueOf(file.length());
		this.id = DigestUtils.md5Hex(id);
	}

	public InputStream getInputStream() throws IOException {
		return new BufferedInputStream(new FileInputStream(file));
	}

	public Metadata getMetadata() {
		return this.metadata;
	}

	public String getUniqueId() {
		return this.id;
	}

}

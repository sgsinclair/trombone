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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.voyanttools.trombone.model.DocumentMetadata;

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
	private DocumentMetadata metadata;

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

		this.metadata = new DocumentMetadata();
		this.metadata.setLocation(file.toString());
		this.metadata.setSource(Source.FILE);
		this.metadata.setModified(file.lastModified());
		this.metadata.setTitle(file.getName().replaceFirst("\\.\\w+$", "")); // default to filename
		String id = metadata.getLocation()
				+ String.valueOf(metadata.getModified())
				+ String.valueOf(file.length());
		this.id = DigestUtils.md5Hex(id);
	}

	public InputStream getInputStream() throws IOException {
		return new BufferedInputStream(new FileInputStream(file));
	}

	public DocumentMetadata getMetadata() {
		return this.metadata;
	}

	public String getUniqueId() {
		return this.id;
	}
	
	public File getFile() {
		return file;
	}

}

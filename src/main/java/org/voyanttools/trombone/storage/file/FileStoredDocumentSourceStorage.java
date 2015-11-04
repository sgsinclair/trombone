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
package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * This is a file-system based adapter for working with stored document sources.
 * 
 * @author Stéfan Sinclair
 */
class FileStoredDocumentSourceStorage implements StoredDocumentSourceStorage {

	/**
	 * the raw bytes file name
	 */
	private static final String RAW_BYTES_FILENAME = "raw_bytes.gz";

	/**
	 * the metadata file name
	 */
	private static final String METADATA_FILENAME = "metadata.xml";

	/**
	 * the directory name to use for stored document sources (under the storage
	 * directory)
	 */
	private static final String STORED_DOCUMENT_SOURCE_DIRECTORY_NAME = "stored_document_sources";

	/**
	 * the name of the file that contains multiple expanded stored document
	 * sources (if they exist)
	 */
	private static final String MULTIPLE_EXPANDED_STORED_DOCUMENT_SOURCE_IDS_FILENAME = "multiple_expanded_stored_document_source_ids.txt";;

	/**
	 * the actual File (directory) for the stored document sources
	 */
	private File documentSourcesDirectory;

	/**
	 * Create a new instance of this object with the specified File (directory)
	 * of the parent Storage. This class shouldn't be instantiated except by
	 * FileStorage.
	 * 
	 * @param storageLocation
	 *            the File (directory) of the parent FileStorage
	 */
	FileStoredDocumentSourceStorage(File storageLocation) {

		this.documentSourcesDirectory = new File(storageLocation,
				STORED_DOCUMENT_SOURCE_DIRECTORY_NAME);
		if (this.documentSourcesDirectory.exists() == false) {
			this.documentSourcesDirectory.mkdir(); // shouldn't need to create
													// parents
		}
	}

	public StoredDocumentSource getStoredDocumentSource(
			InputSource inputSource) throws IOException {

		String id = inputSource.getUniqueId();
		File directory = getDocumentSourceDirectory(id);
		File metadataFile = getMetadataFile(id);
		File rawbytesFile = getRawbytesFile(id);

		// this directory and contents exists, so just return the DocumentSource
		if (directory.exists()) {
			if (metadataFile.exists() && rawbytesFile.exists()) {
				return new StoredDocumentSource(directory.getName(), inputSource.getMetadata());
			}
			// let's keep going in case there was an error last time
		} else {
			directory.mkdir(); // shouldn't need to create parents
		}

		InputStream inputStream = null;
		try {
			inputStream = inputSource.getInputStream();
			storeStoredDocumentSourceInputStream(id, inputStream);
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}

		DocumentMetadata metadata = inputSource.getMetadata(); // get this after reading input stream in case it's changed (like after extraction)
		
		storeStoredDocumentSourceMetadata(id, metadata);
		return new StoredDocumentSource(directory.getName(), metadata);
	}
	
	private void storeStoredDocumentSourceMetadata(String id, DocumentMetadata metadata) throws IOException {
		metadata.getFlexibleParameters().saveFlexibleParameters(getMetadataFile(id));
	}
	
	private void storeStoredDocumentSourceInputStream(String id, InputStream inputStream) throws IOException {
		File rawbytesFile = getRawbytesFile(id);
		OutputStream zippedOutputStream = null;
		try {
			OutputStream fileOutputStream = new FileOutputStream(rawbytesFile);
			zippedOutputStream  = new GZIPOutputStream(fileOutputStream);
			IOUtils.copy(inputStream, zippedOutputStream);
		}
		finally {
			if (zippedOutputStream != null) {
				zippedOutputStream.close();
			}
		}
	}

	/*
	public StoredDocumentSource getStoredDocumentSource(
			ExtractableStoredDocumentSource extractableStoredDocumentSource)
			throws IOException {

		String id = extractableStoredDocumentSource.getUniqueId();
		File directory = getDocumentSourceDirectory(id);
		File metadataFile = getMetadataFile(id);
		File rawbytesFile = getRawbytesFile(id);

		// this directory and contents exists, so just return the DocumentSource
		if (directory.exists()) {
			if (metadataFile.exists() && rawbytesFile.exists()) {
				// we'll grab the stored metadata in case it has more goodies
				Metadata metadata = this.getStoredDocumentSourceMetadata(id);
				return new StoredDocumentSource(id, metadata);
			}
			// let's keep going in case there was an error last time
		} else {
			directory.mkdir(); // shouldn't need to create parents
		}

		InputStream inputStream = null;
		try {
			inputStream = extractableStoredDocumentSource.getInputStream();
			storeStoredDocumentSourceInputStream(id, inputStream);
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}

		Metadata metadata = extractableStoredDocumentSource.getMetadata();
		storeStoredDocumentSourceMetadata(id, metadata);

		return new StoredDocumentSource(directory.getName(), metadata);

	}
	*/

	public DocumentMetadata getStoredDocumentSourceMetadata(String id)
			throws IOException {
		FlexibleParameters parameters = FlexibleParameters.loadFlexibleParameters(getMetadataFile(id));
		return new DocumentMetadata(parameters);
	}

	public InputStream getStoredDocumentSourceInputStream(String id)
			throws IOException {
		File file = getRawbytesFile(id);
		FileInputStream fileInputStream = new FileInputStream(file);		
		return new GZIPInputStream(fileInputStream);
	}
	
	public List<StoredDocumentSource> getMultipleExpandedStoredDocumentSources(
			String id) throws IOException {
		return getMultipleExpandedStoredDocumentSources(id, "");
	}

	public List<StoredDocumentSource> getMultipleExpandedStoredDocumentSources(
			String id, String prefix) throws IOException {
		
		List<StoredDocumentSource> multipleExpandedStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		File file = getMultipleExpandedStoredDocumentSourcesFile(id, prefix);
		if (file.exists()==false) {return multipleExpandedStoredDocumentSources;}
		
		List<String> lines = FileUtils.readLines(file);
		for (String line : lines) {
			DocumentMetadata metadata = getStoredDocumentSourceMetadata(line.trim());
			multipleExpandedStoredDocumentSources.add(new StoredDocumentSource(line, metadata));
		}
		
		return multipleExpandedStoredDocumentSources;
	}

	public void setMultipleExpandedStoredDocumentSources(String id,
			List<StoredDocumentSource> multipleExpandedStoredDocumentSources) throws IOException {
		setMultipleExpandedStoredDocumentSources(id, multipleExpandedStoredDocumentSources, "");
	}

	public void setMultipleExpandedStoredDocumentSources(String id,
			List<StoredDocumentSource> multipleExpandedStoredDocumentSources, String prefix) throws IOException {
		List<String> multipleExpandedStoredDocumentSourceIds = new ArrayList<String>();
		for (StoredDocumentSource doc : multipleExpandedStoredDocumentSources) {
			multipleExpandedStoredDocumentSourceIds.add(doc.getId());
		}
		File file = getMultipleExpandedStoredDocumentSourcesFile(id, prefix);
		FileUtils.writeLines(file, multipleExpandedStoredDocumentSourceIds);
	}

	/**
	 * Get the File (directory) that corresponds to this ID. This method has
	 * default visibility for unit tests but should otherwise be considered
	 * private.
	 * 
	 * @param id
	 *            the ID of the StoredDocumentSource
	 * @return the File (directory) of the StoredDocumentSource
	 */
	File getDocumentSourceDirectory(String id) {
		return new File(documentSourcesDirectory, id);
	}

	/**
	 * Get the raw bytes File for that corresponds to this ID. This method has
	 * default visibility for unit tests but should otherwise be considered
	 * private.
	 * 
	 * @param id
	 *            the ID of the StoredDocumentSource
	 * @return the rawbytes File for the specified StoredDocumentSource
	 */
	File getRawbytesFile(String id) {
		return new File(getDocumentSourceDirectory(id), RAW_BYTES_FILENAME);
	}

	/**
	 * Get the metadata File that corresponds to this ID. This method has
	 * default visibility for unit tests but should otherwise be considered
	 * private.
	 * 
	 * @param id
	 *            the ID of the StoredDocumentSource
	 * @return the rawbytes File for the specified StoredDocumentSource
	 */
	File getMetadataFile(String id) {
		return new File(getDocumentSourceDirectory(id), METADATA_FILENAME);
	}

	/**
	 * Get the multiple expanded stored document source ids File that
	 * corresponds to this ID. This method has default visibility for unit tests
	 * but should otherwise be considered private.
	 * 
	 * @param id
	 *            the ID of the StoredDocumentSource
	 * @param prefix
	 *            a prefix that specifies an identifier for the parameters used
	 *            during expansion
	 * @return the multiple expanded stored document source ids File for the
	 *         specified StoredDocumentSource
	 */
	File getMultipleExpandedStoredDocumentSourcesFile(String id, String prefix) {
		return new File(getDocumentSourceDirectory(id),
				prefix+MULTIPLE_EXPANDED_STORED_DOCUMENT_SOURCE_IDS_FILENAME);
	}

	@Override
	public void updateStoredDocumentSourceMetadata(String id, DocumentMetadata metadata) throws IOException {
		storeStoredDocumentSourceMetadata(id, metadata);
	}

}

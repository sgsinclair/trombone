/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.voyanttools.trombone.document.Metadata;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;

/**
 * This is a file-system based adapter for working with stored document sources.
 * 
 * @author Stéfan Sinclair
 */
class FileStoredDocumentSourceStorage implements StoredDocumentSourceStorage {

	/**
	 * the raw bytes file name
	 */
	private static final String RAW_BYTES_FILENAME = "raw_bytes";

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

	public StoredDocumentSource getStoredDocumentSourceId(
			InputSource inputSource) throws IOException {
		Metadata metadata = inputSource.getMetadata();

		String id = inputSource.getUniqueId();
		File directory = getDocumentSourceDirectory(id);
		File metadataFile = getMetadataFile(id);
		File rawbytesFile = getRawbytesFile(id);

		// this directory and contents exists, so just return the DocumentSource
		if (directory.exists()) {
			if (metadataFile.exists() && rawbytesFile.exists()) {
				return new StoredDocumentSource(directory.getName(), metadata);
			}
			// let's keep going in case there was an error last time
		} else {
			directory.mkdir(); // shouldn't need to create parents
		}

		// store metadata
		OutputStream os = null;
		try {
			os = new FileOutputStream(metadataFile);
			metadata.getProperties()
					.storeToXML(
							os,
							"This file was created by Trombone to store properties associated with the bytes stream in the same directory.");
		} finally {
			if (os != null)
				os.close();
		}

		InputStream inputStream = null;
		try {
			inputStream = inputSource.getInputStream();
			FileUtils.copyInputStreamToFile(inputStream, rawbytesFile);
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}

		return new StoredDocumentSource(directory.getName(), metadata);
	}

	public Metadata getStoredDocumentSourceMetadata(String id)
			throws IOException {

		Properties properties = new Properties();
		InputStream is = null;
		try {
			is = new FileInputStream(getMetadataFile(id));
			properties.loadFromXML(is);
		} finally {
			if (is != null)
				is.close();
		}
		return new Metadata(properties);

	}

	public InputStream getStoredDocumentSourceInputStream(String id)
			throws FileNotFoundException {
		File file = getRawbytesFile(id);
		return new FileInputStream(file);
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
			Metadata metadata = getStoredDocumentSourceMetadata(line.trim());
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
	 * @return the multiple expanded stored document source ids File for the
	 *         specified StoredDocumentSource
	 */
	File getMultipleExpandedStoredDocumentSourcesFile(String id, String prefix) {
		return new File(getDocumentSourceDirectory(id),
				prefix+MULTIPLE_EXPANDED_STORED_DOCUMENT_SOURCE_IDS_FILENAME);
	}
}

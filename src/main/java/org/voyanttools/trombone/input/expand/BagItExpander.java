/**
 * 
 */
package org.voyanttools.trombone.input.expand;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.DocumentMetadata.ParentType;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * @author sgsin
 *
 */
class BagItExpander implements Expander {
	
	/**
	 * the stored document storage strategy
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	
	private FlexibleParameters parameters;
	
	private String[] keyFileNames = new String[]{"CWRC.bin","DC.xml","MODS.bin"};


	BagItExpander(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.input.expand.Expander#getExpandedStoredDocumentSources(org.voyanttools.trombone.model.StoredDocumentSource)
	 */
	@Override
	public List<StoredDocumentSource> getExpandedStoredDocumentSources(StoredDocumentSource storedDocumentSource) throws IOException {
		
		File base = new File(System.getProperty("java.io.tmpdir"), "_temp_bagit_"+UUID.randomUUID());
		assert base.mkdir();
		File zipFile = new File(base, "bagit.zip");

		// to avoid getting too close to the inner workings of the storage and still be able to open a file (for extracting convenience), we'll copy the input stream to a file.
		InputStream is = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
		FileUtils.copyInputStreamToFile(is, zipFile);
		is.close();
		
		// extract the zip into a directory and the traverse the directory to find data
		File extractedFile = new File(base, "extracted");
		try {
			new ZipFile(zipFile).extractAll(extractedFile.getPath());
			List<StoredDocumentSource> expandedStoredDocumentSources = new ArrayList<StoredDocumentSource>();
			addFromDirectory(base, extractedFile, storedDocumentSource, expandedStoredDocumentSources);
			return expandedStoredDocumentSources;
		} catch (ZipException e) {
			throw new IOException("Unable to extract BagIt archive.", e);
		} finally {
			FileUtils.deleteDirectory(base);
		}
		
	}

	private void addFromDirectory(File base, File currentDirectory, StoredDocumentSource parentStoredDocumentSource,
			List<StoredDocumentSource> expandedStoredDocumentSources) throws IOException {
		
		// go through current directory to find key file names
		boolean hasKeyFileNames = true;
		for (String filename : keyFileNames) {
			if (new File(currentDirectory, filename).exists()==false) {
				hasKeyFileNames = false;
				break;
			}
		}
		if (hasKeyFileNames) {
				File zipFile = new File(base, currentDirectory.getName() +".zip");
				ArchiveOutputStream output = new ZipArchiveOutputStream(zipFile);
				for (String filename : keyFileNames) {
					File childFile = new File(currentDirectory, filename);
					ZipArchiveEntry entry = new ZipArchiveEntry(childFile, childFile.getName());
					entry.setSize(childFile.length());
					output.putArchiveEntry(entry);
					FileInputStream fis = new FileInputStream(childFile);
					IOUtils.copy(fis, output);
					fis.close();
					output.closeArchiveEntry();
				}
				output.finish();
				output.close();
				InputSource inputSource = new FileInputSource(zipFile);
				DocumentMetadata metadata = inputSource.getMetadata();
				metadata.setParent(parentStoredDocumentSource.getMetadata(), ParentType.EXPANSION);
				metadata.setDocumentFormat(DocumentFormat.BAGIT);
				StoredDocumentSource storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
				expandedStoredDocumentSources.add(storedDocumentSource);

		}
		for (File childFile : currentDirectory.listFiles()) {
			if (childFile.isDirectory()) { // recurse (even for "data" directory)
				addFromDirectory(base, childFile, parentStoredDocumentSource, expandedStoredDocumentSources);
			}
		}
	}
}

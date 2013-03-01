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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.InputStreamInputSource;
import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;

/**
 * An expander for compressed archives. This is supported through the Apache 
 * Commons Compress library and supports common cases for a range of formats
 * like "ar", "cpio", "dump", "jar", "tar", "tgz", "tbz2", "zip" (though not
 * all of these are tested). Note that for single compressed files (like "gz"
 * and "bzip2") the {@link CompressedExpander} should be used.
 * 
 * @author Stéfan Sinclair
 */
class ArchiveExpander implements Expander {

	/**
	 * the primary expander (child documents are expanded with this)
	 */
	private Expander expander;
	
	/**
	 * the stored document storage strategy
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	
	/**
	 * Create a new instance of this expander (this should only be done by
	 * {@link StoredDocumentSourceExpander}.
	 * 
	 * @param storedDocumentSourceStorage a stored storage strategy
	 * @param storedDocumentSoruceExpander a reference to the primary expander
	 */
	ArchiveExpander(StoredDocumentSourceStorage storedDocumentSourceStorage, StoredDocumentSourceExpander storedDocumentSoruceExpander) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.expander = storedDocumentSoruceExpander;
	}
	
	public List<StoredDocumentSource> getExpandedStoredDocumentSources(StoredDocumentSource storedDocumentSource)
			throws IOException {
		
		// first try to see if we've been here already
		String id = storedDocumentSource.getId();
		List<StoredDocumentSource> archivedStoredDocumentSources = storedDocumentSourceStorage.getMultipleExpandedStoredDocumentSources(id);
		if (archivedStoredDocumentSources!=null && archivedStoredDocumentSources.isEmpty()==false) {
			return archivedStoredDocumentSources;
		}
		
		InputStream inputStream = null;
		try {
			ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();
			inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
			BufferedInputStream bis = new BufferedInputStream(inputStream);
			
			String filename = storedDocumentSource.getMetadata().getLocation();
			ArchiveInputStream archiveInputStream;
			
			if (filename.toLowerCase().endsWith("tgz") || filename.toLowerCase().endsWith("tar.gz")) { // decompress and then untar
				archiveInputStream = archiveStreamFactory.createArchiveInputStream(ArchiveStreamFactory.TAR, new GZIPInputStream(bis));
			}
			else if (filename.toLowerCase().endsWith("tbz2") || filename.toLowerCase().endsWith("tar.bz2")) { // decompress and then untar
				archiveInputStream = archiveStreamFactory.createArchiveInputStream(ArchiveStreamFactory.TAR, new BZip2CompressorInputStream(bis));
			}
			else {
				archiveInputStream = archiveStreamFactory.createArchiveInputStream(bis);
			}
			archivedStoredDocumentSources = getExpandedDocumentSources(archiveInputStream, storedDocumentSource);
			storedDocumentSourceStorage.setMultipleExpandedStoredDocumentSources(storedDocumentSource.getId(), archivedStoredDocumentSources);
			return archivedStoredDocumentSources;
		} catch (ArchiveException e) {
			throw new IOException("A problem was encountered reading this archive: "+storedDocumentSource.getMetadata().getLocation(), e);
		}
		finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}

	/**
	 * Get a list of stored document sources from the specified archive stream
	 * (that corresponds to the specfied parent stored document source).
	 * 
	 * @param archiveInputStream the full archive input stream
	 * @param parentStoredDocumentSource the parent stored document source
	 * @return a list of stored document sources in this archive
	 * @throws IOException thrown when an IO exception occurs during unarchiving
	 */
	private List<StoredDocumentSource> getExpandedDocumentSources(
			ArchiveInputStream archiveInputStream, StoredDocumentSource parentStoredDocumentSource) throws IOException {
		
		List<StoredDocumentSource> expandedDocumentSources = new ArrayList<StoredDocumentSource>();
		
		ArchiveEntry archiveEntry = archiveInputStream.getNextEntry();
		String parentId = parentStoredDocumentSource.getId();
		DocumentMetadata parentMetadata = parentStoredDocumentSource.getMetadata();
		while (archiveEntry != null) {
			
			if (archiveEntry.isDirectory()==false) {
				final String filename = archiveEntry.getName();
				final File file = new File(filename);

				// skip directories and skippable files
				if (DocumentFormat.isSkippable(file)==false) {
					DocumentMetadata childMetadata = parentMetadata.asParent();
					childMetadata.setLocation(file.toString());
					childMetadata.setModified(archiveEntry.getLastModifiedDate().getTime());
					childMetadata.setSource(Source.STREAM);
					String id = DigestUtils.md5Hex(parentId+filename);
					InputSource inputSource = new InputStreamInputSource(id, childMetadata, new CloseShieldInputStream(archiveInputStream));
					StoredDocumentSource storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
					expandedDocumentSources.addAll(this.expander.getExpandedStoredDocumentSources(storedDocumentSource)); // expand this recursively
				}
			}
			archiveEntry = archiveInputStream.getNextEntry();
		}

		return expandedDocumentSources;
	}

}

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
package org.voyanttools.trombone.input.expand;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.util.TestHelper;

/**
 * @author "Stéfan Sinclair"
 *
 */
public class ArchiveExpanderTest {

	@Test
	public void testArchives() throws IOException {
		
		File tempDirectory = TestHelper.getTemporaryTestStorageDirectory();
		Storage storage = new FileStorage(tempDirectory);
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		StoredDocumentSourceExpander storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage);
		
		InputSource inputSource;
		StoredDocumentSource storedDocumentSource;
		List<StoredDocumentSource> expandedSourceDocumentSources;
		
		inputSource = new FileInputSource(TestHelper.getResource("archive/archive.zip"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandArchive(storedDocumentSource);
		assertEquals("Zip archive file should contain two content files", 2, expandedSourceDocumentSources.size());

		// use the same file to ensure that we haven't created more stored documents (multiple stored documents should be stored as list)
		// assume that "stored_document_sources" is the right file name
		int fileCount = new File(tempDirectory, "stored_document_sources").list().length;
		inputSource = new FileInputSource(TestHelper.getResource("archive/archive.zip"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandArchive(storedDocumentSource);
		assertEquals("same number of stored documents as before", fileCount, new File(tempDirectory, "stored_document_sources").list().length);

		inputSource = new FileInputSource(TestHelper.getResource("archive/archive.tar"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandArchive(storedDocumentSource);
		assertEquals("Tar archive file should contain two content files", 2, expandedSourceDocumentSources.size());

		inputSource = new FileInputSource(TestHelper.getResource("archive/archive.tar.gz"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandArchive(storedDocumentSource);
		assertEquals("Compressed tar archive file should contain two content files", 2, expandedSourceDocumentSources.size());

		inputSource = new FileInputSource(TestHelper.getResource("archive/archive.tar.bz2"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandArchive(storedDocumentSource);
		assertEquals("Compressed tar archive file should contain two content files", 2, expandedSourceDocumentSources.size());

		FileUtils.deleteDirectory(tempDirectory);
	}

}

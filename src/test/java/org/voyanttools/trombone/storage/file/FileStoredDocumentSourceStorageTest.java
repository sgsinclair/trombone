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
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.TestHelper;

import static org.junit.Assert.*;

/**
 * @author sgs
 *
 */
public class FileStoredDocumentSourceStorageTest {
	
	@Test
	public void test() throws IOException {
		
		File storageDirectory = TestHelper.getTemporaryTestStorageDirectory();
		Storage storage = new FileStorage(storageDirectory);
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		
		final String STRING_TEST = "this is a test";

		// create a first InputSource and get its information
		InputSource inputSource1 = new StringInputSource(STRING_TEST);
		StoredDocumentSource storedDocumentSource1 = storedDocumentSourceStorage.getStoredDocumentSource(inputSource1);
		String id1 = storedDocumentSource1.getId();
		File inputSourceDirectory1 = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getDocumentSourceDirectory(id1);
		long dir_modified = inputSourceDirectory1.lastModified();
		long rawbytes_modified = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getRawbytesFile(id1).lastModified();
		long metadata_modified = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getMetadataFile(id1).lastModified();
		
		// make sure we can retrieve the document
		InputStream inputStream = null;
		String contents;
		try {
			inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(id1);
			contents = IOUtils.toString(inputStream, "UTF-8");
		}
		finally {
			if (inputStream != null) inputStream.close();
		}	
		assertTrue("raw contents should be the same", contents.equals(STRING_TEST));
		DocumentMetadata m1 = inputSource1.getMetadata();
		DocumentMetadata m2 = storedDocumentSourceStorage.getStoredDocumentSourceMetadata(id1);
		assertTrue("metadata from original and retrieved should be the same", inputSource1.getMetadata().equals(storedDocumentSourceStorage.getStoredDocumentSourceMetadata(id1)));
		
		InputSource inputSource2 = new StringInputSource(STRING_TEST);
		StoredDocumentSource storedDocumentSource2 = storedDocumentSourceStorage.getStoredDocumentSource(inputSource2);
		String id2 = storedDocumentSource2.getId();
		assertEquals("old and new IDs should be identical", id1, id2);
		
		// confirm that files haven't changed
		File inputSourceDirectory2 = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getDocumentSourceDirectory(id2);
		assertEquals(inputSourceDirectory1.toString(), inputSourceDirectory2.toString());
		assertEquals("old and new modified dates of directory should be identical", dir_modified, inputSourceDirectory2.lastModified());
		assertEquals("old and new modified dates of rawbytes file should be identical", rawbytes_modified, ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getRawbytesFile(id2).lastModified());
		assertEquals("old and new modified dates of metadata file should be identical", metadata_modified, ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getMetadataFile(id2).lastModified());
		
		storage.destroy();
	}

}

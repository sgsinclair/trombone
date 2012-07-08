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
package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.input.source.UriInputSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.EmbeddedWebServer;
import org.voyanttools.trombone.util.TestHelper;

/**
 * @author sgs
 *
 */
public class FileStoredDocumentSourceStorageTest {
	
	private File storageDirectory;
	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	
	@Before
	public void setup() throws IOException {
		this.storageDirectory = TestHelper.getTemporaryTestStorageDirectory();
		Storage storage = new FileStorage(storageDirectory);
		this.storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
	}
	
	@After
	public void tearDown() throws IOException {
		FileUtils.deleteDirectory(this.storageDirectory);
	}

	@Test
	public void testStringInputSource() throws IOException {
		
		final String STRING_TEST = "this is a test";

		// create a first InputSource and get its information
		InputSource inputSource1 = new StringInputSource(STRING_TEST);
		StoredDocumentSource storedDocumentSource1 = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource1);
		String id1 = storedDocumentSource1.getId();
		File inputSourceDirectory1 = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getDocumentSourceDirectory(id1);
		long dir_modified = inputSourceDirectory1.lastModified();
		long rawbytes_modified = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getRawbytesFile(id1).lastModified();
		long metadata_modified = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getMetadataFile(id1).lastModified();
		
		// make sure we can retrieve the document
		String contents = FileUtils.readFileToString(((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getRawbytesFile(id1), "UTF-8");
		Assert.assertTrue("raw contents should be the same", contents.equals(STRING_TEST));
		Assert.assertTrue("metadata from original and retrieved should be the same", inputSource1.getMetadata().equals(storedDocumentSourceStorage.getStoredDocumentSourceMetadata(id1)));
		
		InputSource inputSource2 = new StringInputSource(STRING_TEST);
		StoredDocumentSource storedDocumentSource2 = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource2);
		String id2 = storedDocumentSource2.getId();
		Assert.assertEquals("old and new IDs should be identical", id1, id2);
		
		// confirm that files haven't changed
		File inputSourceDirectory2 = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getDocumentSourceDirectory(id2);
		Assert.assertEquals(inputSourceDirectory1.toString(), inputSourceDirectory2.toString());
		Assert.assertEquals("old and new modified dates of directory should be identical", dir_modified, inputSourceDirectory2.lastModified());
		Assert.assertEquals("old and new modified dates of rawbytes file should be identical", rawbytes_modified, ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getRawbytesFile(id2).lastModified());
		Assert.assertEquals("old and new modified dates of metadata file should be identical", metadata_modified, ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getMetadataFile(id2).lastModified());
		
	}

	@Test
	public void testFileInputSource() throws IOException {
		
		final File file = TestHelper.getResource("xml/rss.xml");

		// create a first InputSource and get its information
		InputSource inputSource1 = new FileInputSource(file);
		StoredDocumentSource storedDocumentSource1 = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource1);
		String id1 = storedDocumentSource1.getId();
		File inputSourceDirectory1 = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getDocumentSourceDirectory(id1);
		long dir_modified = inputSourceDirectory1.lastModified();
		long rawbytes_modified = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getRawbytesFile(id1).lastModified();
		long metadata_modified = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getMetadataFile(id1).lastModified();
		
		// make sure we can retrieve the document
		Assert.assertTrue("raw contents should be the same", FileUtils.contentEquals(file, ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getRawbytesFile(id1)));
		Assert.assertTrue("metadata from original and retrieved should be the same", inputSource1.getMetadata().equals(storedDocumentSourceStorage.getStoredDocumentSourceMetadata(id1)));
		
		InputSource inputSource2 =  new FileInputSource(file);
		StoredDocumentSource storedDocumentSource2 = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource2);
		String id2 = storedDocumentSource2.getId();
		Assert.assertEquals("old and new IDs should be identical", id1, id2);
		
		// confirm that files haven't changed
		File inputSourceDirectory2 = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getDocumentSourceDirectory(id2);
		Assert.assertEquals(inputSourceDirectory1.toString(), inputSourceDirectory2.toString());
		Assert.assertEquals("old and new modified dates of directory should be identical", dir_modified, inputSourceDirectory2.lastModified());
		Assert.assertEquals("old and new modified dates of rawbytes file should be identical", rawbytes_modified, ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getRawbytesFile(id2).lastModified());
		Assert.assertEquals("old and new modified dates of metadata file should be identical", metadata_modified, ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getMetadataFile(id2).lastModified());
		
	}
	
	@Test
	public void testUriInputSource() throws Exception {
		
		EmbeddedWebServer webServer = new EmbeddedWebServer();
		webServer.start();
		
		try {

			final String relativePath = "xml/rss.xml";
			final URI uri = new URI("http://localhost:"+webServer.getPort()+"/" + relativePath);
	
			// create a first InputSource and get its information
			InputSource inputSource1 = new UriInputSource(uri);
			StoredDocumentSource storedDocumentSource1 = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource1);
			String id1 = storedDocumentSource1.getId();
			File inputSourceDirectory1 = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getDocumentSourceDirectory(id1);
			long dir_modified = inputSourceDirectory1.lastModified();
			long rawbytes_modified = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getRawbytesFile(id1).lastModified();
			long metadata_modified = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getMetadataFile(id1).lastModified();
			
			// make sure we can retrieve the document
			File localFile = TestHelper.getResource("xml/rss.xml"); // read it locally, not through server
			File storedFile = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getRawbytesFile(id1);
			Assert.assertTrue("raw contents should be the same", FileUtils.contentEquals(localFile, storedFile));
			Assert.assertTrue("metadata from original and retrieved should be the same", inputSource1.getMetadata().equals(storedDocumentSourceStorage.getStoredDocumentSourceMetadata(id1)));
			
			InputSource inputSource2 = new UriInputSource(uri);
			StoredDocumentSource storedDocumentSource2 = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource2);
			String id2 = storedDocumentSource2.getId();
			Assert.assertEquals("old and new IDs should be identical", id1, id2);
			
			// confirm that files haven't changed
			File inputSourceDirectory2 = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getDocumentSourceDirectory(id2);
			Assert.assertEquals(inputSourceDirectory1.toString(), inputSourceDirectory2.toString());
			Assert.assertEquals("old and new modified dates of directory should be identical", dir_modified, inputSourceDirectory2.lastModified());
			Assert.assertEquals("old and new modified dates of rawbytes file should be identical", rawbytes_modified, ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getRawbytesFile(id2).lastModified());
			Assert.assertEquals("old and new modified dates of metadata file should be identical", metadata_modified, ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getMetadataFile(id2).lastModified());
		}
		finally {
			webServer.stop();
		}
	}
}

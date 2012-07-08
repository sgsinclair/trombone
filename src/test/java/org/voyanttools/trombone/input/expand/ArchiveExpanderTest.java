/**
 * 
 */
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

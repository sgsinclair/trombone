package org.voyanttools.trombone.input.expand;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.voyanttools.trombone.document.DocumentFormat;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.util.TestHelper;

public class CompressedExpanderTest {

	@Test
	public void testCompressed() throws IOException {

		File tempDirectory = TestHelper.getTemporaryTestStorageDirectory();
		Storage storage = new FileStorage(tempDirectory);
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		StoredDocumentSourceExpander storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage);
		
		InputSource inputSource;
		StoredDocumentSource storedDocumentSource;
		List<StoredDocumentSource> expandedSourceDocumentSources;

		inputSource = new FileInputSource(TestHelper.getResource("compressed/chars_latin1.txt.gz"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandCompressed(storedDocumentSource);
		assertEquals("Compressed tar archive file should contain two content files", 1, expandedSourceDocumentSources.size());

		inputSource = new FileInputSource(TestHelper.getResource("compressed/chars_latin1.txt.bz2"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandCompressed(storedDocumentSource);
		assertEquals("Compressed tar archive file should contain two content files", 1, expandedSourceDocumentSources.size());

		FileUtils.deleteDirectory(tempDirectory);
	}

}

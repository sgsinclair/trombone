package org.voyanttools.trombone.input.extract;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.voyanttools.trombone.input.expand.StoredDocumentSourceExpander;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class BagItExtractorTest {

	@Test
	public void test() throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();
		StoredDocumentSourceStorage storeDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		FlexibleParameters parameters = new FlexibleParameters();
		StoredDocumentSourceExtractor extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, parameters);
		
		InputSource inputSource;
		StoredDocumentSource storedDocumentSource;
		StoredDocumentSource extractedStoredDocumentSource;
		DocumentMetadata metadata;
		
		
		inputSource = new FileInputSource(TestHelper.getResource("formats/BagIt-One-Document.zip"));
		inputSource.getMetadata().setDocumentFormat(DocumentFormat.BAGIT); // will normally be set by expander
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("A Beautiful Possibility", metadata.getTitle());
		assertEquals("Edith Ferguson Black", metadata.getAuthor());
		InputStream is = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		String contents = IOUtils.toString(is);
		assertTrue(contents.contains("In one of the fairest"));
		is.close();
		
		StoredDocumentSourceExpander storedDocumentSourceExpander = new StoredDocumentSourceExpander(storeDocumentSourceStorage);
		inputSource = new FileInputSource(TestHelper.getResource("formats/BagIt-Multiple-Documents.zip"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		List<StoredDocumentSource> expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		assertEquals(2, expandedSourceDocumentSources.size());
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(expandedSourceDocumentSources.get(0));
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("Further Chronicles of Avonlea", metadata.getTitle());
		assertEquals("L. M. (Lucy Maud) Montgomery", metadata.getAuthor());
		is = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(is);
		assertTrue(contents.contains("Max always blesses the animal"));
		assertFalse(contents.contains("GutenTag"));
		is.close();
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(expandedSourceDocumentSources.get(1));
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("Anne of the Island", metadata.getTitle());
		assertEquals("L. M. (Lucy Maud) Montgomery", metadata.getAuthor());
		is = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(is);
		assertTrue(contents.contains("Harvest is ended and summer"));
		assertFalse(contents.contains("GutenTag"));
		is.close();
		
		
		// test more recent BagIt
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storeDocumentSourceStorage);
		inputSource = new FileInputSource(TestHelper.getResource("formats/bagit_cwrc_lmm_texts-04f2ac7.zip"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		assertEquals(16, expandedSourceDocumentSources.size()); // note that there are 17 directories, but one doesn't have docs
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(expandedSourceDocumentSources.get(0));
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("Further Chronicles of Avonlea", metadata.getTitle());
		assertEquals("L. M. (Lucy Maud) Montgomery", metadata.getAuthor());
		is = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(is);
		assertTrue(contents.contains("Max always blesses the animal"));
		assertFalse(contents.contains("GutenTag"));
		is.close();
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(expandedSourceDocumentSources.get(1));
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("Rainbow Valley", metadata.getTitle());
		assertEquals("L. M. (Lucy Maud) Montgomery", metadata.getAuthor());
		is = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(is);
		assertTrue(contents.contains("apple-green evening in May"));
		assertFalse(contents.contains("GutenTag"));
		is.close();
		storage.destroy();
	}

}

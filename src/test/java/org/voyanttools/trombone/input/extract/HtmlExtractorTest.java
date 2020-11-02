package org.voyanttools.trombone.input.extract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class HtmlExtractorTest {

	@Test
	public void test() throws IOException {
		
		Storage storage = TestHelper.getDefaultTestStorage();
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		
		StoredDocumentSourceExtractor storedDocumentSourceExtractor;
		StoredDocumentSource extractedStoredDocumentSource;
		String contents;
		
		FlexibleParameters parameters = new FlexibleParameters();

		InputSource longerInputSource = new FileInputSource(TestHelper.getResource("html/longer.html"));
		StoredDocumentSource longerStoredDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(longerInputSource);
		storedDocumentSourceExtractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, parameters);
		extractedStoredDocumentSource = storedDocumentSourceExtractor.getExtractedStoredDocumentSource(longerStoredDocumentSource);
		assertEquals("Authors", extractedStoredDocumentSource.getMetadata().getAuthor());
		assertEquals("en", extractedStoredDocumentSource.getMetadata().getLanguageCode());

		// default handling of most things but using HTML Extractor
		parameters.setParameter("htmlKeywordQuery", "keywords");
		storedDocumentSourceExtractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, parameters);
		extractedStoredDocumentSource = storedDocumentSourceExtractor.getExtractedStoredDocumentSource(longerStoredDocumentSource);
		assertEquals("Authors", extractedStoredDocumentSource.getMetadata().getAuthor());
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()), "UTF-8");
		assertEquals("en", extractedStoredDocumentSource.getMetadata().getLanguageCode());
		assertTrue(contents.contains("Content"));
		
		// default handling of most things but using HTML Extractor
		parameters.setParameter("htmlContentQuery", "article article");
		storedDocumentSourceExtractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, parameters);
		extractedStoredDocumentSource = storedDocumentSourceExtractor.getExtractedStoredDocumentSource(longerStoredDocumentSource);
		assertEquals("Authors", extractedStoredDocumentSource.getMetadata().getAuthor());
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()), "UTF-8");
		assertEquals("en", extractedStoredDocumentSource.getMetadata().getLanguageCode());
		assertFalse(contents.contains("Content"));
		
		storage.destroy();
	}

}

package org.voyanttools.trombone.input.extract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
		List<StoredDocumentSource> extractedSourceDocumentSources;
		StoredDocumentSource extractedStoredDocumentSource;
		String contents;
		
		FlexibleParameters parameters = new FlexibleParameters();

		InputSource longerInputSource = new FileInputSource(TestHelper.getResource("html/longer.html"));
		StoredDocumentSource longerStoredDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(longerInputSource);
		storedDocumentSourceExtractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, parameters);
		extractedStoredDocumentSource = storedDocumentSourceExtractor.getExtractedStoredDocumentSource(longerStoredDocumentSource);
		assertEquals("Authors", extractedStoredDocumentSource.getMetadata().getAuthor());

		// default handling of most things but using HTML Extractor
		parameters.setParameter("htmlKeywordQuery", "keywords");
		storedDocumentSourceExtractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, parameters);
		extractedStoredDocumentSource = storedDocumentSourceExtractor.getExtractedStoredDocumentSource(longerStoredDocumentSource);
		assertEquals("Authors", extractedStoredDocumentSource.getMetadata().getAuthor());
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue(contents.contains("Content"));
		
		// default handling of most things but using HTML Extractor
		parameters.setParameter("htmlContentQuery", "article article");
		storedDocumentSourceExtractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, parameters);
		extractedStoredDocumentSource = storedDocumentSourceExtractor.getExtractedStoredDocumentSource(longerStoredDocumentSource);
		assertEquals("Authors", extractedStoredDocumentSource.getMetadata().getAuthor());
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertFalse(contents.contains("Content"));
		
		/*
		assertEquals
		for (StoredDocumentSource source : sources) {
			extractedSourceDocumentSources = storedDocumentSourceExtractor.getExtractedStoredDocumentSources(source);
			assertEquals("HTML without expansion parameters should have one doc", 1, extractedSourceDocumentSources.size());
			extractedStoredDocumentSource = extractedSourceDocumentSources.get(0);
			assertEquals("HTML without expansion parameters should be original source", extractedStoredDocumentSource.getId(), source.getId());
			contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
			assertTrue(contents.contains("résumé"));
		}
		
		parameters.setParameter("htmlDocumentsQuery", "p");
		storedDocumentSourceExtractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, parameters);
		for (StoredDocumentSource source : sources) {
			extractedSourceDocumentSources = storedDocumentSourceExtractor.getExtractedStoredDocumentSource(source);
			assertEquals("HTML p selector should have two docs",2, extractedSourceDocumentSources.size());
			extractedStoredDocumentSource = extractedSourceDocumentSources.get(0);
			contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
			assertTrue(contents.contains("résumé"));
		}

		// group by using valid value
		parameters.setParameter("htmlDocumentsQuery", "article article");
		parameters.setParameter("htmlGroupByQuery", "header p[class=author]");
		storedDocumentSourceExtractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, parameters);
		StoredDocumentSource storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(new FileInputSource(TestHelper.getResource("html/longer.html")));
		extractedSourceDocumentSources = storedDocumentSourceExtractor.getExtractedStoredDocumentSource(storedDocumentSource);
		assertEquals("2 docs (grouped by author)", 2, extractedSourceDocumentSources.size());
		extractedStoredDocumentSource = extractedSourceDocumentSources.get(0);
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertEquals("2 occurrences of author 1", 2, StringUtils.countMatches(contents, "Author 1"));
		
		// group by using valid value as @attr
		parameters.setParameter("htmlDocumentsQuery", "article article");
		parameters.setParameter("htmlGroupByQuery", "header p[class=author] @author");
		storedDocumentSourceExtractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, parameters);
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(new FileInputSource(TestHelper.getResource("html/longer.html")));
		extractedSourceDocumentSources = storedDocumentSourceExtractor.getExtractedStoredDocumentSource(storedDocumentSource);
		assertEquals("2 docs (grouped by author)", 2, extractedSourceDocumentSources.size());
		extractedStoredDocumentSource = extractedSourceDocumentSources.get(0);
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertEquals("2 occurrences of author 1", 2, StringUtils.countMatches(contents, "Author 1"));
		
		// group by using invalid value
		parameters.setParameter("htmlDocumentsQuery", "article article");
		parameters.setParameter("htmlGroupByQuery", "header p[class=authorship]");
		storedDocumentSourceExtractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, parameters);
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(new FileInputSource(TestHelper.getResource("html/longer.html")));
		extractedSourceDocumentSources = storedDocumentSourceExtractor.getExtractedStoredDocumentSource(storedDocumentSource);
		assertEquals("3 docs (grouping by author failed)", 3, extractedSourceDocumentSources.size());
		*/
		storage.destroy();
	}

}

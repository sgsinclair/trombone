package org.voyanttools.trombone.input.extract;

import static org.junit.Assert.*;

import java.io.IOException;
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

public class JsonExtractorTest {

	@Test
	public void test() throws IOException {

		Storage storage = TestHelper.getDefaultTestStorage();
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		
		StoredDocumentSourceExtractor storedDocumentSourceExtractor;
		StoredDocumentSource extractedStoredDocumentSource;
		DocumentMetadata metadata;
		String contents;
		
		FlexibleParameters parameters = new FlexibleParameters();

		// no parameters
		InputSource inputSource = new FileInputSource(TestHelper.getResource("json/feed.json"));
		StoredDocumentSource storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		storedDocumentSourceExtractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, parameters);
		extractedStoredDocumentSource = storedDocumentSourceExtractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals(DocumentFormat.JSON, metadata.getDocumentFormat());
		assertEquals("{\"rss\":{\"channel\":{\"", metadata.getTitle());
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue(contents.startsWith("{\"rss\":{\"channel\":{\"title\":\"Website Feed\""));

		// most parameters
		parameters.setParameter("jsonContentPointer", "/rss/channel/description");
		parameters.setParameter("jsonTitlePointer", "/rss/channel/title");
		parameters.setParameter("jsonAuthorPointer", "/rss/channel/dc:creator");
		parameters.setParameter("jsonPubPlacePointer", "/rss/channel/link");
		parameters.setParameter("jsonPublisherPointer", "/rss/channel/link");
		parameters.setParameter("jsonPubDatePointer", "/rss/channel/link");
		parameters.setParameter("jsonKeywordPointer", "/rss/channel/link");
		parameters.setParameter("jsonCollectionPointer", "/rss/channel/link");
		storedDocumentSourceExtractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, parameters);
		extractedStoredDocumentSource = storedDocumentSourceExtractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals(DocumentFormat.JSON, metadata.getDocumentFormat());
		assertEquals("Website Feed", metadata.getTitle());
		assertEquals("Me (me@example.com)", metadata.getAuthor());
		assertEquals("http://www.yourdomain.com", metadata.getPubDate());
		assertEquals("http://www.yourdomain.com", metadata.getKeywords());
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue(contents.startsWith("Website Feed coded manually"));
		
		// with expansion
		parameters = new FlexibleParameters();
		parameters.setParameter("jsonDocumentsPointer", "/rss/channel/items");
		parameters.setParameter("jsonContentPointer", "/description");
		parameters.setParameter("jsonTitlePointer", "/title");
		parameters.setParameter("jsonAuthorPointer", "/author");
		parameters.setParameter("jsonPubPlacePointer", "/link");
		parameters.setParameter("jsonPublisherPointer", "/link");
		parameters.setParameter("jsonPubDatePointer", "/link");
		parameters.setParameter("jsonKeywordPointer", "/link");
		parameters.setParameter("jsonCollectionPointer", "/link");
		StoredDocumentSourceExpander storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		List<StoredDocumentSource> expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		storedDocumentSourceExtractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, parameters);
		List<StoredDocumentSource> extractedStoredDocumentSources = storedDocumentSourceExtractor.getExtractedStoredDocumentSources(expandedSourceDocumentSources);
		metadata = extractedStoredDocumentSources.get(0).getMetadata();
		assertEquals(DocumentFormat.JSON, metadata.getDocumentFormat());
		assertEquals("A Special Event", metadata.getTitle());
		assertEquals("Joe Blow", metadata.getAuthor());
		assertEquals("http://www.yourdomain.com/events.htm", metadata.getPubDate());
		assertEquals("http://www.yourdomain.com/events.htm", metadata.getKeywords());
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSources.get(0).getId()));
		assertTrue(contents.startsWith("A Special Teleconference for our customers about our products"));
		metadata = extractedStoredDocumentSources.get(1).getMetadata();
		assertEquals(DocumentFormat.JSON, metadata.getDocumentFormat());
		assertEquals("Announcing new Products", metadata.getTitle());
		assertEquals("Joe Blow", metadata.getAuthor());
		assertEquals("http://www.yourdomain.com/events.htm", metadata.getPubDate());
		assertEquals("http://www.yourdomain.com/events.htm", metadata.getKeywords());
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSources.get(1).getId()));
		assertTrue(contents.startsWith("Announcing a new line of products"));
		

		// with group by expansion
		parameters = new FlexibleParameters();
		parameters.setParameter("jsonDocumentsPointer", "/rss/channel/items");
		parameters.setParameter("jsonGroupByPointer", "/author");
		parameters.setParameter("jsonContentPointer", "/description");
		parameters.setParameter("jsonTitlePointer", "/title");
		parameters.setParameter("jsonAuthorPointer", "/author");
		parameters.setParameter("jsonPubPlacePointer", "/link");
		parameters.setParameter("jsonPublisherPointer", "/link");
		parameters.setParameter("jsonPubDatePointer", "/link");
		parameters.setParameter("jsonKeywordPointer", "/link");
		parameters.setParameter("jsonCollectionPointer", "/link");
		inputSource = new FileInputSource(TestHelper.getResource("json/longerfeed.json"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		storedDocumentSourceExtractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, parameters);
		extractedStoredDocumentSources = storedDocumentSourceExtractor.getExtractedStoredDocumentSources(expandedSourceDocumentSources);
		assertEquals(2, extractedStoredDocumentSources.size());
		metadata = extractedStoredDocumentSources.get(0).getMetadata();
		assertEquals(DocumentFormat.JSON, metadata.getDocumentFormat());
		assertEquals("A Special Event", metadata.getTitle());
		assertEquals("Joe Blow", metadata.getAuthor());
		assertEquals("http://www.yourdomain.com/events.htm", metadata.getPubDate());
		assertEquals("http://www.yourdomain.com/events.htm", metadata.getKeywords());
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSources.get(0).getId()));
		assertTrue(contents.startsWith("A Special Teleconference for our customers about our products"));
		metadata = extractedStoredDocumentSources.get(1).getMetadata();
		assertEquals(DocumentFormat.JSON, metadata.getDocumentFormat());
		assertEquals("Announcing new Products", metadata.getTitle());
		assertEquals("Joe Blow", metadata.getAuthor());
		assertEquals("http://www.yourdomain.com/events.htm", metadata.getPubDate());
		assertEquals("http://www.yourdomain.com/events.htm", metadata.getKeywords());
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSources.get(1).getId()));
		assertTrue(contents.startsWith("Announcing a new line of products"));

	}

}

package org.voyanttools.trombone.input.expand;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.json.JsonException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

import junit.framework.Assert;

public class JsonExpanderTest {

	@Test
	public void testJson() throws IOException {
		
		Storage storage = TestHelper.getDefaultTestStorage();
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		
		StoredDocumentSourceExpander storedDocumentSourceExpander;
		InputSource inputSource;
		StoredDocumentSource storedDocumentSource;
		List<StoredDocumentSource> expandedSourceDocumentSources;
		FlexibleParameters parameters;
		InputStream inputStream;
		FileInputStream fileInputStream;

		
		// test auto-detect of .json file
		parameters = new FlexibleParameters();
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("json/feed.json"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		assertEquals(1, expandedSourceDocumentSources.size());
		assertEquals(DocumentFormat.JSON, expandedSourceDocumentSources.get(0).getMetadata().getDocumentFormat());
		inputStream = null;
		fileInputStream = null;
		try {
			inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedSourceDocumentSources.get(0).getId());
			fileInputStream = new FileInputStream(TestHelper.getResource("json/feed.json"));
			assertTrue(IOUtils.contentEquals(fileInputStream, inputStream));
		}
		finally {
			if (inputStream!=null) {inputStream.close();}
			if (fileInputStream!=null) {fileInputStream.close();}
		}

		// try with one document
		parameters = new FlexibleParameters();
		parameters.setParameter("inputFormat", "json");
		parameters.setParameter("jsonDocumentsPointer", "/rss/channel");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("json/feed.json"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		assertEquals(1, expandedSourceDocumentSources.size());
		assertEquals(DocumentFormat.JSON, expandedSourceDocumentSources.get(0).getMetadata().getDocumentFormat());
		inputStream = null;
		try {
			inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedSourceDocumentSources.get(0).getId());
			assertTrue(IOUtils.toString(inputStream, "UTF-8").startsWith("{\"title\":\"Website Feed"));
		}
		finally {
			if (inputStream!=null) {inputStream.close();}
		}
		
		// try with multiple documents
		parameters = new FlexibleParameters();
		parameters.setParameter("jsonDocumentsPointer", "/rss/channel/items");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("json/feed.json"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		assertEquals(2, expandedSourceDocumentSources.size());
		assertEquals(DocumentFormat.JSON, expandedSourceDocumentSources.get(0).getMetadata().getDocumentFormat());
		inputStream = null;
		try {
			inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedSourceDocumentSources.get(0).getId());
			assertTrue(IOUtils.toString(inputStream, "UTF-8").startsWith("{\"title\":\"A Special Event"));
		}
		finally {
			if (inputStream!=null) {inputStream.close();}
		}

		// try with bad query
		parameters = new FlexibleParameters();
		parameters.setParameter("jsonDocumentsPointer", "/nope");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("json/feed.json"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		try {
			expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
			fail("An exception should have been raised.");
		} catch (IOException e) {
		}
	}

	@Test
	public void testJsonLines() throws IOException {
		
		Storage storage = TestHelper.getDefaultTestStorage();
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		
		StoredDocumentSourceExpander storedDocumentSourceExpander;
		InputSource inputSource;
		StoredDocumentSource storedDocumentSource;
		List<StoredDocumentSource> expandedSourceDocumentSources;
		FlexibleParameters parameters;
		InputStream inputStream;
		
		// test auto-detect of .jsonl file
		parameters = new FlexibleParameters();
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("json/feed.jsonl"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		assertEquals(2, expandedSourceDocumentSources.size());
		assertEquals(DocumentFormat.JSON, expandedSourceDocumentSources.get(0).getMetadata().getDocumentFormat());
		inputStream = null;
		try {
			inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedSourceDocumentSources.get(1).getId());
			assertTrue(IOUtils.toString(inputStream, "UTF-8").startsWith("{ \"title\": \"Announcing new Products"));
		}
		finally {
			if (inputStream!=null) {inputStream.close();}
		}
		
		parameters = new FlexibleParameters();
		parameters.setParameter("inputFormat", "JSONLINES");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("json/feed.jsonl"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		assertEquals(2, expandedSourceDocumentSources.size());
		assertEquals(DocumentFormat.JSON, expandedSourceDocumentSources.get(0).getMetadata().getDocumentFormat());
		inputStream = null;
		try {
			inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedSourceDocumentSources.get(1).getId());
			assertTrue(IOUtils.toString(inputStream, "UTF-8").startsWith("{ \"title\": \"Announcing new Products"));
		}
		finally {
			if (inputStream!=null) {inputStream.close();}
		}
		
		storage.destroy();

	}
}

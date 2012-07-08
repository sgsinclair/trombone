/**
 * 
 */
package org.voyanttools.trombone.input.expand;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

/**
 * @author "Stéfan Sinclair"
 *
 */
public class XmlExpanderTest {

	@Test
	public void test() throws IOException {

		File tempDirectory = TestHelper.getTemporaryTestStorageDirectory();
		Storage storage = new FileStorage(tempDirectory);
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		
		StoredDocumentSourceExpander storedDocumentSourceExpander;
		InputSource inputSource;
		StoredDocumentSource storedDocumentSource;
		List<StoredDocumentSource> expandedSourceDocumentSources;
		FlexibleParameters parameters;
		InputStream inputStream;
		FileInputStream fileInputStream;
		
		// make sure we have one document when no xmlDocumentXpath is specified
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage);
		inputSource = new FileInputSource(TestHelper.getResource("xml/rss.xml"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandXml(storedDocumentSource);
		assertEquals("XML file with no Xpath should contain one document", 1, expandedSourceDocumentSources.size());


		// with xmlDocumentXpath we should have two for //item
		parameters = new FlexibleParameters(new String[]{"xmlDocumentsXpath=//item"});
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("xml/rss.xml"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandXml(storedDocumentSource);
		assertEquals("XML file with no Xpath should contain one document", 2, expandedSourceDocumentSources.size());
		inputStream = null;
		fileInputStream = null;
		try {
			inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedSourceDocumentSources.get(1).getId());
			fileInputStream = new FileInputStream(TestHelper.getResource("xml/rss.xml.item2_xpath.xml"));
			assertTrue(IOUtils.contentEquals(fileInputStream, inputStream));
		}
		finally {
			if (inputStream!=null) {inputStream.close();}
			if (fileInputStream!=null) {fileInputStream.close();}
		}
		
		// with xmlDocumentXpath we should have one for dc:creator
		parameters = new FlexibleParameters(new String[]{"xmlDocumentsXpath=//dc:creator"});
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("xml/rss.xml"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandXml(storedDocumentSource);
		assertEquals("XML file with no Xpath should contain one document", 1, expandedSourceDocumentSources.size());
		inputStream = null;
		fileInputStream = null;
		try {
			inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedSourceDocumentSources.get(0).getId());
			fileInputStream = new FileInputStream(TestHelper.getResource("xml/rss.xml.dc_creator_xpath.xml"));
			assertTrue(IOUtils.contentEquals(fileInputStream, inputStream));
		}
		finally {
			if (inputStream!=null) {inputStream.close();}
			if (fileInputStream!=null) {fileInputStream.close();}
		}

		// with xmlDocumentXpath we should have one for dc:creator
		parameters = new FlexibleParameters(new String[]{"xmlDocumentsXpath=//item/title", "xmlDocumentsXpath=//item/description"});
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("xml/rss.xml"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSourceId(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandXml(storedDocumentSource);
		assertEquals("XML file with no Xpath should contain one document", 2, expandedSourceDocumentSources.size());
		inputStream = null;
		fileInputStream = null;
		try {
			inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedSourceDocumentSources.get(0).getId());
			fileInputStream = new FileInputStream(TestHelper.getResource("xml/rss.xml.title_multixpath.xml"));
			assertTrue(IOUtils.contentEquals(fileInputStream, inputStream));
		}
		finally {
			if (inputStream!=null) {inputStream.close();}
			if (fileInputStream!=null) {fileInputStream.close();}
		}
		
		FileUtils.deleteDirectory(tempDirectory);

	}

}

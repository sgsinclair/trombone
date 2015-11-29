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
package org.voyanttools.trombone.input.expand;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

/**
 * @author "Stéfan Sinclair"
 *
 */
public class XmlExpanderTest {

	@Test
	public void test() throws IOException {

		Storage storage = TestHelper.getDefaultTestStorage();
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
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandXml(storedDocumentSource);
		assertEquals("XML file with no Xpath should contain one document", 1, expandedSourceDocumentSources.size());


		// with xmlDocumentXpath we should have two for //item
		parameters = new FlexibleParameters(new String[]{"xmlDocumentsXpath=//item"});
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("xml/rss.xml"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
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
		
		// with no xmlDocumentXpath but splitDocuments we should have two for //item
		parameters = new FlexibleParameters(new String[]{"inputFormat=RSS","splitDocuments=true"});
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("xml/rss.xml"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
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
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
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

		// with xmlDocumentXpath we should have one for local-name()='creator'
		parameters = new FlexibleParameters(new String[]{"xmlDocumentsXpath=//*[local-name()='creator']"});
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("xml/rss.xml"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandXml(storedDocumentSource);
		assertEquals("XML file with local name creator should contain 1 document", 1, expandedSourceDocumentSources.size());
		
		// with xmlDocumentXpath we should have none for creator (without namespace
		parameters = new FlexibleParameters(new String[]{"xmlDocumentsXpath=//creator"});
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("xml/rss.xml"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandXml(storedDocumentSource);
		assertEquals("XML file with creator (no namespace) should contain no documents", 0, expandedSourceDocumentSources.size());
		
		// RSS documents within a zip archive (nested expansion)
		parameters = new FlexibleParameters(new String[]{"inputFormat=RSS","splitDocuments=true"});
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("archive/rss.xml.zip"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
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
		
		// make sure a string is recognized as XML
		parameters = new FlexibleParameters();
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new StringInputSource("<a><b>c</b><b>d</b></a>");
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		assertEquals("XML string with no Xpath should contain one document", 1, expandedSourceDocumentSources.size());
		assertEquals("input string should be recognized as XML", DocumentFormat.XML, expandedSourceDocumentSources.get(0).getMetadata().getDocumentFormat());
		
		// make sure an XML string is expanded
		parameters = new FlexibleParameters(new String[]{"xmlDocumentsXpath=//b"});
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new StringInputSource("<a><b>c</b><b>d</b></a>");
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		assertEquals("XML string with no documents xpath should contain two documents", 2, expandedSourceDocumentSources.size());
		assertEquals("input string should be recognized as XML", DocumentFormat.XML, expandedSourceDocumentSources.get(0).getMetadata().getDocumentFormat());
		
		// make sure namespaces works properly
		parameters = new FlexibleParameters(new String[]{"xmlDocumentsXpath=//*[local-name()='table']"});
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("xml/namespaces.xml"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		assertEquals("XML namespaces example should have three documents", 3, expandedSourceDocumentSources.size());
		
		// test groupby xpath
		parameters = new FlexibleParameters(new String[]{"xmlDocumentsXpath=//*[local-name()='table']","xmlGroupByXpath=//*[local-name()='length']"});
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("xml/namespaces.xml"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		assertEquals("XML namespaces example should have three documents", 2, expandedSourceDocumentSources.size());
		
		parameters = new FlexibleParameters(new String[]{"xmlDocumentsXpath=//*[local-name()='table']","xmlGroupByXpath=//*[local-name()='width']"});
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		inputSource = new FileInputSource(TestHelper.getResource("xml/namespaces.xml"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		assertEquals("XML namespaces example should have three documents", 1, expandedSourceDocumentSources.size());
		
		storage.destroy();

	}

}

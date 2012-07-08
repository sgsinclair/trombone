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
		
		storage.destroy();

	}

}

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
package org.voyanttools.trombone.input.extract;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.voyanttools.trombone.document.Metadata;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

/**
 * @author sgs
 *
 */
public class XmlExtractorTest {

	@Test
	public void test() throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();
		StoredDocumentSourceStorage storeDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		FlexibleParameters parameters = new FlexibleParameters();
		StoredDocumentSourceExtractor extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, parameters);
		
		InputSource inputSource;
		StoredDocumentSource storedDocumentSource;
		StoredDocumentSource extractedStoredDocumentSource;
		Metadata metadata;
		String contents;
		
		String line = FileUtils.readLines(TestHelper.getResource("formats/chars_utf8.txt")).get(0).trim();
		line = line.substring(line.indexOf("I"));
		
		/*
		inputSource = new FileInputSource(TestHelper.getResource("formats/chars_utf8.xml"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		// this should be blank rather than the title tag (for generic XML)
		assertEquals("title for XML document", "", metadata.getTitle());
		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("ensure we have some content in XML", contents.contains(line)==true);

		// try with xmlContentXpath parameter and multiple nodes
		extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, new FlexibleParameters(new String[]{"xmlContentXpath=//p"}));
		inputSource = new FileInputSource(TestHelper.getResource("formats/chars_utf8.xml"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		// this should be blank rather than the title tag (for generic XML)
		assertEquals("title for XML document", "", metadata.getTitle());
		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("ensure we have stripped out other content", contents.contains("<body>")==false);
		assertTrue("ensure we have some content in XML with multiple nodes for the xmlContentXPath parameter", contents.contains(line)==true);
		
		// try with xmlContentXpath parameter and single node
		extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, new FlexibleParameters(new String[]{"xmlContentXpath=//body"}));
		inputSource = new FileInputSource(TestHelper.getResource("formats/chars_utf8.xml"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		// this should be blank rather than the title tag (for generic XML)
		assertEquals("title for XML document", "", metadata.getTitle());
		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("ensure we have stripped out other content", contents.contains("<head>")==false);
		assertTrue("ensure we have some content in XML with a single node xmlContentXpath parameter", contents.contains(line)==true);
		*/
		// try with RSS input format
		extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, new FlexibleParameters(new String[]{"inputFormat=RSS"}));
		inputSource = new FileInputSource(TestHelper.getResource("xml/rss.xml"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		// this should be blank rather than the title tag (for generic XML)
		assertEquals("title for RSS feed", "Website Feed", metadata.getTitle());
		assertEquals("author for RSS feed", "Me (me@example.com)", metadata.getAuthor());
		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("ensure we have stripped out other content in RSS feed", contents.contains("<link>")==false);
		assertTrue("ensure we have three lines of description in RSS feed", StringUtils.countMatches(contents, "<description>")==2);

	}

}

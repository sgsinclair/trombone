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
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
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
		DocumentMetadata metadata;
		InputStream inputStream;
		String contents;
		
		String line = FileUtils.readLines(TestHelper.getResource("formats/chars_utf8.txt")).get(0).trim();
		line = line.substring(line.indexOf("I"));
		
		inputSource = new FileInputSource(TestHelper.getResource("formats/chars_utf8.xml"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		// this should be blank rather than the title tag (for generic XML)
		assertEquals("", metadata.getTitle());
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream);
		inputStream.close();
		assertTrue("ensure we have some content in XML", contents.contains(line)==true);

		// try with xmlContentXpath parameter and multiple nodes
		extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, new FlexibleParameters(new String[]{"xmlContentXpath=//p"}));
		inputSource = new FileInputSource(TestHelper.getResource("formats/chars_utf8.xml"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		// this should be blank rather than the title tag (for generic XML)
		assertEquals("title for XML document", "", metadata.getTitle());
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream);
		inputStream.close();
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
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
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream);
		inputStream.close();
		//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("ensure we have stripped out other content", contents.contains("<head>")==false);
		assertTrue("ensure we have some content in XML with a single node xmlContentXpath parameter", contents.contains(line)==true);

		// try with RSS input format implicit (no inputFormat)
		extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, new FlexibleParameters());
		inputSource = new FileInputSource(TestHelper.getResource("xml/rss.xml"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		// this should be blank rather than the title tag (for generic XML)
		assertEquals("title for RSS feed", "Website Feed", metadata.getTitle());
//		assertEquals("author for RSS feed", "Me (me@example.com)", metadata.getAuthor());
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream);
		inputStream.close();
		assertFalse(contents.contains("<!--")); // make sure we've stripped out XML comments during extraction
		assertTrue("ensure we have stripped out other content in RSS feed", contents.contains("<link>")==false);
		assertTrue("ensure we have three lines of description in RSS feed", StringUtils.countMatches(contents, "<description>")==2);
		
		// try with RSS input format (explicit)
		extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, new FlexibleParameters(new String[]{"inputFormat=RSS"}));
		inputSource = new FileInputSource(TestHelper.getResource("xml/rss.xml"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		// this should be blank rather than the title tag (for generic XML)
		assertEquals("title for RSS feed", "Website Feed", metadata.getTitle());
//		assertEquals("author for RSS feed", "Me (me@example.com)", metadata.getAuthor());
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream);
		inputStream.close();
		assertFalse(contents.contains("<!--")); // make sure we've stripped out XML comments during extraction
		assertTrue("ensure we have stripped out other content in RSS feed", contents.contains("<link>")==false);
		assertTrue("ensure we have three lines of description in RSS feed", StringUtils.countMatches(contents, "<description>")==2);
		
		// try with XML
		extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, new FlexibleParameters(new String[]{"inputFormat=XML"}));
		inputSource = new FileInputSource(TestHelper.getResource("xml/rss.xml"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		// this should be blank rather than the title tag (for generic XML)
		assertEquals(0, metadata.getTitle().length());
//		assertEquals("author for RSS feed", "Me (me@example.com)", metadata.getAuthor());
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream);
		inputStream.close();
		assertFalse(contents.contains("<!--")); // make sure we've stripped out XML comments during extraction
		
		// make sure that we can keep multiple values for metadata
		extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, new FlexibleParameters(new String[]{"xmlTitleXpath=//title"}));
		inputSource = new FileInputSource(TestHelper.getResource("xml/rss.xml"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("title for RSS feed", "Website Feed", metadata.getTitle());
		
		// make sure we can join string values
		extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, new FlexibleParameters(new String[]{"xmlTitleXpath=string-join(//title,'--')"}));
		inputSource = new FileInputSource(TestHelper.getResource("xml/rss.xml"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		// this should be blank rather than the title tag (for generic XML)
		assertEquals("Website Feed--A Special Event--Announcing new Products", metadata.getTitle());
		
		// make sure we recognize XML in a string
		extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, new FlexibleParameters());
		inputSource = new StringInputSource("<a><b>c</b><b>d</b></a>");
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		// this should be blank rather than the title tag (for generic XML)
		assertEquals(DocumentFormat.XML, metadata.getDocumentFormat());

		// make sure we recognize HTML in a string
		extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, new FlexibleParameters());
		inputSource = new StringInputSource("<html><body><div>This is a current sentence.</div><div>d</div></body></html>");
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("en", metadata.getLanguageCode()); // make sure default is English
		// this should be blank rather than the title tag (for generic XML)
		assertEquals(DocumentFormat.HTML, metadata.getDocumentFormat());
		
		// make sure we find XPath in string XML
		extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, new FlexibleParameters(new String[]{"xmlContentXpath=//b", "xmlTitleXpath=//b[1]","language=fr"}));
		inputSource = new StringInputSource("<a><b>c</b><b>d &amp; e</b><z>x</z></a>");
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("fr", metadata.getLanguageCode()); // make sure our set value is respected
		// this should be blank rather than the title tag (for generic XML)
		assertEquals(DocumentFormat.XML, metadata.getDocumentFormat());
		assertEquals("c", metadata.getTitle());
//		String string = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream);
		inputStream.close();
		assertTrue(contents.contains("<a>") && contents.contains("<b>") && !contents.contains("<z>"));
		
		extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, new FlexibleParameters());
		inputSource = new FileInputSource(TestHelper.getResource("xml/fictionbook.xml"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("A Study in Scarlet", metadata.getTitle());
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream);
		inputStream.close();
		assertTrue(contents.contains("Frontispiece, with the caption"));
		assertFalse(contents.contains("Project Gutenberg"));

		
		storage.destroy();

	}

}

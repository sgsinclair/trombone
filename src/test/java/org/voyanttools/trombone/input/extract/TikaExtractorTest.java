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
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.voyanttools.trombone.document.Metadata;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

/**
 * @author sgs
 *
 */
public class TikaExtractorTest {
	
	@Test
	public void testStrings() throws IOException, URISyntaxException {
		Storage storage = TestHelper.getDefaultTestStorage();
		StoredDocumentSourceStorage storeDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		FlexibleParameters parameters = new FlexibleParameters();
		StoredDocumentSourceExtractor extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, parameters);
		
		InputSource inputSource;
		StoredDocumentSource storedDocumentSource;
		StoredDocumentSource extractedStoredDocumentSource;
		Metadata metadata;
		String contents;
		
		inputSource = new StringInputSource("This is <b>a</b> test.");
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("Text string shouldn't contain tags", contents.contains("&lt;b&gt;a&lt;/b&gt;"));
		
		inputSource = new StringInputSource("<html><body>This is <b>a</b> test.</body></html>");
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("HTML string should contain tags", contents.contains("<b>a</b>"));
		
		inputSource = new StringInputSource("<test>This is <b>a</b> test.</test>");
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("XML-looking string shouldn't contain tags", contents.contains("&lt;b&gt;a&lt;/b&gt;"));
		
		parameters.setParameter("inputFormat", "XML");
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, parameters);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("XML-declared string should contain tags", contents.contains("<b>a</b>"));
		
	}

	@Test
	public void testFormats() throws IOException, URISyntaxException {
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
		
		inputSource = new FileInputSource(TestHelper.getResource("formats/chars_utf8.txt"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("ensure we have two paragraphs in text", StringUtils.countMatches(contents, "<p>")==2);
		assertTrue("ensure we've escaped & in text", contents.contains("&amp;")==true);
		assertTrue("ensure we have some content in text", contents.contains(line)==true);

		inputSource = new FileInputSource(TestHelper.getResource("formats/chars.pages"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("title for Pages document", "Titre du document test de Pages", metadata.getTitle());
		assertEquals("author for Pages document", "Stéfan Sinclair", metadata.getAuthor());
		assertEquals("keywords for Pages document", "test, Pages", metadata.getKeywords());
		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("ensure we have some content in Pages", contents.contains(line)==true);
		
		inputSource = new FileInputSource(TestHelper.getResource("formats/chars.doc"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("title for MSWord (.doc) document", "Titre du document test de MSWord", metadata.getTitle());
		assertEquals("author for MSWord (.doc) document", "Stéfan Sinclair", metadata.getAuthor());
		assertEquals("keywords for MSWord (.doc) document", "test, MSWord", metadata.getKeywords());
		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("ensure we have some content in MSWord (.doc)", contents.contains(line)==true);

		inputSource = new FileInputSource(TestHelper.getResource("formats/chars.docx"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("title for MSWord (.docx) document", "Titre du document test de MSWord", metadata.getTitle());
		assertEquals("author for MSWord (.docx) document", "Stéfan Sinclair", metadata.getAuthor());
		assertEquals("keywords for MSWord (.docx) document", "test, MSWord", metadata.getKeywords());
		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("ensure we have some content in MSWord (.docx)", contents.contains(line)==true);

		inputSource = new FileInputSource(TestHelper.getResource("formats/chars.rtf"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("title for RTF document", "Titre du document test de RTF", metadata.getTitle());
		assertEquals("author for RTF document", "Stéfan Sinclair", metadata.getAuthor());
		assertEquals("keywords for RTF document", "test, RTF", metadata.getKeywords());
		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("ensure we have some content in RTF", contents.contains(line)==true);

		inputSource = new FileInputSource(TestHelper.getResource("formats/chars.pdf"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("title for PDF document", "Titre du document test de PDF", metadata.getTitle());
		assertEquals("author for PDF document", "Stéfan Sinclair", metadata.getAuthor());
		assertEquals("keywords for PDF document", "test, PDF", metadata.getKeywords());
		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("ensure we have some content in PDF", contents.contains(line)==true);
		
		inputSource = new FileInputSource(TestHelper.getResource("formats/chars_utf8.htm"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("title for HTML document", "Titre du document test de HTML", metadata.getTitle());
		assertEquals("author for HTML document", "Stéfan Sinclair", metadata.getAuthor());
		assertEquals("keywords for HTML document", "test, HTML", metadata.getKeywords());
		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("strip out script tag from html", contents.contains("script")==false);
		assertTrue("strip out style tag from html", contents.contains("style")==false);
		assertTrue("ensure we have some content in html", contents.contains(line)==true);

		storage.destroy();
	}

}

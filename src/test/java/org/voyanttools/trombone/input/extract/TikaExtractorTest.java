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
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.StringInputSource;
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
		DocumentMetadata metadata;
		InputStream inputStream;
		String contents;
		

		inputSource = new StringInputSource("This — is <b>a</b> test.");
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("Text string shouldn't contain tags", contents.contains("&lt;b&gt;a&lt;/b&gt;"));

		inputSource = new StringInputSource("<html><body><div>This is <b>a</b> test.</div></body></html>");
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("HTML string should contain tags", contents.contains("<b>a</b>"));
		assertEquals("en", extractedStoredDocumentSource.getMetadata().getLanguageCode());
		
		inputSource = new StringInputSource("<html><body><section><div>This is <b>a</b> test.</div></section></body></html>");
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("HTML string should contain tags", contents.contains("<b>a</b>"));
// TODO: find a way to keep html5 tags with xhtml transformer		assertTrue("HTML string should contain HTML5 tags", contents.contains("<section>"));
		
		inputSource = new StringInputSource("<test>This is <b>a</b> test.</test>");
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("XML-looking string should contain tags", contents.contains("<b>a</b>"));
		
		parameters.setParameter("inputFormat", "XML");
		parameters.setParameter("language", "no"); // make sure we can override language
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, parameters);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("XML-declared string should contain tags", contents.contains("<b>a</b>"));
		assertEquals("no", extractedStoredDocumentSource.getMetadata().getLanguageCode()); // check override language (from params above)

		
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
		DocumentMetadata metadata;
		InputStream inputStream;
		String contents;
		
		String line;
		
		line = FileUtils.readLines(TestHelper.getResource("formats/chars_utf8.txt"), "UTF-8").get(0).trim();
		line = line.substring(line.indexOf("I"));
		
		inputSource = new FileInputSource(TestHelper.getResource("formats/snippet.txt"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
//		assertEquals("chars_utf8", metadata.getTitle());
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
//		assertTrue("ensure we have two paragraphs in text", StringUtils.countMatches(contents, "<p>")==2);
//		assertTrue("ensure we've escaped & in text", contents.contains("&amp;")==true);
//		assertTrue("ensure we have some content in text", contents.contains(line)==true);

		inputSource = new FileInputSource(TestHelper.getResource("formats/chars_utf8.txt"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("chars_utf8", metadata.getTitle());
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
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
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		assertTrue("ensure we have some content in Pages", contents.contains(line)==true);
		
		inputSource = new FileInputSource(TestHelper.getResource("formats/chars.doc"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("title for MSWord (.doc) document", "Titre du document test de MSWord", metadata.getTitle());
		assertEquals("author for MSWord (.doc) document", "Stéfan Sinclair", metadata.getAuthor());
		assertEquals("keywords for MSWord (.doc) document", "test, MSWord", metadata.getKeywords());
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
		assertTrue("ensure we have some content in MSWord (.doc)", contents.contains(line)==true);

		inputSource = new FileInputSource(TestHelper.getResource("formats/chars.docx"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("title for MSWord (.docx) document", "Titre du document test de MSWord", metadata.getTitle());
		assertEquals("author for MSWord (.docx) document", "Stéfan Sinclair", metadata.getAuthor());
		assertEquals("keywords for MSWord (.docx) document", "test, MSWord", metadata.getKeywords());
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
		assertTrue("ensure we have some content in MSWord (.docx)", contents.contains(line)==true);

		inputSource = new FileInputSource(TestHelper.getResource("formats/chars.rtf"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("title for RTF document", "Titre du document test de RTF", metadata.getTitle());
		assertEquals("author for RTF document", "Stéfan Sinclair", metadata.getAuthor());
		assertEquals("keywords for RTF document", "test, RTF", metadata.getKeywords());
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
		assertTrue("ensure we have some content in RTF", contents.contains(line)==true);

		inputSource = new FileInputSource(TestHelper.getResource("formats/chars.pdf"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("title for PDF document", "Titre du document test de PDF", metadata.getTitle());
		assertEquals("author for PDF document", "Stéfan Sinclair", metadata.getAuthor());
		assertEquals("keywords for PDF document", "test, PDF", metadata.getKeywords());
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
		assertTrue("ensure we have some content in PDF", contents.contains(line)==true);
		
		inputSource = new FileInputSource(TestHelper.getResource("formats/chars_utf8.htm"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("fr", metadata.getLanguageCode());
		assertEquals("title for HTML document", "Titre du document test de HTML", metadata.getTitle());
		assertEquals("author for HTML document", "Stéfan Sinclair", metadata.getAuthor());
		assertEquals("keywords for HTML document", "test, HTML", metadata.getKeywords());
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
		assertTrue("strip out script tag from html", contents.contains("script")==false);
		assertTrue("strip out style tag from html", contents.contains("style")==false);
		assertTrue("ensure we have some content in html", contents.contains(line)==true);
		
		inputSource = new FileInputSource(TestHelper.getResource("formats/chars.xlsx"));
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("title for XLSX document", "chars", metadata.getTitle());
//		contents = IOUtils.toString(storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId()));
		inputStream = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
		assertTrue("strip out script tag from html", contents.contains("script")==false);
		assertTrue("strip out style tag from html", contents.contains("style")==false);
		assertTrue("ensure we have some content in html", contents.contains(line)==true);

		storage.destroy();
	}

}

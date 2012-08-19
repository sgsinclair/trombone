package org.voyanttools.trombone.tool;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.tika.io.IOUtils;
import org.junit.Test;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;

public class DocumentExtractorTest {

	@Test
	public void test() throws IOException {
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("formats/chars.rtf")});
		Storage storage = TestHelper.getDefaultTestStorage();
		
		// store
		DocumentStorer storer = new DocumentStorer(storage, parameters);
		storer.run();
		
		// extract
		parameters.setParameter("storedId", storer.getStoredId());
		DocumentExtractor extractor = new DocumentExtractor(storage, parameters);
		extractor.run();

		List<StoredDocumentSource> storedDocumentSources = extractor.getStoredDocumentSources();
		
		// make sure we have some plausible content
		String line = FileUtils.readLines(TestHelper.getResource("formats/chars_utf8.txt")).get(0).trim();
		line = line.substring(line.indexOf("I"));
		String original;
		InputStream is = null;
		try {
			String id = storedDocumentSources.get(0).getId();
			is = storage.getStoredDocumentSourceStorage().getStoredDocumentSourceInputStream(id);
			original = IOUtils.toString(is);
		}
		finally {
			if (is!=null) is.close();
		}
		assertTrue(original.contains(line));

		// ensure we have two documents
		assertEquals(1, storedDocumentSources.size());
		
		XStream xstream;
		
		// serialize to XML
		xstream = new XStream();
		xstream.autodetectAnnotations(true);
		String xml = xstream.toXML(extractor);
		assertTrue(xml.startsWith("<extractedStoredDocuments>"));
		
		
	    Matcher matcher = Pattern.compile("<storedId>(.+?)</storedId>").matcher(xml);
	    assertTrue(matcher.find()); // we should match
	    String id = matcher.group(1);
	    List<String> ids = storage.retrieveStrings(id);
	    for (int i=0, len=ids.size(); i<len; i++) {
	    	assertEquals(ids.get(i),storedDocumentSources.get(i).getId());
	    }

	    // serialize to JSON
		xstream = new XStream(new JsonHierarchicalStreamDriver());
		xstream.autodetectAnnotations(true);
		String json = xstream.toXML(extractor);
		Gson gson = new Gson();
		StringMap<StringMap> obj = gson.fromJson(json, StringMap.class);
		StringMap<String> sd = obj.get("extractedStoredDocuments");
		String idString = (String) sd.get("storedId");
		assertEquals(id, idString);	}

}

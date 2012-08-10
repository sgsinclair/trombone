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
package org.voyanttools.trombone.tool;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;

/**
 * @author sgs
 * 
 */
public class DocumentStorerTest {

	@Test
	public void test() throws IOException, ParserConfigurationException, SAXException {
		
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"string=test","string=another test"});
		Storage storage = TestHelper.getDefaultTestStorage();
		DocumentStorer tool = new DocumentStorer(storage, parameters);
		tool.run();
		
		// ensure we have two documents
		assertEquals(2, tool.getStoredDocumentSources().size());
		
		XStream xstream;
		
		// serialize to XML
		xstream = new XStream();
		xstream.autodetectAnnotations(true);
		String xml = xstream.toXML(tool);
		assertTrue(xml.startsWith("<storedDocuments>"));
	    assertEquals(1, StringUtils.countMatches(xml, "<ids>")); // two documents, so two IDs
	    assertEquals(2, StringUtils.countMatches(xml, "<string>")); // two documents, so two IDs
	    
	    // serialize to JSON
		xstream = new XStream(new JsonHierarchicalStreamDriver());
		xstream.autodetectAnnotations(true);
		String json = xstream.toXML(tool);
		Gson gson = new Gson();
		StringMap<StringMap> obj = gson.fromJson(json, StringMap.class);
		StringMap<ArrayList> sd = obj.get("storedDocuments");
		ArrayList<String> ids = (ArrayList<String>) sd.get("ids");
		assertEquals(2, ids.size());
    
	}
}

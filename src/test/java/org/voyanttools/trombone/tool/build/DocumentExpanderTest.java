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
package org.voyanttools.trombone.tool.build;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.build.DocumentExpander;
import org.voyanttools.trombone.tool.build.DocumentStorer;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;

/**
 * @author sgs
 *
 */
public class DocumentExpanderTest {

	@Test(expected=IOException.class)
	public void testDocumentNotStored() throws IOException {
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"storedId=1"});
		Storage storage = TestHelper.getDefaultTestStorage();
		DocumentExpander tool = new DocumentExpander(storage, parameters);
		tool.run();
	}

	@Test
	public void test() throws IOException {
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("archive/archive.zip")});
		Storage storage = TestHelper.getDefaultTestStorage();
		DocumentStorer storer = new DocumentStorer(storage, parameters);
		storer.run();
		String storedId = storer.getStoredId();
		
		parameters.setParameter("storedId", storedId);
		
		DocumentExpander expander = new DocumentExpander(storage, parameters);
		expander.run();
		List<StoredDocumentSource> storedDocumentSources = expander.getStoredDocumentSources();

		// ensure we have two documents
		assertEquals(2, storedDocumentSources.size());
		
		XStream xstream;
		
		// serialize to XML
		xstream = new XStream();
		xstream.autodetectAnnotations(true);
		String xml = xstream.toXML(expander);
		assertTrue(xml.startsWith("<expandedStoredDocuments>"));
		
		
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
		String json = xstream.toXML(expander);
		Gson gson = new Gson();
		StringMap<StringMap> obj = gson.fromJson(json, StringMap.class);
		StringMap<String> sd = obj.get("expandedStoredDocuments");
		String idString = (String) sd.get("storedId");
		assertEquals(id, idString);
	}

}

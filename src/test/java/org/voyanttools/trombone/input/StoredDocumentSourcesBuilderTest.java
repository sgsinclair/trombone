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
package org.voyanttools.trombone.input;


import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

/**
 * @author sgs
 *
 */
public class StoredDocumentSourcesBuilderTest {

	@Test
	public void test() throws IOException, URISyntaxException {
		Storage storage = TestHelper.getDefaultTestStorage();
		FlexibleParameters parameters = new FlexibleParameters();
		parameters.addParameter("string", "this is a test");
		parameters.addParameter("file", TestHelper.getResource("xml/rss.xml").toString());
		parameters.addParameter("file", TestHelper.getResource("xml/").toString());
		ExpandedStoredDocumentSourcesBuilder builder = new ExpandedStoredDocumentSourcesBuilder(storage.getStoredDocumentSourceStorage(), parameters);
		List<StoredDocumentSource> storedDocumentSources = builder.getStoredDocumentSources();
		assertEquals("we should have three documents", 3, storedDocumentSources.size());
		storage.destroy();
	}

}

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
package org.voyanttools.trombone.document;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URI;

import org.junit.Test;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.input.source.UriInputSource;
import org.voyanttools.trombone.util.EmbeddedWebServer;

/**
 * @author sgs
 *
 */
public class MetadataTest {

	@Test
	public void testDocumentFormats() throws Exception {
		
		InputSource inputSource;
		
		inputSource = new StringInputSource("test");
		assertEquals("string document should be unknown format", DocumentFormat.UNKNOWN, inputSource.getMetadata().getDocumentFormat());
		
		inputSource = new FileInputSource(new File("junk.xml"));
		assertEquals("file with no extension document should be unknown format", DocumentFormat.XML, inputSource.getMetadata().getDocumentFormat());

		inputSource = new FileInputSource(new File("junk"));
		assertEquals("file with no extension document should be unknown format", DocumentFormat.UNKNOWN, inputSource.getMetadata().getDocumentFormat());
		
		EmbeddedWebServer webServer = new EmbeddedWebServer();
		webServer.start();

		try {
			StringBuilder uriBuilder = new StringBuilder("http://localhost:").append(webServer.getPort());
			
			inputSource = new UriInputSource(new URI(uriBuilder.toString()));
			assertEquals("URI with no path should default to server format (HTML)", DocumentFormat.HTML, inputSource.getMetadata().getDocumentFormat());
	
			uriBuilder.append("/");
			inputSource = new UriInputSource(new URI(uriBuilder.toString()));
			assertEquals("URI with slash path should default to server format (HTML)", DocumentFormat.HTML, inputSource.getMetadata().getDocumentFormat());
	
			uriBuilder.append("xml");
			inputSource = new UriInputSource(new URI(uriBuilder.toString()));
			assertEquals("URI with no file extension should default to server format (HTML)", DocumentFormat.HTML, inputSource.getMetadata().getDocumentFormat());
			
			uriBuilder.append("/rss.xml");
			inputSource = new UriInputSource(new URI(uriBuilder.toString()));
			assertEquals("URI with XML extension should default to XML", DocumentFormat.XML, inputSource.getMetadata().getDocumentFormat());
			
			uriBuilder.append("/rss.xml?html=1");
			inputSource = new UriInputSource(new URI(uriBuilder.toString()));
			assertEquals("URI with XML extension and query should default to XML", DocumentFormat.XML, inputSource.getMetadata().getDocumentFormat());
		}
		finally {
			webServer.stop();
		}
	}

}

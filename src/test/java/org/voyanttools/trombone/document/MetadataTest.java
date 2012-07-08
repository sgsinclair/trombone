/**
 * 
 */
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

package org.voyanttools.trombone.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.EmbeddedWebServer;
import org.voyanttools.trombone.util.TestHelper;

public class KeywordsTest {

	@Test
	public void test() throws Exception {
		
		Storage storage = TestHelper.getDefaultTestStorage();
		Keywords keywords;
		
		keywords = new Keywords();
		
		// test named list, comma-separated values and multiple strings
		keywords.load(storage, new String[]{"stop.en.taporware.txt,testzz","testaa"});
		assertTrue(keywords.isKeyword("the"));
		assertTrue(keywords.isKeyword("testzz"));
		assertTrue(keywords.isKeyword("testaa"));
		assertFalse(keywords.isKeyword("word"));
		
		// try from stored data
		String id = storage.storeStrings(keywords.getKeywords());
		keywords = new Keywords();
		keywords.load(storage, new String[]{"keywords-"+id});
		assertTrue(keywords.isKeyword("the"));
		assertTrue(keywords.isKeyword("testzz"));
		assertTrue(keywords.isKeyword("testaa"));
		assertFalse(keywords.isKeyword("word"));

		/* FIXME: re-enable this 
		// try with a URL
		EmbeddedWebServer webServer = new EmbeddedWebServer();
		webServer.start();
		try {
			keywords = new Keywords();
			String uri = "http://localhost:"+webServer.getPort()+"/keywords/stop.en.taporware.txt";
			keywords.load(storage, new String[]{uri+",testzz","testaa"});
			assertTrue(keywords.isKeyword("the"));
			assertTrue(keywords.isKeyword("testzz"));
			assertTrue(keywords.isKeyword("testaa"));
			assertFalse(keywords.isKeyword("word"));
		}
		finally {
			webServer.stop();
		}
		*/
		
		storage.destroy();
		
	}

}

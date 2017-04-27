package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.*;

import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class VelizaTest {

	@Test
	public void test() throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();

		FlexibleParameters parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("udhr")});
		CorpusCreator creator = new CorpusCreator(storage, parameters);
		creator.run();
		parameters.setParameter("corpus", creator.getStoredId());

		Veliza veliza;
		
		// test empty
		veliza = new Veliza(storage, parameters);
		try {
			veliza.run();
		} catch (RuntimeException e) {
			assertEquals("Veliza needs an input sentence to function.", e.getMessage());
		}
		parameters.setParameter("sentence", "");
		veliza = new Veliza(storage, parameters);
		try {
			veliza.run();
		} catch (RuntimeException e) {
			assertEquals("Veliza needs an input sentence to function.", e.getMessage());
		}
		
		// test junk
		parameters.setParameter("sentence", "asfkafjaa");
		veliza = new Veliza(storage, parameters);
		veliza.run();
// commented because it's now selected randomly
//		assertEquals("I'm not sure I understand you fully.", veliza.response);
//		assertEquals(0, veliza.previous.length);
		
		// test normal
		parameters.setParameter("sentence", "What is the meaning of this text?");
		veliza = new Veliza(storage, parameters);
		veliza.run();
		assertTrue(veliza.response!=null && veliza.response.length()>0);
		assertEquals(1, veliza.previous.length);
		// commented because it's now selected randomly
//		assertEquals("Why do you ask ?", veliza.previous[0]);

		// test memory
		parameters.setParameter("previous", new String[]{"Why do you ask ?", "two"});
		parameters.setParameter("sentence", "asfkafjaa");
		veliza = new Veliza(storage, parameters);
		veliza.run();
		// commented because it's now selected randomly
//		assertEquals("Why do you ask ?", veliza.response);
		assertEquals(2, veliza.previous.length);
//		assertEquals("Why do you ask ?", veliza.previous[0]); // order of previous matters, take first
		
		// test from text
		parameters.setParameter("fromCorpus", "true");
		veliza = new Veliza(storage, parameters);
		veliza.run();
		assertTrue(veliza.response!=null && veliza.response.length()>0);
		
		
		
		// test sentences
		String string = "This is a sentence.  It has fruits, vegetables,\n etc. but does not have meat.  Mr. Smith went to Washington.";
		List<String> sentences = veliza.getSentences(string);
		assertEquals(3, sentences.size());
	}

}

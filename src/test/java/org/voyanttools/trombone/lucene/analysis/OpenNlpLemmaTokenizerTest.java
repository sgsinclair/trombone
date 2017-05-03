package org.voyanttools.trombone.lucene.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Test;
import org.voyanttools.trombone.nlp.NlpFactory;
import org.voyanttools.trombone.nlp.OpenNlpAnnotator;
import org.voyanttools.trombone.nlp.PosLemmas;

public class OpenNlpLemmaTokenizerTest {

	@Test
	public void test() throws IOException {
		NlpFactory factory = new NlpFactory();
		OpenNlpAnnotator annotator;
		PosLemmas lemmas;
		
		annotator = factory.getOpenNlpAnnotator("en");
		lemmas = annotator.getPosLemmas("These dogs are interesting.", annotator.getLang());
		lemmas.iterator();
		lemmas.next();
		lemmas.next();
		assertEquals("dog", lemmas.getCurrentLemma());
		lemmas.next();
		lemmas.next();
		assertEquals("interesting", lemmas.getCurrentLemma());
		assertFalse(lemmas.hasNext());
	}
}

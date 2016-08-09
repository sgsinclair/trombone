package org.voyanttools.trombone.lucene;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.junit.Test;
import org.voyanttools.trombone.lucene.analysis.HTMLCharFilter;
import org.voyanttools.trombone.lucene.analysis.UimaLemmaTokenizer;
import org.voyanttools.trombone.nlp.NlpFactory;

public class UimaLemmaTokenizerTest {

	// FIXME: restore this test
	// @Test
	public void testLanguages() throws IOException {
		NlpFactory factory = new NlpFactory();
		AnalysisEngine engine;
		Reader reader;
		Tokenizer tokenizer;
		String[] lemmas;
		CharTermAttribute termAtt;
		int i;
		
		// test English
		engine = factory.getLemmatizationAnalysisEngine("en");
		// should normally be filtered by analyzer
		reader = new HTMLCharFilter(new StringReader("These dogs <b>are</b> interesting."));
		tokenizer = new UimaLemmaTokenizer(engine, "en");
		tokenizer.setReader(reader);
		tokenizer.reset();
		lemmas = new String[]{"these","dog","be","interesting"};
		i = 0;
		while (tokenizer.incrementToken()) {
			termAtt = tokenizer.getAttribute(CharTermAttribute.class);
			assertEquals(termAtt.toString(), lemmas[i]);
			i++;
		}
		assertEquals(i, 3); // FIXME: why is the last lemma being dropped from the iterator?
		tokenizer.end();
		tokenizer.close();
		
		// test French
		engine = factory.getLemmatizationAnalysisEngine("fr");
		// should normally be filtered by analyzer
		reader = new HTMLCharFilter(new StringReader("Ces chiens <b>sont</b> intéressants."));
		tokenizer = new UimaLemmaTokenizer(engine, "fr");
		tokenizer.setReader(reader);
		tokenizer.reset();
		lemmas = new String[]{"ce","chien","être","intéressant"};
		i = 0;
		while (tokenizer.incrementToken()) {
			termAtt = tokenizer.getAttribute(CharTermAttribute.class);
			assertEquals(termAtt.toString(), lemmas[i]);
			i++;
		}
		assertEquals(i, 3); // FIXME: why is the last lemma being dropped from the iterator?
		tokenizer.end();
		tokenizer.close();
	}
}

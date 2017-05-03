package org.voyanttools.trombone.lucene.analysis;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.junit.Test;
import org.voyanttools.trombone.nlp.NlpAnnotator;
import org.voyanttools.trombone.nlp.NlpFactory;
import org.voyanttools.trombone.nlp.StanfordNlpAnnotator;

public class StanfordNlpLemmaTokenizerTest {

	@Test
	public void test() throws IOException {
		NlpFactory nlpFactory = new NlpFactory();
		
		NlpAnnotator annotator = nlpFactory.getNlpAnnotator("en");
		CharTermAttribute termAtt;
		if (annotator instanceof StanfordNlpAnnotator) {
			Tokenizer tokenizer = new StanfordNlpLemmaTokenizer((StanfordNlpAnnotator) annotator);
			Reader reader = new HTMLCharFilter(new StringReader("These dogs <b>are</b> interesting."));
			tokenizer.setReader(reader);
			tokenizer.reset();
			String[] lemmas = new String[]{"these","dog","be","interesting"};
			int i = 0;
			while (tokenizer.incrementToken()) {
				termAtt = tokenizer.getAttribute(CharTermAttribute.class);
				assertEquals(termAtt.toString(), lemmas[i]);
				System.out.println(tokenizer.getAttribute(OffsetAttribute.class).startOffset());
				i++;
			}
			assertEquals(i, 4); // FIXME: why is the last lemma being dropped from the iterator?
			tokenizer.end();
			tokenizer.close();
		}
		
		/*
		annotator = nlpFactory.getNlpAnnotator("fr");
		if (annotator instanceof StanfordNlpAnnotator) {
			Tokenizer tokenizer = new StanfordNlpLemmaTokenizer((StanfordNlpAnnotator) annotator);
			Reader reader = new HTMLCharFilter(new StringReader("Ces chiens <b>sont</b> intéressants."));
			tokenizer.setReader(reader);
			tokenizer.reset();
			String[] lemmas = new String[]{"ce","chien","sont","intéressant"};
			int i = 0;
			while (tokenizer.incrementToken()) {
				termAtt = tokenizer.getAttribute(CharTermAttribute.class);
				assertEquals(termAtt.toString(), lemmas[i]);
				System.out.println(tokenizer.getAttribute(OffsetAttribute.class).startOffset());
				i++;
			}
			assertEquals(i, 4); // FIXME: why is the last lemma being dropped from the iterator?
			tokenizer.end();
			tokenizer.close();
		}
		*/

	}

}

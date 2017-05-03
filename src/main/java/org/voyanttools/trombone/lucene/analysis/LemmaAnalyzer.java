/**
 * 
 */
package org.voyanttools.trombone.lucene.analysis;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.nlp.NlpFactory;
import org.voyanttools.trombone.nlp.OpenNlpAnnotator;
import org.voyanttools.trombone.nlp.PosLemmas;

/**
 * @author sgs
 *
 */
public class LemmaAnalyzer extends LexicalAnalyzer {
	
	private NlpFactory factory;
	
	private OpenNlpLemmaTokenizer openNlpTokenizer;
	
	public LemmaAnalyzer(NlpFactory factory) {
		this.factory = factory;
	}

	@Override
	protected Reader initReader(String fieldName, Reader reader) {

		if (fieldName.equals(TokenType.lemma.name())) {
			reader = initReader(reader);
		}
		else {
			parameters.clear();
		}

		try {
			return new HTMLCharFilter(reader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {

		if (fieldName.equals(TokenType.lemma.name()) && lang!=null && lang.isEmpty()==false) {
			/*
			NlpAnnotator annotator = factory.getNlpAnnotator(lang);
			if (annotator instanceof StanfordNlpAnnotator) {
				Tokenizer tokenizer = new StanfordNlpLemmaTokenizer((StanfordNlpAnnotator) annotator);
				TokenStream stream = new LowerCaseFilter(tokenizer);
				return new TokenStreamComponents(tokenizer, stream);
			} */
			if (lang.equals("en") || lang.equals("fr") || lang.equals("de") || lang.equals("it") || lang.equals("nl")) {
				OpenNlpAnnotator annotator = factory.getOpenNlpAnnotator(lang);
				openNlpTokenizer = new OpenNlpLemmaTokenizer(annotator);
				TokenStream stream = new LowerCaseFilter(openNlpTokenizer);
				return new TokenStreamComponents(openNlpTokenizer, stream);
			} else {
				throw new RuntimeException("Unable to create Lemmatizer for "+lang);
			}
		}
		
		// not sure this is a good idea, but let's use lexical forms for now
		return super.createComponents(TokenType.lexical.name());
	}
	
	public PosLemmas getPostStreamPosLemmas() {
		return openNlpTokenizer.getPosLemmas();
	}
}

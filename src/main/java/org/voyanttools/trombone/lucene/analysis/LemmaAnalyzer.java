/**
 * 
 */
package org.voyanttools.trombone.lucene.analysis;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.nlp.NlpFactory;

/**
 * @author sgs
 *
 */
public class LemmaAnalyzer extends LexicalAnalyzer {
	
	private NlpFactory factory;
	
	LemmaAnalyzer(NlpFactory factory) {
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
			AnalysisEngine engine = factory.getLemmatizationAnalysisEngine(lang);
			if (engine!=null) { // confirm that this language engine is available
				Tokenizer tokenizer = new UimaLemmaTokenizer(engine, lang);
				TokenStream stream = new LowerCaseFilter(tokenizer);
				return new TokenStreamComponents(tokenizer, stream);
			}
		}
		
		// not sure this is a good idea, but let's use lexical forms for now
		return super.createComponents(TokenType.lexical.name());
	}
}

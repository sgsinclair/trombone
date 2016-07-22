/**
 * 
 */
package org.voyanttools.trombone.lucene.analysis;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.tika.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.voyanttools.trombone.nlp.NlpFactory;
import org.voyanttools.trombone.nlp.uima.ICUSegmenter;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * This tokenizer produces a {@link TokenStream} with lemmas.
 * 
 * In particular, it reads from the current input source to produce a UIMA
 * document and processing pipeline with
 * <a href="https://dkpro.github.io/">DKPro</a> as an intermediary. The tokenizer
 * only works with some languages ({@link NlpFactory#getLemmatizationAnalysisEngine(String)})
 * and uses very simple segmentation ({@link ICUSegmenter}).
 * 
 * Thanks to Richard Eckart for help with this. 
 * 
 * @author Stéfan Sinclair
 */
public class UimaLemmaTokenizer extends Tokenizer {

	private AnalysisEngine engine;
	private String lang;
	private PositionIncrementAttribute posIncr;
	private CharTermAttribute termAtt;
	private OffsetAttribute offsetAttribute;
	private Iterator<Token> tokensIterator;
	private Token token;
	private Lemma lemma;

	public UimaLemmaTokenizer(AnalysisEngine engine, String lang) {
		this.engine = engine;
		this.lang = lang;
		posIncr = addAttribute(PositionIncrementAttribute.class);
		termAtt = addAttribute(CharTermAttribute.class);
		offsetAttribute = addAttribute(OffsetAttribute.class);
	}

	@Override
	public boolean incrementToken() throws IOException {
		clearAttributes();
		token = tokensIterator.next();
		termAtt.setEmpty();
		lemma = token.getLemma();
		if (lemma != null) {
			termAtt.append(lemma.getValue());
			termAtt.setLength(lemma.getValue().length());
			offsetAttribute.setOffset(token.getBegin(), token.getEnd());
			posIncr.setPositionIncrement(1);
		}
		return tokensIterator.hasNext();
	}

	@Override
	public void reset() throws IOException {
		super.reset();

		JCas jcas;
		try {
			jcas = JCasFactory.createJCas();
		} catch (UIMAException e) {
			throw new RuntimeException("Unable to instantiate document for lemmatization", e);
		}

		jcas.setDocumentLanguage(lang);
		jcas.setDocumentText(IOUtils.toString(input));

		try {
			engine.process(jcas);
		} catch (AnalysisEngineProcessException e) {
			throw new IOException(e);
		}

		Collection<Token> tokens = JCasUtil.select(jcas, Token.class);
		tokensIterator = tokens.iterator();
	}
}

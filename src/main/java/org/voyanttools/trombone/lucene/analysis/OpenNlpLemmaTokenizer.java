package org.voyanttools.trombone.lucene.analysis;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.tika.io.IOUtils;
import org.voyanttools.trombone.nlp.OpenNlpAnnotator;
import org.voyanttools.trombone.nlp.PosLemmas;

import opennlp.tools.util.Span;

final public class OpenNlpLemmaTokenizer extends Tokenizer {

	private OpenNlpAnnotator annotator;
	private Iterator<PosLemmas> tokensIterator;
	private PositionIncrementAttribute posIncr;
	private CharTermAttribute termAtt;
	private OffsetAttribute offsetAttribute;
	PosLemmas lemmas = null;
	
	public OpenNlpLemmaTokenizer(OpenNlpAnnotator annotator) {
		super();
		this.annotator = annotator;
		posIncr = addAttribute(PositionIncrementAttribute.class);
		termAtt = addAttribute(CharTermAttribute.class);
		offsetAttribute = addAttribute(OffsetAttribute.class);
	}

	@Override
	public boolean incrementToken() throws IOException {
		clearAttributes();
		PosLemmas token = tokensIterator.next();
		termAtt.setEmpty();
		String lemma = token.getCurrentLemma();
		if (lemma != null) {
			termAtt.append(lemma);
			termAtt.setLength(lemma.length());
			int correctedStart = correctOffset(token.getCurrentStart());
			int correctedEnd = correctOffset(token.getCurrentEnd());
			token.setCurrentOffset(correctedStart, correctedEnd);
			offsetAttribute.setOffset(correctedStart, correctedEnd);
			posIncr.setPositionIncrement(1);
			
		}
		return tokensIterator.hasNext();
	}
	
	@Override
	public void reset() throws IOException {
		super.reset();
		String text = IOUtils.toString(input);
		lemmas =  annotator.getPosLemmas(text, annotator.getLang());
		tokensIterator = lemmas.iterator();
	}
	
	public PosLemmas getPosLemmas() {
		return lemmas;
	}

}

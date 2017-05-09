package org.voyanttools.trombone.lucene.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.tika.io.IOUtils;
import org.voyanttools.trombone.nlp.StanfordNlpAnnotator;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

final public class StanfordNlpLemmaTokenizer extends Tokenizer {
	
	private StanfordNlpAnnotator annotator;
	private Iterator<CoreLabel> tokensIterator;
	private PositionIncrementAttribute posIncr;
	private CharTermAttribute termAtt;
	private OffsetAttribute offsetAttribute;

	public StanfordNlpLemmaTokenizer(StanfordNlpAnnotator annotator) {
		super();
		this.annotator = annotator;
		posIncr = addAttribute(PositionIncrementAttribute.class);
		termAtt = addAttribute(CharTermAttribute.class);
		offsetAttribute = addAttribute(OffsetAttribute.class);
	}

	@Override
	public boolean incrementToken() throws IOException {
		clearAttributes();
		CoreLabel token = tokensIterator.next();
		termAtt.setEmpty();
		String lemma = token.lemma();
		if (lemma != null) {
			termAtt.append(lemma);
			termAtt.setLength(lemma.length());
			offsetAttribute.setOffset(token.beginPosition(), token.endPosition());
			posIncr.setPositionIncrement(1);
		}
		return tokensIterator.hasNext();
	}
	
	@Override
	public void reset() throws IOException {
		super.reset();
		Annotation document = annotator.getAnnotated(IOUtils.toString(input));
		Collection<CoreMap> sentences = document.get(SentencesAnnotation.class);
		Collection<CoreLabel> tokens = new ArrayList<CoreLabel>();
		for (CoreMap sentence : sentences) {
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
				tokens.add(token);
			}
		}
		tokensIterator = tokens.iterator();
	}
}

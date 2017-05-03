package org.voyanttools.trombone.nlp.uima;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class ICUSegmenter extends JCasAnnotator_ImplBase {
	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		String text = aJCas.getDocumentText();
		StringReader reader = new StringReader(text);
		Tokenizer tokenizer = new ICUTokenizer();
		tokenizer.setReader(reader);
		
		OffsetAttribute offsetAttribute;
		Annotation seg;

		int start = -1;
		int end = 0;
		
		try {
			tokenizer.reset();
			while(tokenizer.incrementToken()) {
				offsetAttribute = tokenizer.getAttribute(OffsetAttribute.class);
				if (start==-1) start = offsetAttribute.startOffset();
				end = offsetAttribute.endOffset();
				seg = new Token(aJCas, offsetAttribute.startOffset(), offsetAttribute.endOffset());
				seg.addToIndexes(aJCas);
			}
			tokenizer.end();
			tokenizer.close();
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}

		if (start>-1) { // for now just create one long sentence
			seg = new Sentence(aJCas, start, end);
			seg.addToIndexes(aJCas);
		}

	}

}

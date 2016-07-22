/**
 * 
 */
package org.voyanttools.trombone.nlp;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.voyanttools.trombone.nlp.uima.ICUSegmenter;

import de.tudarmstadt.ukp.dkpro.core.matetools.MateLemmatizer;
//import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
//import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
//import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosTagger;

/**
 * The primary purpose of this factory class is to store reusable language models and data for NLP operations.
 * Methods are synchronized to avoid multiple concurrent loading of data.
 * 
 * @author Stéfan Sinclair
 */
public class NlpFactory {
	
	// full NLP annotation
	private Map<String, NlpAnnotator> nlpAnnotators = new HashMap<String, NlpAnnotator>();
	
	// minimalist lemmatization
	private Map<String, AnalysisEngine> lemmatizationAnalysisEngines = new HashMap<String, AnalysisEngine>();

	/**
	 * Get an {@link NlpAnnotator} for the specified language
	 * @param languageCode
	 * @return
	 */
	public synchronized NlpAnnotator getNlpAnnotator(String languageCode) {
		if (!nlpAnnotators.containsKey(languageCode)) {
			NlpAnnotator nlpAnnotator = new StanfordNlpAnnotator(languageCode);
			nlpAnnotators.put(languageCode, nlpAnnotator);
		}
		return nlpAnnotators.get(languageCode);
	}
	
	/**
	 * Get a UIMA {@link AnalysisEngine} for the specified language (or null if the language isn't supported). This is
	 * optimized for lemmatization only, as opposed to a full NLP annotation (including part-of-speech, etc.).
	 * 
	 * At the moment this uses <a href="http://www.ims.uni-stuttgart.de/forschung/ressourcen/werkzeuge/matetools.en.html">Mate Tools</a> in a <a href="https://dkpro.github.io/dkpro-core/releases/1.8.0/docs/component-reference.html#engine-MateLemmatizer">DKPro</a> pipeline.
	 * @param languageCode the language of the document (currently supported: en, fr, es, de)
	 * @return a UIMA {@link AnalysisEngine} for the specified language
	 */
	public synchronized AnalysisEngine getLemmatizationAnalysisEngine(String languageCode) {
		if (languageCode.equals("en") || languageCode.equals("fr") || languageCode.equals("es") || languageCode.equals("de")) {
			if (lemmatizationAnalysisEngines.containsKey(languageCode)==false) {
				AnalysisEngine engine;
				try {
					AnalysisEngineDescription segmenter = AnalysisEngineFactory.createEngineDescription(ICUSegmenter.class, new Object[0]);
					AnalysisEngineDescription lemmatizer = AnalysisEngineFactory.createEngineDescription(MateLemmatizer.class, new Object[0]);
					AnalysisEngineDescription engineDescription = AnalysisEngineFactory. createEngineDescription(segmenter, lemmatizer);
					engine = AnalysisEngineFactory.createEngine(engineDescription);
					lemmatizationAnalysisEngines.put(languageCode, engine);
				} catch (ResourceInitializationException e) {
					throw new RuntimeException("Unable to initialize a needed analysis engine during lemmatization.", e);
				}
			}
			return lemmatizationAnalysisEngines.get(languageCode);
		} else {
			return null;
		}
	}
}

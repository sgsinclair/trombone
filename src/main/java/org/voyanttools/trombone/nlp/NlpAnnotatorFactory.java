/**
 * 
 */
package org.voyanttools.trombone.nlp;

import java.util.HashMap;
import java.util.Map;

/**
 * @author sgs
 *
 */
public class NlpAnnotatorFactory {
	
	private Map<String, NlpAnnotator> nlpAnnotators = new HashMap<String, NlpAnnotator>();

	public synchronized NlpAnnotator getNlpAnnotator(String languageCode) {
		if (!nlpAnnotators.containsKey(languageCode)) {
			NlpAnnotator nlpAnnotator = new StanfordNlpAnnotator(languageCode);
			nlpAnnotators.put(languageCode, nlpAnnotator);
		}
		return nlpAnnotators.get(languageCode);
	}
}

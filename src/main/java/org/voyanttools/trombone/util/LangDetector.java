/**
 * 
 */
package org.voyanttools.trombone.util;

import java.io.IOException;
import java.util.List;

import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;

/**
 * @author sgs
 *
 */
public class LangDetector {
	
	private List<LanguageProfile> languageProfiles;

	private LanguageDetector languageDetector;

	private TextObjectFactory textObjectFactory;
	
	public static LangDetector langDetector = new LangDetector();
	

	/**
	 * 
	 */
	public LangDetector() {
		//load all languages:
		try {
			languageProfiles = new LanguageProfileReader().readAllBuiltIn();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		//build language detector:
		languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
		        .withProfiles(languageProfiles)
		        .build();

		//create a text object factory
		textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
	}
	
	public String detect(String text) {

		if (text==null) return "";
		
		text = text.trim();
		
		// quick and dirty tags stripper
		if (text.startsWith("<")) {
			text = text.replaceAll("<.+?>", "");
		}
				
		TextObject textObject = textObjectFactory.forText(text);
		List<DetectedLanguage> langs = languageDetector.getProbabilities(textObject);
		
		return langs.isEmpty() ? "" : langs.get(0).getLocale().getLanguage();
	}

}

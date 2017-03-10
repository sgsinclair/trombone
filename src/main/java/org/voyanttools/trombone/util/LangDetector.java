/**
 * 
 */
package org.voyanttools.trombone.util;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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
	
	private static Pattern tagStripper = Pattern.compile("<.+?>", Pattern.DOTALL);
	

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
	
	public String detect(String text, FlexibleParameters parameters) {
		return parameters.containsKey("language") ? new Locale(parameters.getParameterValue("language")).getLanguage() : detect(text);
	}
	public String detect(String text) {

		if (text==null) return "";
		
		text = text.trim();
		
		// quick and dirty tags stripper
		if (text.startsWith("<")) {
			text = tagStripper.matcher(text).replaceAll("").trim();
		}
		
		if (text.contains("\u0F0B")) { // TIBETAN MARK INTERSYLLABIC TSHEG
			return new Locale("bo").getLanguage();
		}
		TextObject textObject = textObjectFactory.forText(text);
		List<DetectedLanguage> langs = languageDetector.getProbabilities(textObject);
		
		return langs.isEmpty() ? "" : langs.get(0).getLocale().getLanguage();
	}

}

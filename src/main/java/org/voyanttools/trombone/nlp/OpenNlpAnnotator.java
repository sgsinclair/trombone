package org.voyanttools.trombone.nlp;

import java.io.IOException;
import java.util.List;

import opennlp.tools.util.Span;
import postaggersalanguage.five.POSTaggersALanguage;

public class OpenNlpAnnotator {

	private POSTaggersALanguage annotator;
	private String lang;
	
	public OpenNlpAnnotator(String lang) throws IOException {
		annotator = new POSTaggersALanguage(lang);
		this.lang = lang;
	}
	
	public PosLemmas getPosLemmas(String text, String lang) throws IOException {
		return annotator.getLemmatized(text);
		
	}
	
	public String getLang() {
		return lang;
	}
}

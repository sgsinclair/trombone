/**
 * 
 */
package org.voyanttools.trombone.util;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * @author sgs
 *
 */
public class TextUtils {

	public static List<String> getSentences(String text, Locale locale) {
		Pattern abbreviations = null;
		if (locale.equals(Locale.ENGLISH)) {
			abbreviations = Pattern.compile("\\b(Mrs?|Dr|Rev|Mr|Ms|st)\\.$", Pattern.CASE_INSENSITIVE);
		}
		List<String> sentences = new ArrayList<String>();
		Stripper stripper = new Stripper(Stripper.TYPE.ALL); // only used for text output
		text = stripper.strip(text).trim().replace("&amp;", "&");
		text = text.replaceAll("\\s+", " "); // all whitepace becomes a single space
		BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(locale);
		sentenceIterator.setText(text);
		int start = sentenceIterator.first();
		StringBuffer sb = new StringBuffer();
		for (int end = sentenceIterator.next(); end != sentenceIterator.DONE; start = end, end = sentenceIterator
				.next()) {
			sb.append(text.substring(start, end).trim());
			String sentence = sb.toString();
			if (abbreviations==null || abbreviations.matcher(sentence).find() == false) {
				if (sentence.contains(" ")) {
					sentences.add(sentence);
				}
				sb.setLength(0); // reset buffer
			} else {
				sb.append(" ");
			}
		}
		return sentences;
	}

	public static List<String> getSentences(String text, String language) {
		return getSentences(text, new Locale(language));
	}

	public static List<String> getSentences(String text) {
		return getSentences(text, Locale.ENGLISH);
	}
	
	public static String getLanguageCode(String text) {
		return Locale.ENGLISH.getLanguage();
	}

	public static void main(String[] args) {
		System.out.println(Locale.ENGLISH.equals(new Locale("en")));
	}
}

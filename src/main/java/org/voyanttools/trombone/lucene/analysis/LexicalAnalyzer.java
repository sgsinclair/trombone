/*******************************************************************************
 * Trombone is a flexible text processing and analysis library used
 * primarily by Voyant Tools (voyant-tools.org).
 * 
 * Copyright (©) 2007-2012 Stéfan Sinclair & Geoffrey Rockwell
 * 
 * This file is part of Trombone.
 * 
 * Trombone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.lucene.analysis;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cn.smart.HMMChineseTokenizer;
import org.apache.lucene.analysis.core.LetterTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.UnicodeWhitespaceTokenizer;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer;
import org.apache.tika.io.IOUtils;
import org.voyanttools.trombone.lucene.analysis.el.GreekCustomFilter;
import org.voyanttools.trombone.lucene.analysis.icu.TromboneICUTokenizerConfig;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.util.FlexibleParameters;


/**
 * @author sgs
 *
 */
public class LexicalAnalyzer extends Analyzer {
	
	protected FlexibleParameters parameters = new FlexibleParameters();
	protected String lang = "";
	
	@Override
	protected Reader initReader(String fieldName, Reader reader) {

		if (fieldName.equals(TokenType.lexical.name())) {
			reader = initReader(reader);
		}
		else {
			parameters.clear();
		}

		try {
			return new HTMLCharFilter(reader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected Reader initReader(Reader reader) {
		
		/* since there doesn't seem to be a way of passing parameters to the
		 * analyzer that's content-aware and per-field, we can add some 
		 * instructions to the end of the reader (this is done by
		 * {@link LuceneIndexer}). At this end we're especially interested
		 * in determining the language and if a parameter was set to use
		 * a simple word-boundary tokenizer (for some Asian languages
		 * the tokenizer is too aggressive and we want to allow the user
		 * to do segmentation. */

		String text;
		try {
			text = IOUtils.toString(reader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		if (text.endsWith("-->") && text.contains("<!--")) {
			int start = text.lastIndexOf("<!--");
			parameters = getParameters(text.substring(start+4, text.length()-3));
			if (parameters.containsKey("language")) {
				lang = parameters.getParameterValue("language");
			}
			text  = text.substring(0, start);
		}
		else {
			parameters.clear();
		}
		return new StringReader(text);
		
	}
	
	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		if (fieldName.equals(TokenType.lexical.name()) && parameters.getParameterValue("tokenization", "").equals("wordBoundaries")) {
			Tokenizer tokenizer = new LetterTokenizer();
			TokenStream stream = new LowerCaseFilter(tokenizer);
			return new TokenStreamComponents(tokenizer, stream);
		}
		else if (fieldName.equals(TokenType.lexical.name()) && parameters.getParameterValue("tokenization", "").equals("whitespace")) {
			Tokenizer tokenizer = new UnicodeWhitespaceTokenizer();
			return new TokenStreamComponents(tokenizer);
		}
		else if (lang.startsWith("zh") && fieldName.equals(TokenType.lexical.name())) { // Chinese
			Tokenizer tokenizer = new HMMChineseTokenizer();
			return new TokenStreamComponents(tokenizer, tokenizer);
		}
		else if (lang.equals("bo") && fieldName.equals(TokenType.lexical.name())) { // Tibetan
			Tokenizer tokenizer = new ICUTokenizer(new TromboneICUTokenizerConfig(true, true, lang));
			TokenStream stream = new LowerCaseFilter(tokenizer);
			return new TokenStreamComponents(tokenizer, stream);
		}
		else if (lang.equals("grc") /* Ancient Greek */ || lang.equals("el") /* Modern Greek */) {
			Tokenizer tokenizer = new ICUTokenizer();
			TokenStream stream = new GreekCustomFilter(tokenizer);
			return new TokenStreamComponents(tokenizer, stream);
		}
		else { // default case
			Tokenizer tokenizer = new ICUTokenizer();
			TokenStream stream = new LowerCaseFilter(tokenizer);
			return new TokenStreamComponents(tokenizer, stream);
		}
	}
	
	private FlexibleParameters getParameters(String query) {
		FlexibleParameters parameters = new FlexibleParameters();
		String[] pairs = query.trim().split("&");
		try {
		    for (String pair : pairs) {
		        int idx = pair.indexOf("=");
		        parameters.addParameter(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
		    }
		}
		catch (UnsupportedEncodingException e) { // should never happen
			throw new RuntimeException(e);
		}
	    return parameters;
	}

}

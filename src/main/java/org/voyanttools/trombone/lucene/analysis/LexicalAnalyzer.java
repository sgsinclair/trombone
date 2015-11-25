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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cn.smart.HMMChineseTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer;
import org.apache.tika.io.IOUtils;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.util.LangDetector;


/**
 * @author sgs
 *
 */
public class LexicalAnalyzer extends Analyzer {
	
	private String lang = "";
	
	@Override
	protected Reader initReader(String fieldName, Reader reader) {
		
		/* we're going to try to determine the language of the text so that we can adapt the components */
		
		// quick and dirty strip tags
		String text;
		try {
			text = IOUtils.toString(reader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		String strippedText = text.replaceAll("<.+?>", "").trim();
		
		lang = LangDetector.langDetector.detect(strippedText);
		
		try {
			return new HTMLCharFilter(new StringReader(text));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		if (lang.startsWith("zzh") && fieldName.equals(TokenType.lexical.name())) {
			Tokenizer tokenizer = new HMMChineseTokenizer();
			return new TokenStreamComponents(tokenizer, tokenizer);
		}
		else {
			Tokenizer tokenizer = new ICUTokenizer();
			TokenStream stream = new LowerCaseFilter(tokenizer);
			return new TokenStreamComponents(tokenizer, stream);
		}
	}

}

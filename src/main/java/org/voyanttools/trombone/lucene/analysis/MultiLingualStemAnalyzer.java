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

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.snowball.SnowballFilter;

/**
 * @author sgs
 *
 */
public class MultiLingualStemAnalyzer extends LexicalAnalyzer {

	private StemmableLanguage sl;
	
	public MultiLingualStemAnalyzer(String lang) {
		sl = lang.length()==2 ? StemmableLanguage.fromCode(lang) : StemmableLanguage.valueOf(lang);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.lucene.analysis.Analyzer#createComponents(java.lang.String, java.io.Reader)
	 */
	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		TokenStreamComponents tsc = super.createComponents(fieldName);
		return new TokenStreamComponents(tsc.getTokenizer(), new SnowballFilter(tsc.getTokenStream(), StringUtils.capitalize(sl.name().toLowerCase())));
	}

}

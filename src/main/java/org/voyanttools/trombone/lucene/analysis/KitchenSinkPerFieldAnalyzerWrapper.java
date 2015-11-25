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

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.core.KeywordAnalyzer;

/**
 * @author sgs
 *
 */
public class KitchenSinkPerFieldAnalyzerWrapper extends AnalyzerWrapper {

	private static Analyzer keywordAnalyzer = new KeywordAnalyzer();
	private enum AnalyzerName {
		ID("id") {
			@Override
			Analyzer getAnalyzer() {return keywordAnalyzer;}
		},
		VERSION("version") {
			@Override
			Analyzer getAnalyzer() {return keywordAnalyzer;}
		},
		STEMMED_EN("stemmed-en") {
			@Override
			Analyzer getAnalyzer() {return new MultiLingualStemAnalyzer("en");}
		},
		// TODO: re-enable lemmatization
//		LEMMATIZED_EN("lemmatized-en") {
//			@Override
//			Analyzer getAnalyzer() {return new EnglishMorphologicalAnalyzer();}
//		},
		LEXICAL("lexical");
//		MORPH_EN("morph-en");
		
		private String name;
		AnalyzerName(String name) {
			this.name = name;
		}
		
		Analyzer getAnalyzer() {
			return new LexicalAnalyzer();
		}
		
		public static AnalyzerName getName(String name) {
			for (AnalyzerName n : values()) {
				if (n.name.equals(name)) {return n;}
			}
			return null;
		}
		
	}
	  private final Analyzer defaultAnalyzer;
	  private final Map<AnalyzerName, Analyzer> fieldAnalyzers;
	  
	  
	  public KitchenSinkPerFieldAnalyzerWrapper() {
		  super(Analyzer.PER_FIELD_REUSE_STRATEGY);
		  this.defaultAnalyzer = new LexicalAnalyzer();
		  this.fieldAnalyzers = new HashMap<AnalyzerName, Analyzer>();
		  for (AnalyzerName name : AnalyzerName.values()) {
			  this.fieldAnalyzers.put(name, null);
		  }
	  }

	  @Override
	  protected Analyzer getWrappedAnalyzer(String fieldName) {
		  AnalyzerName name = AnalyzerName.getName(fieldName);
		  if (name==null) return defaultAnalyzer;
		  
		  Analyzer analyzer = this.fieldAnalyzers.get(name);
		  if (analyzer==null) {
			  analyzer = name.getAnalyzer();
			  this.fieldAnalyzers.put(name, analyzer);
		  }
		  
		  return analyzer;
	  }

	  @Override
	  protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
	    return components;
	  }
	  
	  @Override
	  public String toString() {
	    return "KitchenSinkPerFieldAnalyzerWrapper(" + fieldAnalyzers + ", default=" + defaultAnalyzer + ")";
	  }
	  
}

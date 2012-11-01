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
package org.voyanttools.trombone.tool.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.storage.Storage;

/**
 * @author sgs
 *
 */
public class CorpusTermFrequenciesAnalyzer {
	
	private Storage storage;

	/**
	 * 
	 */
	public CorpusTermFrequenciesAnalyzer(Storage storage) {
		this.storage = storage;
	}

	/*
	private Collection<DistributedTermFrequencies> getAllCorpusTermFrequencies(StoredToLuceneDocumentsMapper corpusToLuceneDocumentMapper, LuceneHelper.Field field) throws IOException {
		Map<String, DistributedTermFrequencies> termFrequenciesMap = new HashMap<String, DistributedTermFrequencies>();
		TermEnum termEnum = ireader.terms(new Term(field.name()));
		String fieldString = field.name();
		while (termEnum.next()) {
			String currentField = termEnum.term().field();
			if (currentField.equals(fieldString)) {
				String text = termEnum.term().text();
				int[] freqs = getTermTermFrequencies(ireader, corpusToLuceneDocumentMapper, termEnum.term());
				for (int i : freqs) { // loop through to make sure the array has a positive value
					if (i>0) {
						termFrequenciesMap.put(text, new DistributedTermFrequencies(text, freqs));
						break;
					}
				}
			}
			else {break;} // we're beyond the current field
		}
		return termFrequenciesMap.values();
	}
	*/

}

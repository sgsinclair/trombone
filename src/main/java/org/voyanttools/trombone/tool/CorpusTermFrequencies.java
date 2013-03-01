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
package org.voyanttools.trombone.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.corpus.CorpusTermFrequencyStats;
import org.voyanttools.trombone.tool.analysis.corpus.CorpusTermFrequencyStatsSort;
import org.voyanttools.trombone.tool.analysis.corpus.CorpusTermFrequencyStatsQueue;
import org.voyanttools.trombone.tool.analysis.document.DocumentTermFrequencyStats;
import org.voyanttools.trombone.tool.analysis.document.DocumentTermFrequencyStatsQueue;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public class CorpusTermFrequencies extends AbstractTermFrequencies {
	
	private List<CorpusTermFrequencyStats> corpusTerms = new ArrayList<CorpusTermFrequencyStats>();
	
	@XStreamOmitField
	private CorpusTermFrequencyStatsSort corpusTermFrequencyStatsSort;
	
	private boolean includeDocumentDistribution = false;

	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusTermFrequencies(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		corpusTermFrequencyStatsSort = CorpusTermFrequencyStatsSort.rawFrequencyDesc;
	}

	protected void runAllTerms(Corpus corpus, StoredToLuceneDocumentsMapper corpusMapper) throws IOException {
		
		int size = start+limit;
		
		Bits docIdSet = corpusMapper.getDocIdOpenBitSet();
		
		AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getIndexReader());
		
		// now we look for our term frequencies
		Terms terms = atomicReader.terms(tokenType.name());
		TermsEnum termsEnum = terms.iterator(null);
		DocsEnum docsEnum = null;
		CorpusTermFrequencyStatsQueue queue = new CorpusTermFrequencyStatsQueue(size, corpusTermFrequencyStatsSort);
		String termString;
		while(true) {
			
			BytesRef term = termsEnum.next();
			
			if (term != null) {
				termString = term.utf8ToString();
				docsEnum = termsEnum.docs(docIdSet, docsEnum, DocsEnum.FLAG_FREQS);
				int doc = docsEnum.nextDoc();
				int termFreq = 0;
				int[] documentFreqs = new int[corpus.size()];
				while(doc!=DocsEnum.NO_MORE_DOCS) {
					int freq = docsEnum.freq();
					termFreq += freq;
					int documentPosition = corpusMapper.getDocumentPositionFromLuceneDocumentIndex(doc);
					documentFreqs[documentPosition] = freq;
					doc = docsEnum.nextDoc();
				}
				queue.insertWithOverflow(new CorpusTermFrequencyStats(termString, termFreq, includeDocumentDistribution ? documentFreqs : null));
			}
			else {
				break; // no more terms
			}
		}
		seCorpusTermsFromQueue(queue);
	}

	private void seCorpusTermsFromQueue(CorpusTermFrequencyStatsQueue queue) {
		for (int i=0, len = queue.size()-start; i<len; i++) {
			corpusTerms.add(queue.pop());
		}
		Collections.reverse(corpusTerms);
	}

	@Override
	protected void runQueries(Corpus corpus,
			StoredToLuceneDocumentsMapper corpusMapper) throws IOException {
		// FIXME
	}

	List<CorpusTermFrequencyStats> getCorpusTermFrequencyStats() {
		return corpusTerms;
	}

}

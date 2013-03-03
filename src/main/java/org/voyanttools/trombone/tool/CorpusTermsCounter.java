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
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.DocumentTerm;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.corpus.CorpusTermsSort;
import org.voyanttools.trombone.tool.analysis.corpus.CorpusTermsQueue;
import org.voyanttools.trombone.tool.analysis.document.DocumentTermsQueue;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public class CorpusTermsCounter extends AbstractTermsCounter {
	
	private List<CorpusTerm> corpusTerms = new ArrayList<CorpusTerm>();
	
	@XStreamOmitField
	private CorpusTermsSort corpusTermFrequencyStatsSort;
	
	@XStreamOmitField
	private boolean includeDocumentDistribution = false;

	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusTermsCounter(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		corpusTermFrequencyStatsSort = CorpusTermsSort.rawFrequencyDesc;
	}

	protected void runAllTerms(Corpus corpus, StoredToLuceneDocumentsMapper corpusMapper) throws IOException {
		Keywords stopwords = getStopwords();
		
		int size = start+limit;
		
		Bits docIdSet = corpusMapper.getDocIdOpenBitSet();
		
		AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getIndexReader());
		
		// now we look for our term frequencies
		Terms terms = atomicReader.terms(tokenType.name());
		TermsEnum termsEnum = terms.iterator(null);
		DocsEnum docsEnum = null;
		CorpusTermsQueue queue = new CorpusTermsQueue(size, corpusTermFrequencyStatsSort);
		String termString;
		while(true) {
			
			BytesRef term = termsEnum.next();
			
			if (term != null) {
				termString = term.utf8ToString();
				total++;
				if (stopwords.isKeyword(termString)) {continue;}
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
				queue.offer(new CorpusTerm(termString, termFreq, includeDocumentDistribution ? documentFreqs : null));
			}
			else {
				break; // no more terms
			}
		}
		seCorpusTermsFromQueue(queue);
	}

	private void seCorpusTermsFromQueue(CorpusTermsQueue queue) {
		for (int i=0, len = queue.size()-start; i<len; i++) {
			corpusTerms.add(queue.poll());
		}
		Collections.reverse(corpusTerms);
	}

	@Override
	protected void runQueries(Corpus corpus,
			StoredToLuceneDocumentsMapper corpusMapper) throws IOException {
		// FIXME
	}

	List<CorpusTerm> getCorpusTermFrequencyStats() {
		return corpusTerms;
	}

}

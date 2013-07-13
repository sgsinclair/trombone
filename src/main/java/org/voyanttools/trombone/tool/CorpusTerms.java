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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.CorpusTermsQueue;
import org.voyanttools.trombone.tool.analysis.SpanQueryParser;
import org.voyanttools.trombone.tool.utils.AbstractTerms;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
@XStreamAlias("corpusTerms")
public class CorpusTerms extends AbstractTerms {
	
	private List<CorpusTerm> terms = new ArrayList<CorpusTerm>();
	
	@XStreamOmitField
	private CorpusTerm.Sort corpusTermSort;
	
	@XStreamOmitField
	private boolean includeDistribution = false;

	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusTerms(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		includeDistribution = parameters.getParameterBooleanValue("includeDocumentDistribution");
		corpusTermSort = CorpusTerm.Sort.rawFrequencyDesc;
	}

	protected void runAllTerms(Corpus corpus, StoredToLuceneDocumentsMapper corpusMapper) throws IOException {
		
		Keywords stopwords = getStopwords(corpus);
		
		int size = start+limit;
		
		Bits docIdSet = corpusMapper.getDocIdOpenBitSet();
		
		AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getIndexReader());
		
		// now we look for our term frequencies
		Terms terms = atomicReader.terms(tokenType.name());
		TermsEnum termsEnum = terms.iterator(null);
		DocsEnum docsEnum = null;
		CorpusTermsQueue queue = new CorpusTermsQueue(size, corpusTermSort);
		String termString;
		while(true) {
			
			BytesRef term = termsEnum.next();
			
			if (term != null) {
				termString = term.utf8ToString();
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
				if (termFreq>0) {
					total++;
					queue.offer(new CorpusTerm(termString, termFreq, includeDistribution ? documentFreqs : null));
				}
			}
			else {
				break; // no more terms
			}
		}
		setTermsFromQueue(queue);
	}

	private void setTermsFromQueue(CorpusTermsQueue queue) {
		for (int i=0, len = queue.size()-start; i<len; i++) {
			terms.add(queue.poll());
		}
		Collections.reverse(terms);
	}

	@Override
	protected void runQueries(Corpus corpus,
			StoredToLuceneDocumentsMapper corpusMapper, String[] queries) throws IOException {
		SpanQueryParser spanQueryParser = new SpanQueryParser(storage.getLuceneManager().getAnalyzer());
		AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getIndexReader());
		Map<String, SpanQuery> spanQueries = spanQueryParser.getSpanQueries(atomicReader, queries, tokenType, isQueryCollapse);
		Map<Term, TermContext> termContexts = new HashMap<Term, TermContext>();
		Map<Integer, AtomicInteger> positionsMap = new HashMap<Integer, AtomicInteger>();
		int size = start+limit;
		CorpusTermsQueue queue = new CorpusTermsQueue(size, corpusTermSort);
		int lastDoc = -1;
		int docIndexInCorpus = -1; // this should always be changed on the first span
		for (Map.Entry<String, SpanQuery> spanQueryEntry : spanQueries.entrySet()) {
			String queryString = spanQueryEntry.getKey();
			Spans spans = spanQueryEntry.getValue().getSpans(atomicReader.getContext(), corpusMapper.getDocIdOpenBitSet(), termContexts);			
			while(spans.next()) {
				int doc = spans.doc();
				if (doc != lastDoc) {
					docIndexInCorpus = corpusMapper.getDocumentPositionFromLuceneDocumentIndex(doc);
					lastDoc = doc;
				}
				if (positionsMap.containsKey(docIndexInCorpus)==false) {
					positionsMap.put(docIndexInCorpus, new AtomicInteger(1));
				}
				else {
					positionsMap.get(docIndexInCorpus).incrementAndGet();
				}
			}
			int freqs[] = new int[corpus.size()];
			int freq = 0;
			for (Map.Entry<Integer, AtomicInteger> entry : positionsMap.entrySet()) {
				int f = entry.getValue().intValue();
				freq+=f;
				freqs[entry.getKey()] = f;
			}
			total++;
			queue.offer(new CorpusTerm(queryString, freq, includeDistribution ? freqs : null));
			positionsMap.clear(); // prepare for new entries
		}
		setTermsFromQueue(queue);	}

	List<CorpusTerm> getCorpusTerms() {
		return terms;
	}

}

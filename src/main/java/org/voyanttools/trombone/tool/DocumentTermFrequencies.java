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
import java.util.PriorityQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.SpanQueryParser;
import org.voyanttools.trombone.tool.analysis.document.DocumentTermFrequencyStats;
import org.voyanttools.trombone.tool.analysis.document.DocumentTermFrequencyStatsQueue;
import org.voyanttools.trombone.tool.analysis.document.DocumentTermFrequencyStatsSort;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public class DocumentTermFrequencies extends AbstractTermFrequencies {
	

	private List<DocumentTermFrequencyStats> documentTerms = new ArrayList<DocumentTermFrequencyStats>();
	
	@XStreamOmitField
	private DocumentTermFrequencyStatsSort documentTermFrequencyStatsSort;
	
	@XStreamOmitField
	boolean isNeedsPositions;
	
	@XStreamOmitField
	boolean isNeedsOffsets;

	
	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentTermFrequencies(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		documentTermFrequencyStatsSort = DocumentTermFrequencyStatsSort.relativeFrequencyDesc;
		isNeedsPositions = parameters.getParameterBooleanValue("includeTokenIndexPositions");
		isNeedsOffsets = parameters.getParameterBooleanValue("includeTokenCharacterOffsets");
	}

	
	protected void runQueries(Corpus corpus, StoredToLuceneDocumentsMapper corpusMapper) throws IOException {
		SpanQueryParser spanQueryParser = new SpanQueryParser(storage.getLuceneManager().getAnalyzer());
		AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getIndexReader());
		Map<String, SpanQuery> spanQueries = spanQueryParser.getSpanQueries(atomicReader, parameters.getParameterValues("query"), tokenType, isQueryCollapse);
		Map<Term, TermContext> termContexts = new HashMap<Term, TermContext>();
		Map<Integer, List<Integer>> positionsMap = new HashMap<Integer, List<Integer>>();
		int size = start+limit;
		DocumentTermFrequencyStatsQueue queue = new DocumentTermFrequencyStatsQueue(size, documentTermFrequencyStatsSort);
		int[] totalTokenCounts = corpus.getTotalTokensCounts(tokenType);
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
				int start = spans.start();
				if (positionsMap.containsKey(docIndexInCorpus)==false) {
					positionsMap.put(docIndexInCorpus, new ArrayList<Integer>());
				}
				positionsMap.get(docIndexInCorpus).add(start);
			}
			for (Map.Entry<Integer, List<Integer>> entry : positionsMap.entrySet()) {
				List<Integer> positionsList = entry.getValue();
				int freq = positionsList.size();
				int[] positions = new int[positionsList.size()];
				for (int i=0; i<positions.length; i++) {
					positions[i] = positionsList.get(i);
				}
				int documentPosition = entry.getKey();
				float rel = (float) freq / totalTokenCounts[documentPosition];
				total++;
				queue.insertWithOverflow(new DocumentTermFrequencyStats(documentPosition, queryString, freq, rel, isNeedsPositions ? positions : null, null));
			}
			positionsMap.clear(); // prepare for new entries
		}
		setDocumentTermsFromQueue(queue);
	}

	protected void runAllTerms(Corpus corpus, StoredToLuceneDocumentsMapper corpusMapper) throws IOException {
		
		int size = start+limit;
		
		int[] totalTokensCounts = corpus.getTotalTokensCounts(tokenType);
		Bits docIdSet = corpusMapper.getDocIdOpenBitSet();
		
		AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getIndexReader());
		
		// now we look for our term frequencies
		Terms terms = atomicReader.terms(tokenType.name());
		TermsEnum termsEnum = terms.iterator(null);
		DocsAndPositionsEnum docsAndPositionsEnum = null;
		DocumentTermFrequencyStatsQueue queue = new DocumentTermFrequencyStatsQueue(size, documentTermFrequencyStatsSort);
		String termString;
		while(true) {
			
			BytesRef term = termsEnum.next();
			
			if (term != null) {
				termString = term.utf8ToString();
				total+=termsEnum.docFreq();
				// FIXME: check for stopwords
				docsAndPositionsEnum = termsEnum.docsAndPositions(docIdSet, docsAndPositionsEnum, DocsAndPositionsEnum.FLAG_OFFSETS);
				int doc = docsAndPositionsEnum.nextDoc();
				while (doc != DocIdSetIterator.NO_MORE_DOCS) {
					int documentPosition = corpusMapper.getDocumentPositionFromLuceneDocumentIndex(doc);
					int totalTokensCount = totalTokensCounts[documentPosition];
					int freq = docsAndPositionsEnum.freq();
					float rel = (float) freq / totalTokensCount;
					if (freq>0) {total++;} // make sure we track that this could be a hit
					int[] positions = null;
					int[] offsets = null;
					if (isNeedsPositions || isNeedsOffsets) {
						positions = new int[freq];
						offsets = new int[freq];
						for (int i=0; i<freq; i++) {
							positions[i] = docsAndPositionsEnum.nextPosition();
							offsets[i] = docsAndPositionsEnum.startOffset();
						}
					}					
					queue.insertWithOverflow(new DocumentTermFrequencyStats(documentPosition, termString, freq, rel, positions, offsets));
					doc = docsAndPositionsEnum.nextDoc();
				}
			}
			else {
				break; // no more terms
			}
		}
		setDocumentTermsFromQueue(queue);
	}

	private void setDocumentTermsFromQueue(DocumentTermFrequencyStatsQueue queue) {
		for (int i=0, len = queue.size()-start; i<len; i++) {
			documentTerms.add(queue.pop());
		}
		Collections.reverse(documentTerms);
	}

	public List<DocumentTermFrequencyStats> getDocumentTermFrequencyStats() {
		return documentTerms;
	}

}

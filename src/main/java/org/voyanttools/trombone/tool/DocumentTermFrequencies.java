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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentTermFrequencyStats;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.SpanQueryParser;
import org.voyanttools.trombone.tool.analysis.document.DocumentTermFrequencyStatsQueue;
import org.voyanttools.trombone.tool.analysis.document.DocumentTermFrequencyStatsSort;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public class DocumentTermFrequencies extends AbstractTool {
	
	private int total = 0;

	private List<DocumentTermFrequencyStats> documentTerms = new ArrayList<DocumentTermFrequencyStats>();
	
	@XStreamOmitField
	private int start;
	
	@XStreamOmitField
	private int limit;
	
	@XStreamOmitField
	private DocumentTermFrequencyStatsSort documentTermFrequencyStatsSort;
	
	@XStreamOmitField
	private TokenType tokenType;
	
	@XStreamOmitField
	boolean isNeedsPositions;
	
	@XStreamOmitField
	boolean isNeedsOffsets;

	@XStreamOmitField
	boolean isQueryCollapse;
	
	
	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentTermFrequencies(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		start = parameters.getParameterIntValue("start", 0);
		limit = parameters.getParameterIntValue("start", 50);
		documentTermFrequencyStatsSort = DocumentTermFrequencyStatsSort.relativeFrequencyDesc;
		tokenType = TokenType.getTokenTypeForgivingly(parameters.getParameterValue("tokenType", "lexical"));
		isNeedsPositions = parameters.getParameterBooleanValue("includeTokenIndexPositions");
		isNeedsOffsets = parameters.getParameterBooleanValue("includeTokenCharacterOffsets");
		isQueryCollapse = parameters.getParameterBooleanValue("queryCollapse");
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.RunnableTool#run()
	 */
	@Override
	public void run() throws IOException {
		if (parameters.containsKey("query")) {
			SpanQueryParser spanQueryParser = new SpanQueryParser();
			AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getIndexReader());
//			SpanQuery spanQuery = spanQueryParser.getSpanQuery(atomicReader, parameters.getParameterValues("query"), tokenType, isQueryCollapse);
			//spanQuery.getSpans(context, acceptDocs, termContexts)
		}
		else {
			runAllTerms();
		}
	}
	
	private void runAllTerms() throws IOException {
		Corpus corpus = storage.getCorpusStorage().getCorpus(parameters.getParameterValue("corpus"));
		runAllTerms(corpus);
	}
	private void runAllTerms(Corpus corpus) throws IOException {
		List<String> ids = this.getCorpusStoredDocumentIdsFromParameters(corpus);
		StoredToLuceneDocumentsMapper mapper = new StoredToLuceneDocumentsMapper(storage, ids);
		runAllTerms(corpus, mapper);
	}
	
	private void runAllTerms(Corpus corpus, StoredToLuceneDocumentsMapper mapper) throws IOException {
		
		int size = start+limit;
		
		int[] totalTokensCounts = corpus.getTotalTokensCounts(tokenType);
		Bits docIdSet = mapper.getDocIdOpenBitSet();
		
		AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getIndexReader());
		
		// first we map our filtered list of lucene document numbers to the corpus id
		Terms idTerms = atomicReader.terms("id");
		TermsEnum idTermsEnum = idTerms.iterator(null);
		DocsEnum idDocsEnum = null;
		Map<Integer, String> filteredDocsMap = new HashMap<Integer, String>();
		while(true) {
			BytesRef term = idTermsEnum.next();
			if (term != null) {
				idDocsEnum = idTermsEnum.docs(docIdSet, idDocsEnum);
				while (true) {
					int docId = idDocsEnum.nextDoc();
					if (docId!=DocsEnum.NO_MORE_DOCS) {
						filteredDocsMap.put(docId, term.utf8ToString());
					}
					else {break;}
				}
			}
			else {break;}
		}
		
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

				docsAndPositionsEnum = termsEnum.docsAndPositions(docIdSet, docsAndPositionsEnum, DocsAndPositionsEnum.FLAG_OFFSETS);
				int doc = docsAndPositionsEnum.nextDoc();
				while (doc != DocIdSetIterator.NO_MORE_DOCS) {
					
					String corpusId = filteredDocsMap.get(doc);
					int documentPosition = corpus.getDocumentPosition(corpusId);
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
		for (int i=0; i<queue.size(); i++) {
			documentTerms.add(queue.pop());
		}
		Collections.reverse(documentTerms);
	}

}

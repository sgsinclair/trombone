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
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.vectorhighlight.FieldTermStack.TermInfo;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentCollocate;
import org.voyanttools.trombone.model.Kwic;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.DocumentCollocatesQueue;
import org.voyanttools.trombone.tool.analysis.KwicsQueue;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class DocumentCollocates extends AbstractContextTerms {

	private List<DocumentCollocate> collocates = new ArrayList<DocumentCollocate>();
		
	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentCollocates(Storage storage,
			FlexibleParameters parameters) {
		super(storage, parameters);
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.utils.AbstractTerms#runQueries(org.voyanttools.trombone.model.Corpus, org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper, java.lang.String[])
	 */
	@Override
	protected void runQueries(Corpus corpus, String[] queries) throws IOException {
		this.queries = queries; // FIXME: this should be set by superclass
		AtomicReader reader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getDirectoryReader());
		this.collocates = getCollocates(reader, getStoredToLuceneDocumentsMapper(corpus), corpus);
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.utils.AbstractTerms#runAllTerms(org.voyanttools.trombone.model.Corpus, org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper)
	 */
	@Override
	protected void runAllTerms(Corpus corpus) throws IOException {
		AtomicReader reader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getDirectoryReader());
		this.collocates = getCollocates(reader, getStoredToLuceneDocumentsMapper(corpus), corpus);
	}

	private List<DocumentCollocate> getCollocates(AtomicReader reader,
			StoredToLuceneDocumentsMapper corpusMapper, Corpus corpus) throws IOException {
		Map<Integer, Collection<DocumentSpansData>> documentSpansDataMap = getDocumentSpansData(reader, corpusMapper, queries);
		return getCollocates(reader, corpusMapper, corpus, documentSpansDataMap);
	}

	private List<DocumentCollocate> getCollocates(AtomicReader reader,
			StoredToLuceneDocumentsMapper corpusMapper, Corpus corpus,
			Map<Integer, Collection<DocumentSpansData>> documentSpansDataMap) throws IOException {

		int[] totalTokens = corpus.getLastTokenPositions(tokenType);
		DocumentCollocate.Sort documentCollocatesSort = DocumentCollocate.Sort.valueOfForgivingly(parameters);
		DocumentCollocatesQueue queue = new DocumentCollocatesQueue(limit, documentCollocatesSort);
		for (Map.Entry<Integer, Collection<DocumentSpansData>> dsd : documentSpansDataMap.entrySet()) {
			int luceneDoc = dsd.getKey();
			int corpusDocIndex = corpusMapper.getDocumentPositionFromLuceneDocumentIndex(luceneDoc);
			int lastToken = totalTokens[corpusDocIndex];
			DocumentCollocatesQueue q = getCollocates(reader, luceneDoc, corpusDocIndex, lastToken, dsd.getValue());
			DocumentCollocate c;
			while ((c = q.poll()) != null) {
				queue.offer(c);
			}
		}
		
		
		List<DocumentCollocate> localCollocates = new ArrayList<DocumentCollocate>();
		DocumentCollocate c;
		while ((c = queue.poll()) != null) {
			localCollocates.add(c);
		}
		Collections.reverse(localCollocates);
		return localCollocates;
	}

	private DocumentCollocatesQueue getCollocates(AtomicReader atomicReader,
			int luceneDoc, int corpusDocIndex, int lastToken,
			Collection<DocumentSpansData> documentSpansData) throws IOException {
		
		
		Map<Integer, TermInfo> termsOfInterest = getTermsOfInterest(atomicReader, luceneDoc, lastToken, documentSpansData, true);

		Map<String, Map<String, AtomicInteger>> mapOfTermsMap = new HashMap<String, Map<String, AtomicInteger>>();
		
		// this keeps track of the terms we want to lookup total document frequencies
		Map<String, Integer> stringsOfInterestMap = new HashMap<String, Integer>();
		
		//		Map<String, Map<String, Integer>>
		for (DocumentSpansData dsd : documentSpansData) {

			Map<String, AtomicInteger> termsMap = new HashMap<String, AtomicInteger>();
			
			int contextTotalTokens = 0;

			for (int[] data : dsd.spansData) {
				
				int keywordstart = data[0];
				int keywordend = data[1];
				
				int leftstart = keywordstart - context;
				if (leftstart<0) {leftstart=0;}
				for (int i=leftstart; i<keywordstart-1; i++) {
					contextTotalTokens++;
					String term = termsOfInterest.get(i).getText();
					stringsOfInterestMap.put(term, 0);
					if (termsMap.containsKey(term)) {termsMap.get(term).getAndIncrement();}
					else {termsMap.put(term, new AtomicInteger(1));}
				}

				int rightend = keywordend + context;
				if (rightend>lastToken) {rightend=lastToken;}
				for (int i=keywordend+1; i<rightend+1; i++) {
					contextTotalTokens++;
					String term = termsOfInterest.get(i).getText();
					stringsOfInterestMap.put(term, 0);
					if (termsMap.containsKey(term)) {termsMap.get(term).getAndIncrement();}
					else {termsMap.put(term, new AtomicInteger(1));}
				}
			}
			
			mapOfTermsMap.put(dsd.queryString, termsMap);
		}
		
		// gather document frequency for strings of interest
		int documentTotalTokens = 0;
		
		Terms terms = atomicReader.getTermVector(luceneDoc, tokenType.name());
		TermsEnum termsEnum = terms.iterator(null);
		while(true) {
			BytesRef term = termsEnum.next();
			if (term!=null) {
				String termString = term.utf8ToString();
				DocsEnum docsEnum = termsEnum.docs(null, null, DocsEnum.FLAG_FREQS);
				docsEnum.nextDoc();
				int freq = docsEnum.freq();
				documentTotalTokens += freq;
				if (stringsOfInterestMap.containsKey(termString)) {
					stringsOfInterestMap.put(termString, freq);
				}
			}
			else {break;}
		}

		DocumentCollocatesQueue documentCollocatesQueue = new DocumentCollocatesQueue(limit, DocumentCollocate.Sort.valueOfForgivingly(parameters));
		
		for (Map.Entry<String, Map<String, AtomicInteger>> keywordMapEntry : mapOfTermsMap.entrySet()) {
			String keyword = keywordMapEntry.getKey();
			
			Map<String, AtomicInteger> termsMap = keywordMapEntry.getValue();

			// once through to determine contextTotalTokens
			int contextTotalTokens = 0;
			for (Map.Entry<String, AtomicInteger> termsMapEntry : termsMap.entrySet()) {
				contextTotalTokens += termsMapEntry.getValue().intValue();
			}
			
			// and now to create document collocate objects
			for (Map.Entry<String, AtomicInteger> termsMapEntry : termsMap.entrySet()) {
				String term = termsMapEntry.getKey();
				int documentTermRawFrequency = stringsOfInterestMap.get(term);
				int contextTermRawFrequency = termsMapEntry.getValue().intValue();
				DocumentCollocate documentCollocate = new DocumentCollocate(corpusDocIndex, keyword, term, contextTermRawFrequency, ((float) contextTermRawFrequency)/contextTotalTokens, documentTermRawFrequency, ((float) documentTermRawFrequency)/documentTotalTokens);
				documentCollocatesQueue.offer(documentCollocate);
			}
			
		}
		
		return documentCollocatesQueue;
	}

	public List<DocumentCollocate> getDocumentCollocates() {
		return collocates;
	}
}

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentCollocate;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.TermInfo;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.FlexibleQueue;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
@XStreamAlias("documentCollocates")
public class DocumentCollocates extends AbstractContextTerms {

	private List<DocumentCollocate> collocates = new ArrayList<DocumentCollocate>();
	
	@XStreamOmitField
	private DocumentCollocate.Sort sort;
	
	@XStreamOmitField
	private Comparator<DocumentCollocate> comparator;
	
	@XStreamOmitField
	private Keywords collocatesWhitelist;
		
	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentCollocates(Storage storage,
			FlexibleParameters parameters) {
		super(storage, parameters);
		sort = DocumentCollocate.Sort.valueOfForgivingly(parameters);
		comparator = DocumentCollocate.getComparator(sort);
		collocatesWhitelist = new Keywords();
		if (parameters.containsKey("collocatesWhitelist")) {
			collocatesWhitelist.add(Arrays.asList(parameters.getParameterValues("collocatesWhitelist")));
		}
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.utils.AbstractTerms#runQueries(org.voyanttools.trombone.model.Corpus, org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper, java.lang.String[])
	 */
	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		Map<Integer, List<DocumentSpansData>> documentSpansDataMap = getDocumentSpansData(corpusMapper, queries);
		this.collocates = getCollocates(corpusMapper.getLeafReader(), corpusMapper, corpusMapper.getCorpus(), documentSpansDataMap);
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.utils.AbstractTerms#runAllTerms(org.voyanttools.trombone.model.Corpus, org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper)
	 */
	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		runQueries(corpusMapper, stopwords, new String[0]); // doesn't make much sense without query
	}


	List<DocumentCollocate> getCollocates(LeafReader reader,
			CorpusMapper corpusMapper, Corpus corpus,
			Map<Integer, List<DocumentSpansData>> documentSpansDataMap) throws IOException {

		Keywords stopwords = getStopwords(corpus);
		
		List<String> idsList = this.getCorpusStoredDocumentIdsFromParameters(corpus);
		Set<String> idsHash = new HashSet<String>(idsList);
		
		int[] totalTokens = corpus.getLastTokenPositions(tokenType);
		FlexibleQueue<DocumentCollocate> queue = new FlexibleQueue<DocumentCollocate>(comparator, limit);
		for (Map.Entry<Integer, List<DocumentSpansData>> dsd : documentSpansDataMap.entrySet()) {
			int luceneDoc = dsd.getKey();
			int corpusDocIndex = corpusMapper.getDocumentPositionFromLuceneId(luceneDoc);
			String id = corpusMapper.getDocumentIdFromDocumentPosition(corpusDocIndex);
			if (idsHash.contains(id)==false) {continue;}
			int lastToken = totalTokens[corpusDocIndex];
			FlexibleQueue<DocumentCollocate> q = getCollocates(reader, luceneDoc, corpusDocIndex, lastToken, dsd.getValue(), stopwords);
			for (DocumentCollocate c : q.getUnorderedList()) {
				queue.offer(c);
			}
		}
		
		return queue.getOrderedList();
	}

	private FlexibleQueue<DocumentCollocate> getCollocates(LeafReader LeafReader,
			int luceneDoc, int corpusDocIndex, int lastToken,
			List<DocumentSpansData> documentSpansData, Keywords stopwords) throws IOException {
		
		
		Map<Integer, TermInfo> termsOfInterest = getTermsOfInterest(LeafReader, luceneDoc, lastToken, documentSpansData, true);

		Map<String, Map<String, AtomicInteger>> mapOfTermsMap = new HashMap<String, Map<String, AtomicInteger>>();
		
		Map<String, Integer> queryStringFrequencyMap = new HashMap<String, Integer>();
		
		// this keeps track of the terms we want to lookup total document frequencies
		Map<String, Integer> stringsOfInterestMap = new HashMap<String, Integer>();
		
		//		Map<String, Map<String, Integer>>
		for (DocumentSpansData dsd : documentSpansData) {

			Map<String, AtomicInteger> termsMap = new HashMap<String, AtomicInteger>();
			
			queryStringFrequencyMap.put(dsd.queryString, dsd.spansData.length);
			
			int contextTotalTokens = 0;

			for (int[] data : dsd.spansData) {
								
				int keywordstart = data[0];
				int keywordend = data[1];
				
				int leftstart = keywordstart - context;
				if (leftstart<0) {leftstart=0;}
				for (int i=leftstart; i<keywordstart-1; i++) {
					contextTotalTokens++;
					String term = termsOfInterest.get(i).getText();
					if (stopwords.isKeyword(term)) {continue;}
					if (collocatesWhitelist.isEmpty()==false && collocatesWhitelist.isKeyword(term)==false) {continue;}
					stringsOfInterestMap.put(term, 0);
					if (termsMap.containsKey(term)) {termsMap.get(term).getAndIncrement();}
					else {termsMap.put(term, new AtomicInteger(1));}
				}

				for (int i=keywordstart; i<keywordend; i++) {
					String term = termsOfInterest.get(i).getText();
					if (stopwords.isKeyword(term)) {continue;}
					if (collocatesWhitelist.isEmpty()==false && collocatesWhitelist.isKeyword(term)==false) {continue;}
					stringsOfInterestMap.put(term, 0);
				}
				
				int rightend = keywordend + context;
				if (rightend>lastToken) {rightend=lastToken;}
				for (int i=keywordend; i<rightend; i++) {
					contextTotalTokens++;
					String term = termsOfInterest.get(i).getText();
					if (stopwords.isKeyword(term)) {continue;}
					if (collocatesWhitelist.isEmpty()==false && collocatesWhitelist.isKeyword(term)==false) {continue;}
					stringsOfInterestMap.put(term, 0);
					if (termsMap.containsKey(term)) {termsMap.get(term).getAndIncrement();}
					else {termsMap.put(term, new AtomicInteger(1));}
				}
			}
			
			mapOfTermsMap.put(dsd.queryString, termsMap);
		}
		
		// gather document frequency for strings of interest
		int documentTotalTokens = 0;
		
		Terms terms = LeafReader.getTermVector(luceneDoc, tokenType.name());
		TermsEnum termsEnum = terms.iterator();
		while(true) {
			BytesRef term = termsEnum.next();
			if (term!=null) {
				String termString = term.utf8ToString();
				PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.FREQS);
				postingsEnum.nextDoc();
				int freq = postingsEnum.freq();
				documentTotalTokens += freq;
				if (stringsOfInterestMap.containsKey(termString)) {
					stringsOfInterestMap.put(termString, freq);
				}
			}
			else {break;}
		}

		FlexibleQueue<DocumentCollocate> documentCollocatesQueue = new FlexibleQueue(comparator, limit);
		
		for (Map.Entry<String, Map<String, AtomicInteger>> keywordMapEntry : mapOfTermsMap.entrySet()) {
			String keyword = keywordMapEntry.getKey();
			int keywordContextRawFrequency = queryStringFrequencyMap.get(keyword);
			
			Map<String, AtomicInteger> termsMap = keywordMapEntry.getValue();

			// once through to determine contextTotalTokens
			int contextTotalTokens = 0;
			for (Map.Entry<String, AtomicInteger> termsMapEntry : termsMap.entrySet()) {
				contextTotalTokens += termsMapEntry.getValue().intValue();
			}
			
			// and now to create document collocate objects
			for (Map.Entry<String, AtomicInteger> termsMapEntry : termsMap.entrySet()) {
				String term = termsMapEntry.getKey();
				int termDocumentRawFrequency = stringsOfInterestMap.get(term);
				int termContextRawFrequency = termsMapEntry.getValue().intValue();
				DocumentCollocate documentCollocate = new DocumentCollocate(corpusDocIndex, keyword, term, keywordContextRawFrequency, termContextRawFrequency, termDocumentRawFrequency, contextTotalTokens, documentTotalTokens);
//				DocumentCollocate documentCollocate = new DocumentCollocate(corpusDocIndex, keyword, term, contextTermRawFrequency, ((float) contextTermRawFrequency)/contextTotalTokens, documentTermRawFrequency, ((float) documentTermRawFrequency)/documentTotalTokens);
				documentCollocatesQueue.offer(documentCollocate);
			}
			
		}
		
		return documentCollocatesQueue;
	}

	public List<DocumentCollocate> getDocumentCollocates() {
		return collocates;
	}
}

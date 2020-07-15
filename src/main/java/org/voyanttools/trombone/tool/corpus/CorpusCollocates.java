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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusCollocate;
import org.voyanttools.trombone.model.DocumentCollocate;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.FlexibleQueue;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("corpusCollocates")
public class CorpusCollocates extends AbstractContextTerms {

	private List<CorpusCollocate> collocates = new ArrayList<CorpusCollocate>();
	
	@Override
	public float getVersion() {
		return super.getVersion()+2;
	}

	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusCollocates(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.utils.AbstractTerms#runQueries(org.voyanttools.trombone.model.Corpus, org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper, java.lang.String[])
	 */
	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries)
			throws IOException {
		this.queries = queries; // FIXME: this should be set by superclass
		Map<Integer, List<DocumentSpansData>> documentSpansDataMap = getDocumentSpansData(corpusMapper, queries);
		this.collocates = getCollocates(corpusMapper.getIndexReader(), corpusMapper, corpusMapper.getCorpus(), documentSpansDataMap);
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.utils.AbstractTerms#runAllTerms(org.voyanttools.trombone.model.Corpus, org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper)
	 */
	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		runQueries(corpusMapper, stopwords, new String[0]); // doesn't make much sense without a query
	}


	private List<CorpusCollocate> getCollocates(IndexReader reader,
			CorpusMapper corpusMapper, Corpus corpus,
			Map<Integer, List<DocumentSpansData>> documentSpansDataMap) throws IOException {
		
		FlexibleParameters localParameters = parameters.clone();
		localParameters.setParameter("limit", Integer.MAX_VALUE); // we need all collocates for documents in order to determine corpus collocates
		localParameters.setParameter("start", 0);
		DocumentCollocates documentCollocatesTool = new DocumentCollocates(storage, localParameters);
		List<DocumentCollocate> documentCollocates = documentCollocatesTool.getCollocates(reader, corpusMapper, corpus, documentSpansDataMap);
		Map<String, Set<DocumentCollocate>> keywordDocumentCollocatesMap = new HashMap<String, Set<DocumentCollocate>>();
		for (DocumentCollocate documentCollocate : documentCollocates) {
			String keyword = documentCollocate.getKeyword();
			if (!keywordDocumentCollocatesMap.containsKey(keyword)) {
				keywordDocumentCollocatesMap.put(keyword, new HashSet<DocumentCollocate>());
			}
			keywordDocumentCollocatesMap.get(keyword).add(documentCollocate);
		}
		
		CorpusCollocate.Sort sort = CorpusCollocate.Sort.getForgivingly(parameters);
		
		FlexibleQueue<CorpusCollocate> flexibleQueue = new FlexibleQueue<CorpusCollocate>(CorpusCollocate.getComparator(sort), start+limit);
		
		
		// now build corpus collocates
		for (Map.Entry<String, Set<DocumentCollocate>> keywordDocumentCollocaatesEntry : keywordDocumentCollocatesMap.entrySet()) {

			int keywordRawFrequency = 0;
			HashSet<Integer> seenDocumentIds = new HashSet<Integer>();
			// build map (group) for context terms
			Map<String, Set<DocumentCollocate>> contextTermDocumentCollocatesMap = new HashMap<String, Set<DocumentCollocate>>();
			for (DocumentCollocate documentCollocate : keywordDocumentCollocaatesEntry.getValue()) {
				String contextTerm = documentCollocate.getTerm();
				if (!contextTermDocumentCollocatesMap.containsKey(contextTerm)) {
					contextTermDocumentCollocatesMap.put(contextTerm, new HashSet<DocumentCollocate>());
				}
				contextTermDocumentCollocatesMap.get(contextTerm).add(documentCollocate);
				if (!seenDocumentIds.contains(documentCollocate.getDocIndex())) {
					keywordRawFrequency += documentCollocate.getKeywordContextRawFrequency();
					seenDocumentIds.add(documentCollocate.getDocIndex());
				}
			}
			
			String keyword = keywordDocumentCollocaatesEntry.getKey();
			for (Map.Entry<String, Set<DocumentCollocate>> contextTermCollocatesEntry : contextTermDocumentCollocatesMap.entrySet()) {
				int contextTermTotal = 0;
				for (DocumentCollocate documentCollocate : contextTermCollocatesEntry.getValue()) {
					contextTermTotal += documentCollocate.getContextRawFrequency();
				}
				total++;
				CorpusCollocate c = new CorpusCollocate(keyword, keywordRawFrequency, contextTermCollocatesEntry.getKey(), contextTermTotal);
				flexibleQueue.offer(c);
			}
			
		}
		
		return flexibleQueue.getOrderedList();
	}

	List<CorpusCollocate> getCorpusCollocates() {
		return collocates;
	}

}

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.TermInfo;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public abstract class AbstractContextTerms extends AbstractTerms {
	
	@XStreamOmitField
	protected int context;
	
	@XStreamOmitField
	protected String[] queries;

	@XStreamOmitField
	protected Set<Integer> positions;

	/**
	 * @param storage
	 * @param parameters
	 */
	public AbstractContextTerms(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		this.queries = parameters.getParameterValues("query");
		this.context = parameters.getParameterIntValue("context", 5);
		this.positions = new HashSet<Integer>();
		for (String i : parameters.getParameterValues("position")) {
			this.positions.add(Integer.parseInt(i));
		}
	}
	
	protected Map<Integer, List<DocumentSpansData>> getDocumentSpansData(CorpusMapper corpusMapper, String[] queries) throws IOException {
		
		Map<String, SpanQuery> queriesMap = getCategoriesAwareSpanQueryMap(corpusMapper, queries);

		Collection<DocumentSpansData> documentSpansDataList = new ArrayList<DocumentSpansData>();
		
		List<String> ids = this.getCorpusStoredDocumentIdsFromParameters(corpusMapper.getCorpus());
		BitSet bitSet = corpusMapper.getBitSetFromDocumentIds(ids);
		
//		CorpusTermsQueue queue = new CorpusTermsQueue(size, corpusTermSort);
		for (Map.Entry<String, SpanQuery> spanQueryEntry : queriesMap.entrySet()) {
			String queryString = spanQueryEntry.getKey();
			SpanQuery spanQuery = spanQueryEntry.getValue();
			Spans spans = corpusMapper.getFilteredSpans(spanQuery, bitSet);
			if (spans != null) {
				// map lucene document id to span offset information
				List<int[]> spansDocDataList = new ArrayList<int[]>();
				
				// we're going to go through all the span for all documents so that we can then
				// parallelize the searching of kwics
				int doc = spans.nextDoc();
				while (doc!=Spans.NO_MORE_DOCS) {
					int pos = spans.nextStartPosition();
					while (pos != Spans.NO_MORE_POSITIONS) {
						spansDocDataList.add(new int[]{spans.startPosition(), spans.endPosition()});
						pos = spans.nextStartPosition();
					}
					if (!spansDocDataList.isEmpty()) {
						int[][] data = new int[spansDocDataList.size()][2];
						for (int i=0, len=data.length; i<len; i++) {
							data[i] = spansDocDataList.get(i);
						}
						documentSpansDataList.add(new DocumentSpansData(doc, data, queryString));
						spansDocDataList.clear();
//						total++;
					}
					doc = spans.nextDoc();
				}
			}
		}
		
		
		// build a map to organize by document for efficiency
		Map<Integer, List<DocumentSpansData>> documentSpansDataMap = new HashMap<Integer, List<DocumentSpansData>>();
		for (DocumentSpansData dsd : documentSpansDataList) {
			if (!documentSpansDataMap.containsKey(dsd.luceneDoc)) {
				documentSpansDataMap.put(dsd.luceneDoc, new ArrayList<DocumentSpansData>());
			}
			documentSpansDataMap.get(dsd.luceneDoc).add(dsd);
		}
		
		return documentSpansDataMap;
	}

	protected Map<Integer, TermInfo> getTermsOfInterest(IndexReader indexReader, int luceneDoc, int lastToken, List<DocumentSpansData> documentSpansData, boolean fill) throws IOException	{
		Map<Integer, TermInfo> termsOfInterest = getTermsOfInterest(documentSpansData, lastToken, fill);
		fillTermsOfInterest(indexReader, luceneDoc, termsOfInterest);
		return termsOfInterest;
	}
	
	private Map<Integer, TermInfo> getTermsOfInterest(List<DocumentSpansData> documentSpansData, int lastToken, boolean fill)	{
		// construct a set of terms of interest
		Map<Integer, TermInfo> termsOfInterest = new HashMap<Integer, TermInfo>();
		for (DocumentSpansData dsd : documentSpansData) {
			for (int[] data : dsd.spansData) {
				int keywordstart = data[0];
				int keywordend = data[1];
				// add keywords
				for (int i=keywordstart; i<keywordend; i++) {
					termsOfInterest.put(i, null);
				}
				
				// add left
				int leftstart = keywordstart - context;
				if (leftstart<0) {leftstart = 0;}
				for (int i = leftstart, len = fill ? keywordstart : leftstart+1; i<len; i++) {
					termsOfInterest.put(i, null);					
				}
				
				// add right
				int rightend = keywordend-1 + context;
				if (rightend>lastToken) {rightend=lastToken;}
				for (int i = fill ? keywordend : rightend, len = rightend+1; i < len; i++) {
					termsOfInterest.put(i, null);
				}
			}
		}
		return termsOfInterest;
	}
		
	private void fillTermsOfInterest(IndexReader indexReader, int luceneDoc, Map<Integer, TermInfo> termsOfInterest) throws IOException {
		// fill in terms of interest
		Terms terms = indexReader.getTermVector(luceneDoc, tokenType.name());
		TermsEnum termsEnum = terms.iterator();
		while(true) {
			BytesRef term = termsEnum.next();
			if (term!=null) {
				String termString = term.utf8ToString();
				PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.OFFSETS);
				if (postingsEnum!=null) {
					postingsEnum.nextDoc();
					for (int i=0, len = postingsEnum.freq(); i<len; i++) {
						int pos = postingsEnum.nextPosition();
						if (termsOfInterest.containsKey(pos)) {
							termsOfInterest.put(pos, new TermInfo(termString, postingsEnum.startOffset(), postingsEnum.endOffset(), pos, 1));
						}
					}
				}
			}
			else {break;}
		}
	}
		
	public class DocumentSpansData implements Comparable<DocumentSpansData> {
		public int luceneDoc;
		public int[][] spansData;
		public String queryString;
		public DocumentSpansData(int luceneDoc, int[][] spansData ,String queryString) {
			this.luceneDoc = luceneDoc;
			this.queryString = queryString;
			this.spansData = spansData;
		}
		@Override
		public int compareTo(DocumentSpansData dsd) {
			// order by document and then position of first term
			return  luceneDoc==dsd.luceneDoc ?  spansData[0][0] - dsd.spansData[0][0] : luceneDoc - dsd.luceneDoc;
		}
	}
}

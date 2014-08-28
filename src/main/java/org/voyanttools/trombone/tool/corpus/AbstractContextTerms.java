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

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.vectorhighlight.FieldTermStack.TermInfo;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.lucene.search.SpanQueryParser;
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
	
	protected Map<Integer, Collection<DocumentSpansData>> getDocumentSpansData(AtomicReader atomicReader, StoredToLuceneDocumentsMapper corpusMapper, String[] queries) throws IOException {
		
		SpanQueryParser spanQueryParser = new SpanQueryParser(atomicReader, storage.getLuceneManager().getAnalyzer());
		Map<String, SpanQuery> spanQueries = spanQueryParser.getSpanQueriesMap(queries, tokenType, isQueryCollapse);
		Map<Term, TermContext> termContexts = new HashMap<Term, TermContext>();
		
		Collection<DocumentSpansData> documentSpansDataList = new ArrayList<DocumentSpansData>();
		
//		CorpusTermsQueue queue = new CorpusTermsQueue(size, corpusTermSort);
		for (Map.Entry<String, SpanQuery> spanQueryEntry : spanQueries.entrySet()) {
			String queryString = spanQueryEntry.getKey();
			SpanQuery spanQuery = spanQueryEntry.getValue();
			Spans spans = spanQuery.getSpans(atomicReader.getContext(), corpusMapper.getDocIdOpenBitSet(), termContexts);

			// we're going to go through all the span for all documents so that we can then
			// parallelize the searching of kwics
			
			// map lucene document id to span offset information
			List<int[]> spansDocDataList = new ArrayList<int[]>();
			int doc = -1;
			int lastDoc = -1;
			while(true) {
				boolean hasNext = spans.next();
				if (hasNext) {doc=spans.doc();}
				
				// add our values if we're done with spans or if we're on a new document
				if (!hasNext || doc!=lastDoc) {
					if (!spansDocDataList.isEmpty()) {
						int[][] data = new int[spansDocDataList.size()][2];
						for (int i=0, len=data.length; i<len; i++) {
							data[i] = spansDocDataList.get(i);
						}
						documentSpansDataList.add(new DocumentSpansData(lastDoc==-1 ? doc : lastDoc, data, queryString));
						spansDocDataList.clear();
					}
					if (!hasNext) {break;}
					lastDoc = doc;
				}
				if (this.positions.isEmpty()==false && this.positions.contains(spans.start())==false) {continue;} // skip 
				spansDocDataList.add(new int[]{spans.start(), spans.end()});
				total++;
			}
		}
		
		
		// build a map to organize by document for efficiency
		Map<Integer, Collection<DocumentSpansData>> documentSpansDataMap = new HashMap<Integer, Collection<DocumentSpansData>>();
		for (DocumentSpansData dsd : documentSpansDataList) {
			if (!documentSpansDataMap.containsKey(dsd.luceneDoc)) {
				documentSpansDataMap.put(dsd.luceneDoc, new ArrayList<DocumentSpansData>());
			}
			documentSpansDataMap.get(dsd.luceneDoc).add(dsd);
		}
		return documentSpansDataMap;
	}

	protected Map<Integer, TermInfo> getTermsOfInterest(AtomicReader atomicReader, int luceneDoc, int lastToken, Collection<DocumentSpansData> documentSpansData, boolean fill) throws IOException	{
		Map<Integer, TermInfo> termsOfInterest = getTermsOfInterest(documentSpansData, lastToken, fill);
		fillTermsOfInterest(atomicReader, luceneDoc, termsOfInterest);
		return termsOfInterest;
	}
	
	private Map<Integer, TermInfo> getTermsOfInterest(Collection<DocumentSpansData> documentSpansData, int lastToken, boolean fill)	{
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
				int rightend = keywordend + context;
				if (rightend>lastToken) {rightend=lastToken;}
				for (int i = fill ? keywordend : rightend, len = rightend+1; i < len; i++) {
					termsOfInterest.put(i, null);
				}
			}
		}
		return termsOfInterest;
	}
		
	private void fillTermsOfInterest(AtomicReader atomicReader, int luceneDoc, Map<Integer, TermInfo> termsOfInterest) throws IOException {
		// fill in terms of interest
		Terms terms = atomicReader.getTermVector(luceneDoc, tokenType.name());
		TermsEnum termsEnum = terms.iterator(null);
		while(true) {
			BytesRef term = termsEnum.next();
			if (term!=null) {
				String termString = term.utf8ToString();
				DocsAndPositionsEnum docsAndPositionsEnum = termsEnum.docsAndPositions(null, null, DocsAndPositionsEnum.FLAG_OFFSETS);
				docsAndPositionsEnum.nextDoc();
				for (int i=0, len = docsAndPositionsEnum.freq(); i<len; i++) {
					int pos = docsAndPositionsEnum.nextPosition();
					if (termsOfInterest.containsKey(pos)) {
						termsOfInterest.put(pos, new TermInfo(termString, docsAndPositionsEnum.startOffset(), docsAndPositionsEnum.endOffset(), pos, 1));
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
			return luceneDoc - dsd.luceneDoc;
		}
	}
}

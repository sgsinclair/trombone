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
package org.voyanttools.trombone.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SparseFixedBitSet;
import org.voyanttools.trombone.lucene.search.DocumentFilter;
import org.voyanttools.trombone.lucene.search.DocumentFilterSpans;
import org.voyanttools.trombone.lucene.search.FilteredCorpusDirectoryReader;
import org.voyanttools.trombone.lucene.search.FilteredCorpusReader;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;

/**
 * @author sgs
 *
 */
public class CorpusMapper {
	
	Storage storage;
	DirectoryReader reader;
	IndexSearcher searcher;
	Corpus corpus;
	private List<Integer> luceneIds = null;
	private BitSet bitSet = null;
	private Map<String, Integer> documentIdToLuceneIdMap = null;
	private Map<Integer, String> luceneIdToDocumentIdMap = null;

	public CorpusMapper(Storage storage, Corpus corpus) throws IOException {
		this.storage = storage;
		this.corpus = corpus;
	}
	
	public Storage getStorage() {
		return storage;
	}

	public Corpus getCorpus() {
		return corpus;
	}


	private synchronized List<String> getCorpusDocumentIds() {
		return corpus.getDocumentIds();
	}
	
	public synchronized List<Integer> getLuceneIds() throws IOException {
		if (luceneIds==null) {
			build();
		}
		return luceneIds;
	}
	
	public BitSet getBitSet() throws IOException {
		if (bitSet==null) {build();}
		return bitSet;
	}
	
	public IndexReader getIndexReader() throws IOException {
		if (reader==null) {
			build();
		}
		return reader;
	}
	
	public IndexSearcher getSearcher() throws IOException {
		if (searcher==null) {
			searcher = new IndexSearcher(getIndexReader());
		}
		return searcher;
	}

	public int getDocumentPositionFromLuceneId(int doc) throws IOException {
		String id = getDocumentIdFromLuceneId(doc);
		return corpus.getDocumentPosition(id);
	}


	public int getLuceneIdFromDocumentId(String id) throws IOException {
		if (documentIdToLuceneIdMap==null) {
			build();
		}
		return documentIdToLuceneIdMap.get(id);
	}

	public String getDocumentIdFromLuceneId(int doc) throws IOException {
		if (luceneIdToDocumentIdMap==null) {
			build();
		}
		return luceneIdToDocumentIdMap.get(doc);
	}
	
	public int getLuceneIdFromDocumentPosition(int doc) throws IOException {
		return getLuceneIdFromDocumentId(getDocumentIdFromDocumentPosition(doc));
	}

	private void build() throws IOException {
		luceneIdToDocumentIdMap =  new HashMap<Integer, String>();
		documentIdToLuceneIdMap = new HashMap<String, Integer>();
		luceneIds = new ArrayList<Integer>();
		buildFromTermsEnum();
	}
	
	/**
	 * This should not be called, except from the private build() method.
	 * @throws IOException
	 */
	private void buildFromTermsEnum() throws IOException {
		DirectoryReader dr = storage.getLuceneManager().getDirectoryReader(corpus.getId());
		for (LeafReaderContext rc : dr.leaves()) {
			LeafReader reader = rc.reader();
		
			Terms terms = reader.terms("id");
			TermsEnum termsEnum = terms.iterator();
			BytesRef bytesRef = termsEnum.next();
			int doc;
			String id;
			Set<String> ids = new HashSet<String>(getCorpusDocumentIds());
			bitSet = new SparseFixedBitSet(reader.numDocs());
			Bits liveBits = reader.getLiveDocs();
			while (bytesRef!=null) {
				PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.NONE);
				doc = postingsEnum.nextDoc();
				if (doc!=PostingsEnum.NO_MORE_DOCS) {
					id = bytesRef.utf8ToString();
					if (ids.contains(id)) {
						bitSet.set(doc);
						luceneIds.add(doc);
						documentIdToLuceneIdMap.put(id, doc);
						luceneIdToDocumentIdMap.put(doc, id);
					}
				}
				bytesRef = termsEnum.next();
			}
		}
		this.reader = new FilteredCorpusDirectoryReader(dr);
	}
	
	public String getDocumentIdFromDocumentPosition(int documentPosition) {
		return getCorpusDocumentIds().get(documentPosition);
	}

	public boolean hasLuceneId(int doc) throws IOException {
		if (bitSet==null) {
			build();
		}
		return bitSet.get(doc);
	}

	/**
	 * Get a Spans that filters for this corpus.
	 * @param spanQuery
	 * @return
	 * @throws IOException
	 */
	public Spans getFilteredSpans(SpanQuery spanQuery) throws IOException {
		return getFilteredSpans(spanQuery, getBitSet());
	}
	
	/**
	 * Get a Spans that filters for the specified BitSet.
	 * @param spanQuery
	 * @param bitSet
	 * @return
	 * @throws IOException
	 */
	public Spans getFilteredSpans(SpanQuery spanQuery, BitSet bitSet) throws IOException {
		SpanWeight weight = spanQuery.createWeight(getSearcher(), ScoreMode.COMPLETE_NO_SCORES, 1f);
		Spans spans = weight.getSpans(getLeafReader().getContext(), SpanWeight.Postings.POSITIONS);
		return spans != null ? new DocumentFilterSpans(spans, bitSet) : null;
	}
	
//	public Filter getFilter() throws IOException {
//		return new DocumentFilter(this);
//	}
//	
//	public Query getFilteredQuery(Query query) throws IOException {
//		BooleanQuery.Builder builder = new BooleanQuery.Builder();
//		builder.add(query, BooleanClause.Occur.MUST);
//		builder.add(getFilter(), BooleanClause.Occur.FILTER);
//		return builder.build();
//	}

	public BitSet getBitSetFromDocumentIds(Collection<String> documentIds) throws IOException {
		BitSet subBitSet = new SparseFixedBitSet(getIndexReader().numDocs());
		for (String id : documentIds) {
			subBitSet.set(getLuceneIdFromDocumentId(id));
		}
		return subBitSet;
	}
	
	public DocIdSet getDocIdSet() throws IOException {
		return new BitDocIdSet(getBitSet());
	}
}

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
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.DocIdBitSet;
import org.voyanttools.trombone.lucene.queries.CorpusFilter;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;

/**
 * @author sgs
 *
 */
public class CorpusMapper {
	
	Storage storage;
	AtomicReader reader;
	IndexSearcher searcher;
	Corpus corpus;
	private List<String> documentIds = null;
	private CorpusFilter corpusFilter = null;
	private DocIdSet docIdSet = null;
	private DocIdBitSet docIdBitSet = null;
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
		if (documentIds==null) {
			documentIds = corpus.getDocumentIds();
		}
		return documentIds;
	}
	
	public synchronized Filter getCorpusFilter() {
		if (corpusFilter==null) {
			corpusFilter = new CorpusFilter(corpus);
		}
		return corpusFilter;
	}
	
	public synchronized DocIdBitSet getDocIdBitSet() throws IOException {
		if (docIdBitSet==null) {
			BitSet bitSet = new BitSet(getAtomicReader().numDocs());
			DocIdSetIterator docIdSetIterator = getDocIdSet().iterator();
			int doc = docIdSetIterator.nextDoc();
			while (doc!=DocIdSetIterator.NO_MORE_DOCS) {
				bitSet.set(doc);
				doc = docIdSetIterator.nextDoc();
			}
			docIdBitSet = new DocIdBitSet(bitSet);
		}
		return docIdBitSet;
	}
	
	private synchronized DocIdSet getDocIdSet() throws IOException {
		if (docIdSet==null) {
			docIdSet = getCorpusFilter().getDocIdSet(getAtomicReader().getContext(), getAtomicReader().getLiveDocs());
		}
		return docIdSet;
	}


	public AtomicReader getAtomicReader() throws IOException {
		if (reader==null) {
			reader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getDirectoryReader());
		}
		return reader;
	}
	
	public IndexSearcher getSearcher() throws IOException {
		if (searcher==null) {
			searcher = new IndexSearcher(getAtomicReader());
		}
		return searcher;
	}

	public int getDocumentPositionFromLuceneId(int doc) throws IOException {
		String id = getDocumentIdFromLuceneId(doc);
		return corpus.getDocumentPosition(id);
	}


	public int getLuceneIdFromDocumentId(String id) throws IOException {
		if (documentIdToLuceneIdMap==null) {
			buildMaps();
		}
		return documentIdToLuceneIdMap.get(id);
	}

	public String getDocumentIdFromLuceneId(int doc) throws IOException {
		if (luceneIdToDocumentIdMap==null) {
			buildMaps();
		}
		return luceneIdToDocumentIdMap.get(doc);
	}

	private void buildMaps() throws IOException {
		luceneIdToDocumentIdMap =  new HashMap<Integer, String>();
		documentIdToLuceneIdMap = new HashMap<String, Integer>();
		buildMapsFromTermsEnum();
	}
	
	private void buildMapsFromTermsEnum() throws IOException {
		Terms terms = getAtomicReader().terms("id");
		TermsEnum termsEnum = terms.iterator(null);
		BytesRef bytesRef = termsEnum.next();
		DocIdBitSet docIdBitSet = getDocIdBitSet();
		DocsEnum docsEnum = null;
		int doc;
		String id;
		while (bytesRef!=null) {
			docsEnum = termsEnum.docs(docIdBitSet, docsEnum, DocsEnum.FLAG_NONE);
			doc = docsEnum.nextDoc();
			if (doc!=DocsEnum.NO_MORE_DOCS) {
				id = bytesRef.utf8ToString();
				while (doc!=DocsEnum.NO_MORE_DOCS) {
					documentIdToLuceneIdMap.put(id, doc);
					luceneIdToDocumentIdMap.put(doc, id);
					doc = docsEnum.nextDoc(); // can we have multiple documents with same ID in the same corpus?
				}
			}
			bytesRef = termsEnum.next();
		}
	}
	
	public String getDocumentIdFromDocumentPosition(int documentPosition) {
		return getCorpusDocumentIds().get(documentPosition);
	}

	public Bits getDocIdOpenBitSetFromStoredDocumentIds(
			List<String> documentIds) throws IOException {
		BitSet bitSet = new BitSet(getAtomicReader().numDocs());
		for (String id : documentIds) {
			bitSet.set(getLuceneIdFromDocumentId(id));
		}
		return new DocIdBitSet(bitSet);
	}



	/**
	 * @param ids 
	 * @param storage 
	 * @throws IOException 
	 * 
	 */
	/*
	public static StoredToLuceneDocumentsMapper getInstance(IndexSearcher searcher, Corpus corpus) throws IOException {
		return getInstance(searcher, LuceneManager.getCorpusQuery(corpus), corpus.getDocumentIds());
	}
	*/
	
	/*
	private static StoredToLuceneDocumentsMapper getInstance(IndexSearcher searcher, Query query, List<String> documentIds) throws IOException {
		SimpleDocIdsCollector collector = new SimpleDocIdsCollector();
		searcher.search(query, collector);
		Map<String, Integer> map = collector.getDocIdsMap();
		if (documentIds.size()!=map.size()) {
			throw new IllegalStateException("Corpus mapper has mismatched number of documents.");
		}
		int[] luceneIds = new int[documentIds.size()];
		for (int i=0, len = documentIds.size(); i<len; i++) {
			luceneIds[i] = map.get(documentIds.get(i));
		}
		return new StoredToLuceneDocumentsMapper(documentIds, luceneIds, searcher.getIndexReader().maxDoc());
	}
	*/
	
	/*
	private static int[] getLuceneIds(Storage storage, List<String> documentIds) throws IOException {
		int[] luceneIds = new int[documentIds.size()];
		for (int i=0; i<documentIds.size(); i++) {
			luceneIds[i] = storage.getLuceneManager().getLuceneDocumentId(documentIds.get(i));
		}
		return luceneIds;
	}
	*/
	
	/*
	private StoredToLuceneDocumentsMapper(List<String> documentIds, int[] luceneIds, int maxDocs) {
		this.maxDocs = maxDocs;
		this.lucenedIdToDocumentPositionMap = new HashMap<Integer, Integer>(documentIds.size());
		this.documentIdToLuceneId = new HashMap<String, Integer>(documentIds.size());
		this.sortedLuceneIds = new int[documentIds.size()];
		this.luceneIdToDocumentId = new HashMap<Integer, String>();
		this.documentIds = documentIds;
		for (int i=0, len=luceneIds.length; i<len; i++) {
			String documentId = documentIds.get(i);
			int luceneDocId = luceneIds[i];
			this.sortedLuceneIds[i] = luceneDocId;
			this.lucenedIdToDocumentPositionMap.put(luceneDocId, i);
			this.documentIdToLuceneId.put(documentId, luceneDocId);
			this.luceneIdToDocumentId.put(luceneDocId, documentId);
		}
		Arrays.sort(this.sortedLuceneIds);
		
	}
	
	public StoredToLuceneDocumentsMapper subSet(int[] luceneDocumentIds) {
		List<String> documentIds = new ArrayList<String>();
		for (int luceneDocumentId : luceneDocumentIds) {
			documentIds.add(luceneIdToDocumentId.get(luceneDocumentId));
		}
		return new StoredToLuceneDocumentsMapper(documentIds, luceneDocumentIds, maxDocs);
	}
	
	public DocIdSetIterator getDocIdSetIterator() {
		return new OpenBitSetIterator(getDocIdOpenBitSet());
	}
	
	public OpenBitSet getDocIdOpenBitSet() {
		if (docIdOpenBitSet==null) {
			OpenBitSet obs = new OpenBitSet(maxDocs);
			for (int i : sortedLuceneIds) {
				obs.set((long) i);
			}
			docIdOpenBitSet = obs;
		}
		return docIdOpenBitSet;
	}

	public int getDocumentPositionFromLuceneDocumentIndex(int luceneDocumentIndex) {
		return lucenedIdToDocumentPositionMap.get(luceneDocumentIndex);
	}
	
	public String getDocumentIdFromLuceneDocumentIndex(int doc) {
		return luceneIdToDocumentId.get(doc);
	}
	
	public String getDocumentIdFromDocumentPosition(int documentPosition) {
		return documentIds.get(documentPosition); 
	}



	public OpenBitSet getDocIdOpenBitSetFromStoredDocumentIds(
			List<String> storedDocumentIds) {
		OpenBitSet obs = new OpenBitSet(storedDocumentIds.size());
		for (String id : storedDocumentIds) {
			obs.set((long) documentIdToLuceneId.get(id));
		}
		return obs;
	}

	public int getLuceneIdFromDocumentId(String id) {
		return documentIdToLuceneId.get(id);
	}
	*/

}

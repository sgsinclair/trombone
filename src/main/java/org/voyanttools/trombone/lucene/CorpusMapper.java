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
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.DocIdBitSet;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;

/**
 * @author sgs
 *
 */
public class CorpusMapper extends Filter {
	
	Storage storage;
	AtomicReader reader;
	IndexSearcher searcher;
	Corpus corpus;
	private List<Integer> luceneIds = null;
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
		return corpus.getDocumentIds();
	}
	
	public synchronized List<Integer> getLuceneIds() throws IOException {
		if (luceneIds==null) {
			build();
		}
		return luceneIds;
	}
	
	public synchronized DocIdBitSet getDocIdBitSet() throws IOException {
		if (docIdBitSet==null) {
			build();
		}
		return docIdBitSet;
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
		Terms terms = getAtomicReader().terms("id");
		TermsEnum termsEnum = terms.iterator(null);
		BytesRef bytesRef = termsEnum.next();
		DocsEnum docsEnum = null;
		int doc;
		String id;
		Set<String> ids = new HashSet<String>(getCorpusDocumentIds());
		BitSet bitSet = new BitSet(getAtomicReader().numDocs());
		Bits liveBits = getAtomicReader().getLiveDocs();
		while (bytesRef!=null) {
			docsEnum = termsEnum.docs(liveBits, docsEnum, DocsEnum.FLAG_NONE);
			doc = docsEnum.nextDoc();
			if (doc!=DocsEnum.NO_MORE_DOCS) {
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
		docIdBitSet = new DocIdBitSet(bitSet);
	}
	
	public String getDocumentIdFromDocumentPosition(int documentPosition) {
		return getCorpusDocumentIds().get(documentPosition);
	}

	public DocIdBitSet getDocIdOpenBitSetFromStoredDocumentIds(
			List<String> documentIds) throws IOException {
		BitSet bitSet = new BitSet(getAtomicReader().numDocs());
		for (String id : documentIds) {
			bitSet.set(getLuceneIdFromDocumentId(id));
		}
		return new DocIdBitSet(bitSet);
	}

	@Override
	public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
		return getDocIdBitSet(); // this ignores the context and acceptDocs arguments, let's hope that's not a problem :)
	}


}

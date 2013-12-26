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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.DocIdSetIterator;

import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.OpenBitSetIterator;
import org.voyanttools.trombone.storage.Storage;

/**
 * @author sgs
 *
 */
public class StoredToLuceneDocumentsMapper {

//	private String[] documentIds;
	private Map<Integer, Integer> lucenedIdToDocumentPositionMap;
	private Map<String, Integer> documentIdToLuceneId;
	private Map<Integer, String> luceneIdToDocumentId;
	private int[] sortedLuceneIds;
	private OpenBitSet docIdOpenBitSet = null; // initialize this lazily
	
	/**
	 * @param ids 
	 * @param storage 
	 * @throws IOException 
	 * 
	 */
	public StoredToLuceneDocumentsMapper(Storage storage, List<String> documentIds) throws IOException {
		this(documentIds, getLuceneIds(storage, documentIds));
	}
	
	private static int[] getLuceneIds(Storage storage, List<String> documentIds) throws IOException {
		int[] luceneIds = new int[documentIds.size()];
		for (int i=0; i<documentIds.size(); i++) {
			luceneIds[i] = storage.getLuceneManager().getLuceneDocumentId(documentIds.get(i));
		}
		return luceneIds;
	}
	
	private StoredToLuceneDocumentsMapper(List<String> documentIds, int[] luceneIds) {
		this.lucenedIdToDocumentPositionMap = new HashMap<Integer, Integer>(documentIds.size());
		this.documentIdToLuceneId = new HashMap<String, Integer>(documentIds.size());
		this.sortedLuceneIds = new int[documentIds.size()];
		for (int i=0, len=luceneIds.length; i<len; i++) {
			int luceneDocId = luceneIds[i];
			this.sortedLuceneIds[i] = luceneDocId;
			this.lucenedIdToDocumentPositionMap.put(luceneDocId, i);
			this.documentIdToLuceneId.put(documentIds.get(i), luceneDocId);
		}
		Arrays.sort(this.sortedLuceneIds);
		
	}
	
	public StoredToLuceneDocumentsMapper subSet(int[] luceneDocumentIds) {
		List<String> documentIds = new ArrayList<String>();
		for (int luceneDocumentId : luceneDocumentIds) {
			documentIds.add(luceneIdToDocumentId.get(luceneDocumentId));
		}
		return new StoredToLuceneDocumentsMapper(documentIds, luceneDocumentIds);
	}
	
	public DocIdSetIterator getDocIdSetIterator() {
		return new OpenBitSetIterator(getDocIdOpenBitSet());
	}
	
	public OpenBitSet getDocIdOpenBitSet() {
		if (docIdOpenBitSet==null) {
			OpenBitSet obs = new OpenBitSet(sortedLuceneIds.length);
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


}

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
package org.voyanttools.trombone.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.storage.Storage;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public class Corpus implements Iterable<IndexedDocument> {

	@XStreamOmitField
	private Storage storage;

	private CorpusMetadata corpusMetadata;
	
	List<IndexedDocument> documents = null;
	
	@XStreamOmitField
	Map<String, Integer> documentPositionsMap = null;
	
	
	public Corpus(Storage storage, CorpusMetadata corpusMetadata) {
		this.storage = storage;
		this.corpusMetadata = corpusMetadata;
	}

	private List<IndexedDocument> getDocumentsList() throws IOException {
		if (documents==null) {
			documentPositionsMap = new HashMap<String, Integer>();
			documents = new ArrayList<IndexedDocument>();
			for (String id : getDocumentIds()) {
				documentPositionsMap.put(id, documents.size());
				documents.add(new IndexedDocument(storage, id));
			}
		}
		return documents;
	}

	public IndexedDocument getDocument(String id) throws IOException {
		if (documentPositionsMap==null) {getDocumentsList();} // this builds the map
		return getDocument(documentPositionsMap.get(id));
	}

	@Override
	public Iterator<IndexedDocument> iterator() {
		try {
			return getDocumentsList().iterator();
		} catch (IOException e) {
			throw new RuntimeException("Unable to load corpus documents.");
		}
	}

	public int size() throws IOException {
		return getDocumentIds().size();
	}

	public IndexedDocument getDocument(int docIndex) throws IOException {
		return getDocumentsList().get(docIndex);
	}

	public int[] getTokensCounts(TokenType tokenType) throws IOException {
		int[] counts = new int[size()];
		int index = 0;
		for (IndexedDocument document : this) {
			counts[index++] = document.getMetadata().getTokensCount(tokenType);
		}
		return counts;
	}

	public int[] getLastTokenPositions(TokenType tokenType) throws IOException {
		int[] counts = new int[size()];
		int index = 0;
		for (IndexedDocument document : this) {
			counts[index++] = document.getMetadata().getLastTokenPositionIndex(tokenType);
		}
		return counts;
	}

	public String getId() {
		return corpusMetadata.getId();
	}

	public CorpusMetadata getCorpusMetadata() {
		return corpusMetadata;
	}

	public int getDocumentPosition(String corpusId) {
		return documentPositionsMap.get(corpusId);
	}

	public List<String> getDocumentIds() {
		return corpusMetadata.getDocumentIds();
	}

	public int getTokensCount(TokenType tokenType) throws IOException {
		// TODO: this should probably be drawn from the corpus metadata instead
		int totalTokensCount = 0;
		for (int i : getTokensCounts(tokenType)) {
			totalTokensCount += i;
		
		}
		return totalTokensCount;
	}
}

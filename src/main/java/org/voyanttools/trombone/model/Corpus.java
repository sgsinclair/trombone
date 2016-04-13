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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.corpus.AbstractCorpusTool;
import org.voyanttools.trombone.tool.corpus.ConsumptiveTool;
import org.voyanttools.trombone.util.FlexibleParameters;

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

	public int getDocumentPosition(String corpusId) throws IOException {
		if (documentPositionsMap==null) {getDocumentsList();} // this builds the map
		return documentPositionsMap.get(corpusId);
	}

	public List<String> getDocumentIds() {
		return corpusMetadata.getDocumentIds();
	}

	public int getTokensCount(TokenType tokenType) throws IOException {
		// TODO: this should probably be drawn from the corpus metadata instead
		return corpusMetadata.getTokensCount(tokenType);
		/*
		int totalTokensCount = 0;
		for (int i : getTokensCounts(tokenType)) {
			totalTokensCount += i;
		
		}
		return totalTokensCount;
		*/
	}
	
	public CorpusAccess getValidatedCorpusAccess(FlexibleParameters parameters) throws CorpusAccessException {

		String password = parameters.getParameterValue("password", "");
		for (CorpusAccess mode : new CorpusAccess[]{CorpusAccess.ADMIN, CorpusAccess.ACCESS}) {
			String[] passwords = corpusMetadata.getAccessPasswords(mode);
			if (passwords.length>0) {
				for (String pass : passwords) {
					if (pass.isEmpty()==false && pass.equals(password)) {return mode;}
				}
				
				// if we have defined passwords for full and no matches, we raise error
				if (mode==CorpusAccess.ACCESS) {
					CorpusAccess noPasswordAccess = corpusMetadata.getNoPasswordAccess();
					if (noPasswordAccess==CorpusAccess.ACCESS) {
						throw new CorpusAccessException("Access to this tool requires a valid password.");
					}
					else if (noPasswordAccess==CorpusAccess.NONCONSUMPTIVE) {
						return CorpusAccess.NONCONSUMPTIVE;
					}
				}
			}
		}

		return CorpusAccess.NORMAL;
	}
	
	public String[] getLanguageCodes(boolean upateStorage) throws IOException {
		String[] languages = getCorpusMetadata().getLanguageCodes();
		if (languages!=null && languages.length>0) {
			return languages;
		} else {
			Set<String> languagesSet = new HashSet<String>();
			for (IndexedDocument indexedDocument : this) {
				String lang = indexedDocument.getMetadata().getLanguageCode();
				if (lang!=null && lang.isEmpty()==false) {
					languagesSet.add(lang);
				}
			}
			if (languagesSet.isEmpty()) {languagesSet.add("??");}
			languages = languagesSet.toArray(new String[0]);
			getCorpusMetadata().setLanguageCodes(languages);
			if (upateStorage) {
				storage.getCorpusStorage().updateStoredMetadata(this);
			}
			return languages;
		}
	}

	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Corpus)) {return false;}
		Corpus corpusObj = (Corpus) obj;
		List<String> corpusObjIds = corpusObj.getDocumentIds();
		List<String> ids = getDocumentIds();
		if (corpusObjIds.size()!=ids.size()) {return false;}
		for (int i=0; i<ids.size(); i++) {
			if (ids.get(i).equals(corpusObjIds.get(i))==false) {return false;}
		}
		return true;
	}

}

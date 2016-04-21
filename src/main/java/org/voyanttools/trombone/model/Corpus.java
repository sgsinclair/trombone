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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.voyanttools.trombone.storage.Storage;
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
	}
	
	public int[] getTokensCounts(TokenType tokenType) throws IOException {
		String id = getId()+"-"+tokenType.name().toLowerCase()+"TokenCounts";
		List<String> countList = new ArrayList<String>();
		if (tokenType==TokenType.lexical && storage.isStored(id)==false) {
			cacheCommonDocumentValues();
		} else if (storage.isStored(id)==false) {
			for (IndexedDocument doc : this) {
				countList.add(String.valueOf(doc.getMetadata().getTokensCount((tokenType))));
			}
			storage.storeStrings(countList, id);
		}
		if (countList.isEmpty()) {
			countList = storage.retrieveStrings(id);
		}
		int[] counts = new int[size()];
		int i=0;
		for (String pos : countList) {
			counts[i++] = Integer.parseInt(pos);
		}
		return counts;
	}	
	
	public int[] getLastTokenPositions(TokenType tokenType) throws IOException {
		String id = getId()+"-"+tokenType.name().toLowerCase()+"LastTokenPositions";
		List<String> positionsList = new ArrayList<String>();
		if (tokenType==TokenType.lexical && storage.isStored(id)==false) {
			cacheCommonDocumentValues();
		} else if (storage.isStored(id)==false) {
			for (IndexedDocument doc : this) {
				positionsList.add(String.valueOf(doc.getMetadata().getLastTokenPositionIndex(tokenType)));
			}
			storage.storeStrings(positionsList, id);
		}
		if (positionsList.isEmpty()) {
			positionsList = storage.retrieveStrings(id);
		}
		int[] positions = new int[size()];
		int i=0;
		for (String pos : positionsList) {
			positions[i++] = Integer.parseInt(pos);
		}
		return positions;
	}

	public float[] getTypesCountMeans(TokenType tokenType) throws IOException {
		String id = getId()+"-"+tokenType.name().toLowerCase()+"TypesCountMeans-1";
		List<String> meansList = new ArrayList<String>();
		if (tokenType==TokenType.lexical && storage.isStored(id)==false) {
			cacheCommonDocumentValues();
		} else if (storage.isStored(id)==false) {
			for (IndexedDocument doc : this) {
				meansList.add(String.valueOf(doc.getMetadata().getTypesCountMean(tokenType)));
			}
			storage.storeStrings(meansList, id);
		}
		if (meansList.isEmpty()) {
			meansList = storage.retrieveStrings(id);
		}
		float[] means = new float[size()];
		int i=0;
		for (String pos : meansList) {
			means[i++] = Float.parseFloat(pos);
		}
		return means;
	}

	public float[] getTypesCountStdDevs(TokenType tokenType) throws IOException {
		String id = getId()+"-"+tokenType.name().toLowerCase()+"TypesCountStdDevs-1";
		List<String> stdDevsList = new ArrayList<String>();
		if (tokenType==TokenType.lexical && storage.isStored(id)==false) {
			cacheCommonDocumentValues();
		} else if (storage.isStored(id)==false) {
			for (IndexedDocument doc : this) {
				stdDevsList.add(String.valueOf(doc.getMetadata().getTypesCountStdDev(tokenType)));
			}
			storage.storeStrings(stdDevsList, id);
		}
		if (stdDevsList.isEmpty()) {
			stdDevsList = storage.retrieveStrings(id);
		}
		float[] stdDevs = new float[size()];
		int i=0;
		for (String pos : stdDevsList) {
			stdDevs[i++] = Float.parseFloat(pos);
		}
		return stdDevs;
	}

	public Collection<String> getLanguageCodes() throws IOException {
		if (storage.isStored(getId()+"-langs")==false) {
			cacheCommonDocumentValues();
		}
		return storage.retrieveStrings(getId()+"-langs");
	}
	
	/**
	 * This is to help ensure that we don't load each document metadata individually, which takes time.
	 * @throws IOException
	 */
	private void cacheCommonDocumentValues() throws IOException {
		Set<String> langs = new HashSet<String>();
		List<String> tokenCounts = new ArrayList<String>();
		List<String> lastTokens = new ArrayList<String>();
		List<String> typesCountMeans = new ArrayList<String>();
		List<String> typesCountStdDev = new ArrayList<String>();
		DocumentMetadata metadata;
		for (IndexedDocument doc : this) {
			metadata = doc.getMetadata();
			String lang = metadata.getLanguageCode();
			if (lang!=null && lang.isEmpty()==false) langs.add(lang);
			tokenCounts.add(String.valueOf(metadata.getTokensCount(TokenType.lexical)));
			lastTokens.add(String.valueOf(metadata.getLastTokenPositionIndex(TokenType.lexical)));
			typesCountMeans.add(Float.toString(metadata.getTypesCountMean(TokenType.lexical)));
			typesCountStdDev.add(Float.toString(metadata.getTypesCountStdDev(TokenType.lexical)));
		}
		if (langs.isEmpty()) {langs.add("??");}
		if (storage.isStored(this.getId()+"-langs")==false) {
			storage.storeStrings(langs, this.getId()+"-langs");
		}
		if (storage.isStored(this.getId()+"-lexicalTokenCounts")==false) {
			storage.storeStrings(tokenCounts, this.getId()+"-lexicalTokenCounts");
		}
		if (storage.isStored(this.getId()+"-lexicalLastTokenPositions")==false) {
			storage.storeStrings(lastTokens, this.getId()+"-lexicalLastTokenPositions");
		}
		if (storage.isStored(this.getId()+"-lexicalTypesCountMeans-1")==false) {
			storage.storeStrings(typesCountMeans, this.getId()+"-lexicalTypesCountMeans-1");
		}
		if (storage.isStored(this.getId()+"-lexicalTypesCountStdDevs-1")==false) {
			storage.storeStrings(typesCountStdDev, this.getId()+"-lexicalTypesCountStdDevs-1");
		}
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

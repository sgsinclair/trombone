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
package org.voyanttools.trombone.tool.build;

import java.io.IOException;
import java.util.List;

import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusMetadata;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.ibm.icu.util.Calendar;

/**
 * @author sgs
 *
 */
class CorpusBuilder extends AbstractTool {

	private String storedId = null;

	/**
	 * @param storage
	 * @param parameters
	 */
	CorpusBuilder(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.RunnableTool#run()
	 */
	@Override
	public void run() throws IOException {
		// we shouldn't get here without a storedId parameter
		String sid = parameters.getParameterValue("storedId");
		run(sid);
	}
	
	void run(String corpusId, List<StoredDocumentSource> storedDocumentSources) throws IOException {
		// we should only get here during the corpus creator sequeence – 
		// the storedDocumentSource isn't used as a parameter, but it helps enforce the sequence
		run(corpusId);
	}
	
	private void run(String corpusId) throws IOException {
		// store and compute the corpus if it hasn't been stored
		if (storage.getCorpusStorage().corpusExists(corpusId)==false) {
			List<String> documentIds = storage.retrieveStrings(corpusId);
			CorpusMetadata metadata = new CorpusMetadata(corpusId);
			metadata.setDocumentIds(documentIds);
			metadata.setCreatedTime(Calendar.getInstance().getTimeInMillis());
			Corpus corpus = new Corpus(storage, metadata);
			DocumentMetadata documentMetadata;
			int totalWordTokens = 0;
			int totalWordTypes = 0;
			for (IndexedDocument doc : corpus) {
				documentMetadata = doc.getMetadata();
				totalWordTokens += documentMetadata.getTokensCount(TokenType.lexical);
				totalWordTypes +=  documentMetadata.getTypesCount(TokenType.lexical);
			}
			metadata.setTokensCount(TokenType.lexical, totalWordTokens);
			metadata.setTypesCount(TokenType.lexical, totalWordTypes);
			storage.getCorpusStorage().storeCorpus(corpus);
		}
		this.storedId = corpusId;
	}

	/*
	void run(List<StoredDocumentSource> storedDocumentSources) throws IOException {
		
		List<String> sortedIds = new ArrayList<String>();
		for (StoredDocumentSource sds : storedDocumentSources) {
			sortedIds.add(sds.getId());
		}

		// build a hash set of the ids to check against the corpus
		Set<String> ids = new HashSet<String>(sortedIds);

		// first see if we can load an existing corpus
		if (parameters.containsKey("corpus")) {
			Corpus corpus = storage.getCorpusStorage().getCorpus(parameters.getParameterValue("corpus"));
			if (corpus!=null) {		
				
				// add documents that aren't in the corpus already
				List<StoredDocumentSource> corpusStoredDocumentSources = new ArrayList<StoredDocumentSource>();
				boolean overlap = true;
				for (IndexedDocument document : corpus) {
					String id = document.getId();
					if (ids.contains(id)==false) {
						overlap = false;
						corpusStoredDocumentSources.add(document.asStoredDocumentSource());
						sortedIds.add(id);
						ids.add(id);
					}
				}
				
				// we have overlap and the two sets are the same size, so just use the current corpus
				if (overlap && ids.size() == corpus.size()) {
					storedId = parameters.getParameterValue("corpus");
					return;
				}
				
				// we're adding document to an existing corpus, so prepend the corpus documents that aren't here
				storedDocumentSources.addAll(0, corpusStoredDocumentSources);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for (String id : sortedIds) {
			sb.append(id);
		}
		
		storedId = DigestUtils.md5Hex(sb.toString());
		CorpusMetadata metadata = new CorpusMetadata(storedId);
		metadata.setDocumentIds(sortedIds);
		metadata.setCreatedTime(Calendar.getInstance().getTimeInMillis());
		Corpus corpus = new Corpus(storage, metadata);
		DocumentMetadata documentMetadata;
		int totalWordTokens = 0;
		int totalWordTypes = 0;
		for (IndexedDocument doc : corpus) {
			documentMetadata = doc.getMetadata();
			totalWordTokens += documentMetadata.getTokensCount(TokenType.lexical);
			totalWordTypes +=  documentMetadata.getTypesCount(TokenType.lexical);
		}
		metadata.setTokensCount(TokenType.lexical, totalWordTokens);
		metadata.setTypesCount(TokenType.lexical, totalWordTypes);
		if (storage.getCorpusStorage().corpusExists(storedId)==false) {
			storage.getCorpusStorage().storeCorpus(corpus);
		}
		
		
		
		if (parameters.containsKey("corpus")==false) {
			parameters.addParameter("corpus", storedId);
		}

	}
	*/

	String getStoredId() {
		return storedId;
	}

}

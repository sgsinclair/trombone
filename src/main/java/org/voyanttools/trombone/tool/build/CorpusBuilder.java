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
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusAccess;
import org.voyanttools.trombone.model.CorpusMetadata;
import org.voyanttools.trombone.model.CorpusTermMinimal;
import org.voyanttools.trombone.model.CorpusTermMinimalsDB;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class CorpusBuilder extends AbstractTool {

	private String storedId = null;

	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusBuilder(Storage storage, FlexibleParameters parameters) {
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
	
	void run(String corpusId) throws IOException {
		
		// store and compute the corpus if it hasn't been stored
		if (storage.getCorpusStorage().corpusExists(corpusId)==false) {
			
			List<String> documentIds = storage.retrieveStrings(corpusId, Storage.Location.object);
			
			// check if we have an admin password and update the corpusId if so
			if (parameters.getParameterValue("adminPassword", "").isEmpty()==false) {
				// we'll generate a new random corpus ID to make sure this administered corpus is unique
				corpusId = DigestUtils.md5Hex(corpusId+UUID.randomUUID().toString());
			}

			CorpusMetadata metadata = new CorpusMetadata(corpusId);
			metadata.setDocumentIds(documentIds);
			Corpus corpus = new Corpus(storage, metadata);
			
			setAccessManagement(metadata);
			indexCorpusTerms(corpus);
			if (parameters.containsKey("title")) {
				metadata.setTitle(parameters.getParameterValue("title"));
			}
			if (parameters.containsKey("subTitle")) {
				metadata.setSubTitle(parameters.getParameterValue("subTitle"));
			}
			
			if (DocumentFormat.getForgivingly(parameters.getParameterValue("inputFormat", ""))==DocumentFormat.DTOC) {
				int dtocIndexDoc = parameters.getParameterIntValue("dtocIndexDoc", 0);
				try {
					IndexedDocument doc = corpus.getDocument(dtocIndexDoc);
					doc.getMetadata().setExtra("isDtocIndex", "true");
					storage.getStoredDocumentSourceStorage().updateStoredDocumentSourceMetadata(doc.getId(), doc.getMetadata());
				} catch (IOException e) {
					// index out of bounds
				}
			}
			
			storage.getCorpusStorage().storeCorpus(corpus, parameters);
		}
		
		// TODO: handle existing corpus with new admin, simple duplication of the corpus should work
		else if (parameters.getParameterValue("adminPassword", "").isEmpty()==false) {
			Corpus corpus = storage.getCorpusStorage().getCorpus(corpusId);
			FlexibleParameters params = corpus.getCorpusMetadata().getFlexibleParameters();
			corpusId = DigestUtils.md5Hex(corpusId+UUID.randomUUID().toString());
			params.setParameter("id", corpusId);
			CorpusMetadata newCorpusMetadata = new CorpusMetadata(params);
			Corpus newCorpus = new Corpus(storage, newCorpusMetadata);
			
			setAccessManagement(newCorpusMetadata);
			indexCorpusTerms(corpus);

			storage.getCorpusStorage().storeCorpus(newCorpus, parameters);
		}
		this.storedId = corpusId;
	}
	
	private void setAccessManagement(CorpusMetadata corpusMetadata) {
		Set<String> passwordsSet = new HashSet<String>();
		for (CorpusAccess mode : new CorpusAccess[]{CorpusAccess.ADMIN, CorpusAccess.ACCESS}) {
			for (String password: parameters.getParameterValue(mode.name().toLowerCase()+"Password", "").split(",")) {
				password = password.trim();
				if (password.isEmpty()==false) {
					passwordsSet.add(password);
				}
			}
			if (passwordsSet.isEmpty()==false) {
				corpusMetadata.setPasswords(mode, passwordsSet.toArray(new String[0]));
				passwordsSet.clear();
			}
		}
		
		// set no password mode if there's an access password
		if (corpusMetadata.getAccessPasswords(CorpusAccess.ACCESS).length>0) {
			CorpusAccess corpusAccess = CorpusAccess.getForgivingly(parameters.getParameterValue("noPasswordAccess", ""));
			if (corpusAccess==CorpusAccess.NORMAL) {corpusAccess=CorpusAccess.NONCONSUMPTIVE;} // nothing set, so use non-consumptive
			if (corpusAccess==CorpusAccess.NONE || corpusAccess==CorpusAccess.NONCONSUMPTIVE) {
				corpusMetadata.setNoPasswordAccess(corpusAccess);
			}
		}
	}
	
	private void indexCorpusTerms(Corpus corpus) throws IOException {
		
		CorpusMapper corpusMapper = new CorpusMapper(storage, corpus);
		if (corpus.size()>0) {
			indexCorpusTerms(corpusMapper, corpus, TokenType.lexical);
			
			// build lemmatized forms if requested (assumes lemmatization has been done upstream)
			if (parameters.getParameterBooleanValue("lemmatize")) {
				indexCorpusTerms(corpusMapper, corpus, TokenType.lemma);
			}
		}		
	}
	private void indexCorpusTerms(CorpusMapper corpusMapper, Corpus corpus, TokenType tokenType) throws IOException {
		boolean verbose = parameters.getParameterBooleanValue("verbose");
		Calendar start = Calendar.getInstance();
		if (verbose) {log("Starting corpus terms index "+tokenType.name()+".");}
		// create and close to avoid concurrent requests later 
		CorpusTermMinimalsDB corpusTermMinimalsDB = CorpusTermMinimalsDB.getInstance(corpusMapper, tokenType);
		
		int totalWordTokens = 0;
		int totalWordTypes = 0;
		for (CorpusTermMinimal corpusTermMinimal : corpusTermMinimalsDB.values()) {
			totalWordTokens += corpusTermMinimal.getRawFreq();
			totalWordTypes++;
		}
		corpusTermMinimalsDB.close();
		
		CorpusMetadata metadata = corpus.getCorpusMetadata();
		metadata.setCreatedTime(Calendar.getInstance().getTimeInMillis());
		metadata.setTokensCount(tokenType, totalWordTokens);
		metadata.setTypesCount(tokenType, totalWordTypes);
		if (verbose) {log("Finished corpus terms index for "+tokenType.name()+".", start);}		
	}

	String getStoredId() {
		return storedId;
	}

}

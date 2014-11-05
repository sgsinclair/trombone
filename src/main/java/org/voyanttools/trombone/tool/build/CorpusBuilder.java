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
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusMetadata;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.CorpusTermMinimal;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.corpus.AbstractCorpusTool;
import org.voyanttools.trombone.tool.corpus.CorpusTerms;
import org.voyanttools.trombone.tool.corpus.CorpusTermMinimals;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.FlexibleQueue;

import com.ibm.icu.util.Calendar;

/**
 * @author sgs
 *
 */
public class CorpusBuilder extends AbstractCorpusTool {

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
			List<String> documentIds = storage.retrieveStrings(corpusId);
			CorpusMetadata metadata = new CorpusMetadata(corpusId);
			metadata.setDocumentIds(documentIds);
			metadata.setCreatedTime(Calendar.getInstance().getTimeInMillis());
			Corpus corpus = new Corpus(storage, metadata);
			run(corpus);
		}
		this.storedId = corpusId;
	}
	
	@Override
	public void run(Corpus corpus) throws IOException {
		int totalWordTokens = 0;
		int totalWordTypes = 0;
		for (IndexedDocument doc : corpus) {
			DocumentMetadata documentMetadata = doc.getMetadata();
			totalWordTokens += documentMetadata.getTokensCount(TokenType.lexical);
			totalWordTypes +=  documentMetadata.getTypesCount(TokenType.lexical);
		}
		CorpusMetadata metadata = corpus.getCorpusMetadata();
		metadata.setTokensCount(TokenType.lexical, totalWordTokens);
		metadata.setTypesCount(TokenType.lexical, totalWordTypes);
		

		// run this to store data and avoid concurrent requests later on (we don't need values locally)
		CorpusTermMinimals corpusTermsMinimal = new CorpusTermMinimals(storage, parameters);
		corpusTermsMinimal.run(corpus);
		
		storage.getCorpusStorage().storeCorpus(corpus);
	}


	String getStoredId() {
		return storedId;
	}

}

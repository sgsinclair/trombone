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
package org.voyanttools.trombone.tool;

import java.io.IOException;

import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public abstract class AbstractTermFrequencies extends AbstractTool {

	protected int total = 0;

	@XStreamOmitField
	protected int start;
	
	@XStreamOmitField
	protected int limit;
	
	@XStreamOmitField
	protected TokenType tokenType;
	
	@XStreamOmitField
	protected boolean isQueryCollapse;
	

	/**
	 * @param storage
	 * @param parameters
	 */
	public AbstractTermFrequencies(Storage storage,
			FlexibleParameters parameters) {
		super(storage, parameters);
		start = parameters.getParameterIntValue("start", 0);
		limit = parameters.getParameterIntValue("start", 50);
		tokenType = TokenType.getTokenTypeForgivingly(parameters.getParameterValue("tokenType", "lexical"));
		isQueryCollapse = parameters.getParameterBooleanValue("queryCollapse");
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.RunnableTool#run()
	 */
	@Override
	public void run() throws IOException {
		Corpus corpus = storage.getCorpusStorage().getCorpus(parameters.getParameterValue("corpus"));
		StoredToLuceneDocumentsMapper corpusMapper = new StoredToLuceneDocumentsMapper(storage, corpus.getDocumentIds());
		run(corpus, corpusMapper);
	}

	private void run(Corpus corpus, StoredToLuceneDocumentsMapper corpusMapper) throws IOException {
		if (parameters.containsKey("query")) {
			runQueries(corpus, corpusMapper);
		}
		else {
			runAllTerms(corpus, corpusMapper);
		}
	}
	
	protected abstract void runQueries(Corpus corpus, StoredToLuceneDocumentsMapper corpusMapper) throws IOException;
	protected abstract void runAllTerms(Corpus corpus, StoredToLuceneDocumentsMapper corpusMapper) throws IOException;

}

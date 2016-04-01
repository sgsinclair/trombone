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
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public abstract class AbstractTerms extends AbstractCorpusTool {

	protected int total = 0;

	@XStreamOmitField
	protected int start;
	
	@XStreamOmitField
	protected int limit;
	
	@XStreamOmitField
	protected TokenType tokenType;
	
//	@XStreamOmitField
//	protected boolean isQueryCollapse;
	
	/**
	 * @param storage
	 * @param parameters
	 */
	public AbstractTerms(Storage storage,
			FlexibleParameters parameters) {
		super(storage, parameters);
		start = parameters.getParameterIntValue("start", 0);
		limit = parameters.getParameterIntValue("limit", Integer.MAX_VALUE);
		tokenType = TokenType.getTokenTypeForgivingly(parameters.getParameterValue("tokenType", "lexical"));
//		isQueryCollapse = parameters.getParameterBooleanValue("queryCollapse");
	}


	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		Keywords stopwords = getStopwords(corpusMapper.getCorpus());
		if (parameters.containsKey("query")) {
			String[] queries =  getQueries(); // parameters.getParameterValues("query");
			if (queries.length==0 || (queries.length==1 && queries[0].isEmpty())) {
				runAllTerms(corpusMapper, stopwords);
			}
			else {
				// filter out queries that are in our stopwords list
				Set<String> queriesSet = new HashSet<String>();
				for (String query : queries) {
					if (stopwords.isKeyword(query)==false) {
						queriesSet.add(query);
					}
				}
				runQueries(corpusMapper, stopwords, queriesSet.toArray(new String[0]));
			}
		}
		else {
			runAllTerms(corpusMapper, stopwords);
		}
	}
	
	public int getTotal() {
		return total;
	}
	
	protected abstract void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException;
	protected abstract void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException;

}

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
package org.voyanttools.trombone.tool.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public abstract class AbstractTool implements RunnableTool {

	@XStreamOmitField
	protected FlexibleParameters parameters;
	
	@XStreamOmitField
	protected Storage storage;
	
	/**
	 * @param storage 
	 * 
	 */
	public AbstractTool(Storage storage, FlexibleParameters parameters) {
		this.storage = storage;
		this.parameters = parameters;
		
	}

	protected List<String> getCorpusStoredDocumentIdsFromParameters(Corpus corpus) throws IOException {
		
		List<String> ids = new ArrayList<String>();
		
		// add IDs
		for (String docId : parameters.getParameterValues("docId")) {
			ids.add(docId);
		}
		
		// add indices
		for (int docIndex : parameters.getParameterIntValues("docIndex")) {
			ids.add(corpus.getDocument(docIndex).getId());
		}
		
		// no docs defined, so consider all
		if (ids.isEmpty()) {
			for (IndexedDocument doc : corpus) {ids.add(doc.getId());}
		}
		
		return ids;
		
	}
	
	protected Keywords getStopwords() throws IOException {
		Keywords keywords = new Keywords();
		if (parameters.containsKey("stopList")) {
			keywords.load(storage, parameters.getParameterValues("stopList"));
		}
		return keywords;
	}
}

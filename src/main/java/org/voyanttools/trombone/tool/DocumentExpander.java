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
import java.util.ArrayList;
import java.util.List;

import org.voyanttools.trombone.input.expand.StoredDocumentSourceExpander;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.StoredDocumentSourceInputSource;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import edu.stanford.nlp.util.StringUtils;

/**
 * @author sgs
 *
 */
@XStreamAlias("expandedStoredDocuments")
public class DocumentExpander extends AbstractTool {

	private String storedId = null;
	
	@XStreamOmitField
	private List<StoredDocumentSource> storedDocumentSources = new ArrayList<StoredDocumentSource>();
	
	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentExpander(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run() throws IOException {
		String sid = parameters.getParameterValue("storedId");
		List<String> ids = storage.retrieveStrings(sid);
		StoredDocumentSourceStorage storedDocumentStorage = storage.getStoredDocumentSourceStorage();
		
		List<StoredDocumentSource> expandableStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		for (String id : ids) {
			DocumentMetadata metadata = storedDocumentStorage.getStoredDocumentSourceMetadata(id);
			StoredDocumentSource storedDocumentSource = new StoredDocumentSource(id, metadata);
			expandableStoredDocumentSources.add(storedDocumentSource);
		}

		run(expandableStoredDocumentSources);

	}
	
	void run(List<StoredDocumentSource> expandableStoredDocumentSources) throws IOException {
		
		StoredDocumentSourceStorage storedDocumentStorage = storage.getStoredDocumentSourceStorage();
		
		StoredDocumentSourceExpander expander = new StoredDocumentSourceExpander(storedDocumentStorage, parameters);
		
		for (StoredDocumentSource storedDocumentSource : expandableStoredDocumentSources) {
			storedDocumentSources.addAll(expander.getExpandedStoredDocumentSources(storedDocumentSource));
		}
		
		List<String> expandedIds = new ArrayList<String>();
		for (StoredDocumentSource storedDocumentSource : storedDocumentSources) {
			expandedIds.add(storedDocumentSource.getId());
		}
		
		storedId = storage.storeStrings(expandedIds);
		
	}

	public List<StoredDocumentSource> getStoredDocumentSources() {
		return storedDocumentSources;
	}

	public String getStoredId() {
		return storedId;
	}

}

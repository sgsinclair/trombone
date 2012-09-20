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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;

/**
 * @author sgs
 *
 */
public class Corpus implements Iterable<IndexedDocument> {

	private Storage storage;
	private String id;
	
	Map<String, IndexedDocument> documents = null;
	
	/**
	 * @param id 
	 * @param fileStorage 
	 * 
	 */
	public Corpus(Storage storage, String id) {
		this.storage = storage;
		this.id = id;
	}
	
	private Map<String, IndexedDocument> getDocumentsMap() throws IOException {
		if (documents==null) {
			documents = new LinkedHashMap<String, IndexedDocument>();
			List<StoredDocumentSource> storedDocumentSources = storage.getStoredDocumentSourceStorage().getMultipleExpandedStoredDocumentSources(id);
			for (StoredDocumentSource storedDocumentSource : storedDocumentSources) {
				String id = storedDocumentSource.getId();
				documents.put(id, new IndexedDocument(storage, id));
			}
		}
		return documents;
	}

	public IndexedDocument getDocument(String id) throws IOException {
		return getDocumentsMap().get(id);
	}

	@Override
	public Iterator<IndexedDocument> iterator() {
		try {
			return getDocumentsMap().values().iterator();
		} catch (IOException e) {
			throw new RuntimeException("Unable to load corpus documents.");
		}
	}

	public int size() throws IOException {
		return getDocumentsMap().size();
	}
}

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
package org.voyanttools.trombone.storage.memory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;

import edu.stanford.nlp.util.StringUtils;

/**
 * An in-memory implementation of the {@link StoredDocumentSourceStorage}. This
 * is typically faster (like for testing) but of course the size of the store is
 * limited by memory and is transient.
 * 
 * @author Stéfan Sinclair
 */
public class MemoryStorage implements Storage {

	private Map<String, Object> storedObjectsMap = new HashMap<String, Object>();
	
	/**
	 * the {@link StoredDocumentSourceStorage} for this storage
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;

	private LuceneManager luceneManager = null;

	/**
	 * Create a new instance of this class.
	 */
	public MemoryStorage() {
		storedDocumentSourceStorage = new MemoryStoredDocumentSourceStorage();
	}

	public StoredDocumentSourceStorage getStoredDocumentSourceStorage() {
		return storedDocumentSourceStorage;
	}

	public void destroy() throws IOException {
		storedDocumentSourceStorage = new MemoryStoredDocumentSourceStorage();
	}

	@Override
	public LuceneManager getLuceneManager() throws CorruptIndexException, IOException {
		if (luceneManager==null) {
			luceneManager = new LuceneManager(new RAMDirectory());
		}
		return luceneManager;
	}

	@Override
	public String storeString(String string) {
		String id = DigestUtils.md5Hex(string);
		storedObjectsMap.put(id, string);
		return id;
	}

	@Override
	public String retrieveString(String id) throws IOException {
		Object string = (String) storedObjectsMap.get(id);
		if (string==null) throw new IOException("Unable to find stored string with the ID: "+id);
		if (string instanceof String == false) throw new IOException("An object was stored with this ID but it's not a string: "+id);
		return (String) string;
	}

	@Override
	public String storeStrings(Collection<String> strings) throws IOException {
		String string = StringUtils.join(strings, "\n");
		return storeString(string);
	}

	@Override
	public List<String> retrieveStrings(String id) throws IOException {
		String string = retrieveString(id);
		return StringUtils.split(string, "\n");
	}
}

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

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;

/**
 * An in-memory implementation of the {@link StoredDocumentSourceStorage}. This
 * is typically faster (like for testing) but of course the size of the store is
 * limited by memory and is transient.
 * 
 * @author Stéfan Sinclair
 */
public class MemoryStorage implements Storage {

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
}

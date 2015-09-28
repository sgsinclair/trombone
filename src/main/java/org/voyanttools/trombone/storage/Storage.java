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
package org.voyanttools.trombone.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

import org.mapdb.DB;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.Keywords;

/**
 * This interface defines methods for interacting with stored objects using a storage strategy defined by the
 * implementing class.
 * 
 * @author Stéfan Sinclair
 */
public interface Storage {
	
	/**
	 * Get the {@link StoredDocumentSourceStorage} for this type of Storage.
	 * 
	 * @return the {@link StoredDocumentSourceStorage}
	 */
	public StoredDocumentSourceStorage getStoredDocumentSourceStorage();
	
	public LuceneManager getLuceneManager() throws IOException;
	
	/**
	 * Destroy (delete) this storage.
	 * 
	 * @throws IOException thrown if an exception occurs during deletion
	 */
	public void destroy() throws IOException;


	public boolean hasStoredString(String id);
	
	public String storeStrings(Collection<String> strings) throws IOException;
	
	public String storeString(String string) throws IOException;
	
	public void storeString(String string, String id) throws IOException;

	public String retrieveString(String id) throws IOException;
	
	public List<String> retrieveStrings(String id) throws IOException;
	
	public boolean isStored(String id);
	
	public String store(Object obj) throws IOException;
	
	public void store(Object obj, String id) throws IOException;
	
	public Object retrieve(String id) throws IOException, ClassNotFoundException;
	
	public Reader retrieveStringReader(String id) throws IOException;

	public CorpusStorage getCorpusStorage();
	
	/**
	 * Tries to get a migrator to convert a corpus from an older version to the current version.
	 * @param id the ID of the corpus to try to migrate
	 * @return a Migrator to perform the conversion
	 * @throws IOException
	 */
	public Migrator getMigrator(String id) throws IOException;
	

	public Writer getStoreStringWriter(String id) throws IOException;
	
	public DB getDB(String id, boolean readOnly);

	/**
	 * Storage-specific treatment for closing {@link DB}s.
	 * @param db
	 */
	public void closeDB(DB db);

	public boolean existsDB(String name);

}

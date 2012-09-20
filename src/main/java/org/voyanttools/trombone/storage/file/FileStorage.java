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
package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.store.NIOFSDirectory;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.storage.Storage;

import edu.stanford.nlp.util.StringUtils;

/**
 * A file-system implementation of {@link Storage}.
 * 
 * @author Stéfan Sinclair
 */
public class FileStorage implements Storage {
	
	/**
	 * the default file-system location for storage
	 */
	public static final String DEFAULT_TROMBOME_DIRECTORY = System.getProperty("java.io.tmpdir") + File.separator + "trombone4_0";
	
	
	/**
	 * the actual base directory used for storage
	 */
	File storageLocation;
	
	/**
	 * the handler for InputSource operations
	 */
	private FileStoredDocumentSourceStorage documentSourceStorage = null;
	
	private LuceneManager luceneManager = null;

	/**
	 * Create a new instance in the default location.
	 */
	public FileStorage() {
		this(new File(DEFAULT_TROMBOME_DIRECTORY));
	}
	
	

	/**
	 * Create a new instance at the specified File location
	 * 
	 * @param storageLocation the file location to use for this storage
	 */
	public FileStorage(File storageLocation) {
		System.out.println("Trombone FileStorage location: "+storageLocation);
		this.storageLocation = storageLocation;
		if (storageLocation.exists()==false) {
			storageLocation.mkdirs();
		}
	}

	public StoredDocumentSourceStorage getStoredDocumentSourceStorage() {
		if (documentSourceStorage==null) {
			documentSourceStorage = new FileStoredDocumentSourceStorage(this.storageLocation);
		}
		return documentSourceStorage;
	}

	public void destroy() throws IOException {
		FileUtils.deleteDirectory(storageLocation);
	}

	@Override
	public LuceneManager getLuceneManager() throws IOException {
		if (luceneManager==null) {
			File dir = new File(storageLocation, "lucene");
			if (dir.exists()==false) {dir.mkdirs();}
			luceneManager = new LuceneManager(new NIOFSDirectory(dir));
		}
		return luceneManager;
	}

	@Override
	public String storeString(String string) throws IOException {
		String id = DigestUtils.md5Hex(string);
		FileUtils.writeStringToFile(new File(getObjectStoreDirectory(),  id), string, "UTF-8");
		return id;
	}

	@Override
	public String storeStrings(Collection<String> strings) throws IOException {
		String string = StringUtils.join(strings, "\n");
		return storeString(string);
	}
	
	private File getObjectStoreDirectory() {
		return new File(storageLocation,"object-storage");
	}

	@Override
	public String retrieveString(String id) throws IOException {
		File file = new File(getObjectStoreDirectory(),  id);
		if (file.exists()==false) throw new IOException("An attempt was made to read a store string that that does not exist: "+id);
		return FileUtils.readFileToString(file);
	}
	
	@Override
	public List<String> retrieveStrings(String id) throws IOException {
		String string = retrieveString(id);
		return StringUtils.split(string, "\n");
	}



	@Override
	public Corpus getCorpus(String id) {
		return new Corpus(this, id);
	}
}

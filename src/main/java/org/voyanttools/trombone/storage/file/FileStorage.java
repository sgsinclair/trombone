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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.store.NIOFSDirectory;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.nlp.NlpAnnotator;
import org.voyanttools.trombone.nlp.NlpAnnotatorFactory;
import org.voyanttools.trombone.storage.CorpusStorage;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;

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
	public static final String DEFAULT_TROMBOME_DIRECTORY_NAME = "trombone4_2";

	/**
	 * the default file-system location for storage
	 */
	public static final File DEFAULT_TROMBOME_DIRECTORY = new File(System.getProperty("java.io.tmpdir"), DEFAULT_TROMBOME_DIRECTORY_NAME);
	
	
	/**
	 * the actual base directory used for storage
	 */
	File storageLocation;
	
	/**
	 * the handler for InputSource operations
	 */
	private FileStoredDocumentSourceStorage documentSourceStorage = null;
	
	
	private CorpusStorage corpusStorage = null;
	
	private LuceneManager luceneManager = null;
	
	private NlpAnnotatorFactory nlpAnnotatorFactory = new NlpAnnotatorFactory();

	/**
	 * Create a new instance in the default location.
	 */
	public FileStorage() {
		this(DEFAULT_TROMBOME_DIRECTORY);
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
		getLuceneManager().getIndexWriter().close();
		FileUtils.deleteDirectory(storageLocation);
	}

	@Override
	public LuceneManager getLuceneManager() throws IOException {
		if (luceneManager==null) {
			Path path = Paths.get(storageLocation.getPath(), "lucene");
			if (Files.exists(path)==false) {
				Files.createDirectories(path);
			}
			luceneManager = new LuceneManager(new NIOFSDirectory(path));
		}
		return luceneManager;
	}

	@Override
	public boolean hasStoredString(String id) {
		return getFile(id).exists();
	}

	@Override
	public String storeString(String string) throws IOException {
		String id = DigestUtils.md5Hex(string);
		storeString(string, id);
		return id;
	}
	
	@Override
	public void storeString(String string, String id) throws IOException {
		if (!isStored(id)) {
			FileUtils.writeStringToFile(getFile(id), string, "UTF-8");		
		}
	}


	@Override
	public String storeStrings(Collection<String> strings) throws IOException {
		String string = StringUtils.join(strings, "\n");
		return storeString(string);
	}
	
	@Override
	public void storeStrings(Collection<String> strings, String id) throws IOException {
		String string = StringUtils.join(strings, "\n");
		storeString(string, id);
	}
	
	@Override
	public String retrieveString(String id) throws IOException {
		File file = getFile(id);
		if (file.exists()==false) throw new IOException("An attempt was made to read a store string that that does not exist: "+id);
		return FileUtils.readFileToString(file);
	}
	
	@Override
	public List<String> retrieveStrings(String id) throws IOException {
		String string = retrieveString(id);
		return StringUtils.split(string, "\n");
	}

	@Override
	public CorpusStorage getCorpusStorage() {
		if (corpusStorage==null) {
			corpusStorage = new FileCorpusStorage(this, storageLocation);
		}
		return corpusStorage;
	}

	private File getObjectStoreDirectory() {
		return new File(storageLocation,"object-storage");
	}

	private File getFile(String id) {
		if (id==null) {
			System.err.println(getObjectStoreDirectory()+"\t"+id);
			
		}
		return new File(getObjectStoreDirectory(),  id);
	}
	
	public boolean copyResource(File source, String id) throws IOException {
		File destination = getFile(id);
		if (destination.exists()) {return false;}
		FileUtils.copyFile(source, destination);
		return true;
	}


	@Override
	public boolean isStored(String id) {
		return getFile(id).exists();
	}

	@Override
	public String store(Object obj) throws IOException {
		String id = UUID.randomUUID().toString();
		store(obj, id);
		return id;
	}



	@Override
	public void store(Object obj, String id) throws IOException {
		File file = getFile(id);
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		ObjectOutputStream out = new ObjectOutputStream(fileOutputStream);
		out.writeObject(obj);
		out.close();
	}



	@Override
	public Object retrieve(String id) throws IOException, ClassNotFoundException {
		File file = getFile(id);
		FileInputStream fileInputStream = new FileInputStream(file);
		ObjectInputStream in = new ObjectInputStream(fileInputStream);
		Object obj = in.readObject();
		in.close();
		return obj;
	}

	@Override
	public Reader retrieveStringReader(String id) throws IOException {
		File file = getFile(id);
		return new FileReader(file);
	}



	@Override
	public Writer getStoreStringWriter(String id) throws IOException {
		File file = getFile(id);
		return new FileWriter(file);
	}



	@Override
	public DB getDB(String id, boolean readOnly) {
		DBMaker maker = DBMaker.newFileDB(getFile(id))
			.transactionDisable()
			.closeOnJvmShutdown()
			.mmapFileEnableIfSupported();
		if (readOnly) {return maker.readOnly().make();}
		else {return maker.make();}
	}
	
	public void closeDB(DB db) {
		db.close();
	}
	
	public boolean existsDB(String id) {
		return getFile(id).exists();
	}

	@Override
	public FileMigrator getMigrator(String id) throws IOException {
		return FileMigrationFactory.getMigrator(this, id);
	}



	@Override
	public NlpAnnotator getNlpAnnotator(String languageCode) {
		return nlpAnnotatorFactory.getNlpAnnotator(languageCode);
	}
	
}

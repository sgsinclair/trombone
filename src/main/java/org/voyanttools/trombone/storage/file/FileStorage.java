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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.lucene.PerCorpusIndexLuceneManager;
import org.voyanttools.trombone.lucene.SingleIndexLuceneManager;
import org.voyanttools.trombone.nlp.NlpFactory;
import org.voyanttools.trombone.storage.CorpusStorage;
import org.voyanttools.trombone.storage.DirectoryFactory;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * A file-system implementation of {@link Storage}.
 * 
 * @author Stéfan Sinclair
 */
public class FileStorage implements Storage {
	
	/**
	 * the default file-system location for storage
	 */
	public static final String DEFAULT_TROMBOME_DIRECTORY_NAME = "trombone5_2";

	/**
	 * the default file-system location for storage
	 */
	public static final File DEFAULT_TROMBOME_DIRECTORY = new File(System.getProperty("java.io.tmpdir"), DEFAULT_TROMBOME_DIRECTORY_NAME);
	
	private static final String LUCENE_DIRECTORY_NAME = "lucene";
	private static final String LUCENE_PER_CORPUS_DIRECTORY_NAME = "lucene-per-corpus";
	
	/**
	 * the actual base directory used for storage
	 */
	public File storageLocation;
	
	/**
	 * the handler for InputSource operations
	 */
	private FileStoredDocumentSourceStorage documentSourceStorage = null;
	
	
	private CorpusStorage corpusStorage = null;
	
	private LuceneManager luceneManager = null;
	
	private NlpFactory nlpAnnotatorFactory = new NlpFactory();
	
	private FlexibleParameters parameters;

	/**
	 * Create a new instance in the default location.
	 * @throws IOException 
	 */
	public FileStorage() throws IOException {
		this(DEFAULT_TROMBOME_DIRECTORY, new FlexibleParameters());
	}
	
	/**
	 * Create a new instance at the File location specified by the dataDirectory parameter
	 * 
	 * @param storageLocation the file location to use for this storage
	 * @throws IOException 
	 */
	public FileStorage(FlexibleParameters parameters) throws IOException {
		this(parameters.containsKey("dataDirectory") ? new File(parameters.getParameterValue("dataDirectory")) : DEFAULT_TROMBOME_DIRECTORY, parameters);
	}
	

	/**
	 * Create a new instance at the specified File location
	 * 
	 * @param storageLocation the file location to use for this storage
	 * @throws IOException 
	 */
	public FileStorage(File storageLocation) throws IOException {
		this(storageLocation, new FlexibleParameters());
	}

	/**
	 * Create a new instance at the specified File location
	 * 
	 * @param storageLocation the file location to use for this storage
	 * @throws IOException 
	 */
	public FileStorage(File storageLocation, FlexibleParameters parameters) throws IOException {
		System.out.println("Trombone FileStorage location: "+storageLocation);
		this.parameters = parameters;
		this.storageLocation = storageLocation;
		if (storageLocation.exists()==false) {
			if (!storageLocation.mkdirs()) {
				throw new IOException("Unable to create data directory: "+storageLocation);
			}
		}
	}
	
	public StoredDocumentSourceStorage getStoredDocumentSourceStorage() {
		if (documentSourceStorage==null) {
			documentSourceStorage = new FileStoredDocumentSourceStorage(this.storageLocation);
		}
		return documentSourceStorage;
	}

	public void destroy() throws IOException {
		getLuceneManager().closeAll();
		FileUtils.deleteDirectory(storageLocation);
	}

	@Override
	public LuceneManager getLuceneManager() throws IOException {
		if (luceneManager==null) {
			if (parameters.getParameterValue("storage", "file").toLowerCase().equals("file-per-corpus")) {
				if (Files.exists(Paths.get(storageLocation.getPath(), LUCENE_DIRECTORY_NAME))) {
					System.err.println("\n"+
					"*** WARNING: This instance has been configured for per-corpus Lucene index but\n"+
					"*** a directory for Lucene (as a combined index) already exists, which could\n"+
					"*** cause confusion and mayhem. Please consider changing the data directory\n"+
					"*** or move the existing data directory. Adding a dot and a number to this\n"+
					"*** data directory will facilitate data migration.\n\n");
				}
				Path path = Paths.get(storageLocation.getPath(), LUCENE_PER_CORPUS_DIRECTORY_NAME);
				DirectoryFactory directoryFactory = new PerCorpusDirectoryFactory(path);
				luceneManager = new PerCorpusIndexLuceneManager(this, directoryFactory);
			} else {
				if (Files.exists(Paths.get(storageLocation.getPath(), LUCENE_PER_CORPUS_DIRECTORY_NAME))) {
					System.err.println("\n"+
					"*** WARNING: This instance has been configured for one Lucene index but a\n"+
					"*** directory for Lucene with an index per corpus already exists, which could\n"+
					"*** cause confusion and mayhem. Please consider changing the data directory or\n"+
					"*** move the existing data directory. Adding a dot and a number to this data\n"+
					"*** directory will facilitate data migration.");
				}
				Path path = Paths.get(storageLocation.getPath(), LUCENE_DIRECTORY_NAME);
				DirectoryFactory directoryFactory = new SingleDirectoryFactory(path);
				luceneManager = new SingleIndexLuceneManager(this, directoryFactory);
			}
		}
		return luceneManager;
	}
	
	@Override
	public boolean hasStoredString(String id, Location location) {
		return getResourceFile(id, location).exists();
	}

	@Override
	public String storeString(String string, Location location) throws IOException {
		String id = DigestUtils.md5Hex(string);
		storeString(string, id, location);
		return id;
	}
	
	@Override
	public void storeString(String string, String id, Location location) throws IOException {
		storeString(string, id, location, false);
	}

	@Override
	public void storeString(String string, String id, Location location, boolean canOverwrite) throws IOException {
		File file = getResourceFile(id, location);
		if (file.getParentFile().exists()==false) { // make sure directory exists
			file.getParentFile().mkdirs();
		}
		if (!isStored(id, location) || canOverwrite) {
			FileUtils.writeStringToFile(file, string, "UTF-8");		
		}
	}

	@Override
	public String storeStrings(Collection<String> strings, Location location) throws IOException {
		String string = StringUtils.join(strings, "\n");
		return storeString(string, location);
	}
	
	@Override
	public void storeStrings(Collection<String> strings, String id, Location location) throws IOException {
		String string = StringUtils.join(strings, "\n");
		storeString(string, id, location);
	}
	
	@Override
	public String retrieveString(String id, Location location) throws IOException {
		File file = getResourceFile(id, location);
		if (file.exists()==false) throw new IOException("An attempt was made to read a store string that that does not exist: "+id);
		
		return FileUtils.readFileToString(file);
	}
	
	@Override
	public List<String> retrieveStrings(String id, Location location) throws IOException {
		String string = retrieveString(id, location);
		return Arrays.asList(StringUtils.split(string, "\n"));
	}

	@Override
	public CorpusStorage getCorpusStorage() {
		if (corpusStorage==null) {
			corpusStorage = new FileCorpusStorage(this, storageLocation);
		}
		return corpusStorage;
	}

	private File getObjectStoreDirectory(Location location) {
		switch (location) {
			case cache:
			case notebook:
				return new File(storageLocation, location.name());
		default:
			return new File(storageLocation,"object-storage");
		}
	}
	
	File getResourceFile(String id, Location location) {
		id = new File(id).getName(); // make sure we're not doing any directory traversal
		// package level for migrators
		if (id==null) {
			throw new IllegalArgumentException("No ID provided for stored resource");
		}
		File file = new File(getObjectStoreDirectory(location),  id+".gz");
		if (file.exists()) {return file;}
		else {return new File(getObjectStoreDirectory(location),  id);}
	}
	
	public boolean copyResource(File source, String id, Location location) throws IOException {
		File destination = getResourceFile(id, location);
		if (destination.exists()) {return false;}
		FileUtils.copyFile(source, destination);
		return true;
	}


	@Override
	public boolean isStored(String id, Location location) {
		return getResourceFile(id, location).exists();
	}

	@Override
	public String store(Object obj, Location location) throws IOException {
		String id = UUID.randomUUID().toString();
		store(obj, id, location);
		return id;
	}



	@Override
	public void store(Object obj, String id, Location location) throws IOException {
		File file = getResourceFile(id, location);
		if (file.getParentFile().exists()==false) { // make sure directory exists
			file.getParentFile().mkdirs();
		}
		
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		ObjectOutputStream out = new ObjectOutputStream(fileOutputStream);
		out.writeObject(obj);
		out.close();
	}



	@Override
	public Object retrieve(String id, Location location) throws IOException, ClassNotFoundException {
		File file = getResourceFile(id, location);
		FileInputStream fileInputStream = new FileInputStream(file);
		ObjectInputStream in = new ObjectInputStream(fileInputStream);
		Object obj = in.readObject();
		in.close();
		return obj;
	}
	
	public Writer getStoreWriter(String id, Location location) throws IOException {
		return getStoreWriter(id, location, false);
	}
	
	public Writer getStoreWriter(String id, Location location, boolean append) throws IOException {
		File file = getResourceFile(id, location);
		if (file.getParentFile().exists()==false) { // make sure directory exists
			file.getParentFile().mkdirs();
		}
		return new FileWriter(file, append);
	}
	
	public Reader getStoreReader(String id, Location location) throws IOException {
		File file = getResourceFile(id, location);
		return new FileReader(file);
	}

	/*

	private File getStoreCacheDirectory() {
		return new File(storageLocation, "cache");
	}

	private File getCachedFile(String id) {
		return new File(getStoreCacheDirectory(), id);
	}

	@Override
	public Reader retrieveCachedStringReader(String id) throws IOException {
		File file = getCachedFile(id);
		return new FileReader(file);
	}

	@Override
	public Writer getStoreCachedStringWriter(String id) throws IOException {
		File file = getCachedFile(id);
		if (file.getParentFile().exists()==false) { // make sure directory exists
			file.getParentFile().mkdirs();
		}
		return new FileWriter(file);
	}
	
	@Override
	public boolean isStoredCache(String id) {
		File file = getCachedFile(id);
		return file.exists();
	}
	*/

	@Override
	public DB getDB(String id, boolean readOnly) {
		DBMaker maker = DBMaker.newFileDB(getResourceFile(id, Location.object))
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
		return getResourceFile(id, Location.object).exists();
	}

	@Override
	public FileMigrator getMigrator(String id) throws IOException {
		return FileMigrationFactory.getMigrator(this, id);
	}

	@Override
	public NlpFactory getNlpAnnotatorFactory() {
		return nlpAnnotatorFactory;
	}
	
	public File getLocalSourcesDirectory() {
		File rootData = storageLocation.getParentFile();
		return new File(rootData, "trombone-local-sources");
	}

	public File getLocalResourcesDirectory() {
		File rootData = storageLocation.getParentFile();
		return new File(rootData, "trombone-resources");
	}
}

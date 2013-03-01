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
import java.io.IOException;
import java.io.OutputStream;

import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusMetadata;
import org.voyanttools.trombone.storage.CorpusStorage;
import org.voyanttools.trombone.storage.Storage;

/**
 * @author sgs
 *
 */
public class FileCorpusStorage implements CorpusStorage {
	
	private Storage storage;
	
	private File corpusStorageLocation;
	
	private static String CORPUS_DIRECTORY_NAME = "corpora";
	
	private static String METADATA_FILE_NAME = "metadata.xml"; 

	FileCorpusStorage(Storage storage, File storageLocation) {
		this.storage = storage;
		corpusStorageLocation = new File(storageLocation, CORPUS_DIRECTORY_NAME);
		if (corpusStorageLocation.exists()==false) {
			corpusStorageLocation.mkdir(); // shouldn't need to create parent
		}
	}

	@Override
	public Corpus getCorpus(String id) throws IOException {
		if (corpusExists(id)) {
			File corpusDirectory = new File(corpusStorageLocation, id);
			CorpusMetadata metadata = new CorpusMetadata(id);
			File metadataFile = new File(corpusDirectory, METADATA_FILE_NAME);
			if (metadataFile.exists()) {
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(metadataFile);
					metadata.getProperties().loadFromXML(fis);
					fis.close();
				}
				finally {
					if (fis!=null) fis.close();
				}
				return new Corpus(storage, metadata);
			}
			else {
				throw new IOException("The metadata file for this corpus was not found so the corpus is unusable.");
			}
		}
		else {
			throw new IOException("This corpus was not found. It's possible that it's been removed or that it never existed (that the corpus ID is wrong)");
		}
	}

	@Override
	public void storeCorpus(Corpus corpus) throws IOException {
		String id = corpus.getId();
		if (corpusExists(id)) {
			throw new IOException("This corpus already exists: "+id);
		}
		else {
			File corpusDirectory = new File(corpusStorageLocation, id);
			corpusDirectory.mkdir();
			File metadataFile = new File(corpusDirectory, METADATA_FILE_NAME);
			OutputStream os = null;
			try {
				os = new FileOutputStream(metadataFile);
				corpus.getCorpusMetadata().getProperties().storeToXML(os, "This is a Trombone Corpus Metadata file");
				os.close();
			}
			finally {
				if (os!=null) os.close();
			}
		}
	}

	@Override
	public boolean corpusExists(String id) {
		File corpusDirectory = new File(corpusStorageLocation, id);
		return corpusDirectory.exists();
	}

}

package org.voyanttools.trombone.lucene;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.LockObtainFailedException;
import org.voyanttools.trombone.storage.DirectoryFactory;
import org.voyanttools.trombone.storage.Storage;

public class PerCorpusIndexLuceneManager extends AbstractLuceneManager {
	
	private Map<String, LuceneManager> indexMap;
	
	private DirectoryFactory directoryFactory;
	
	private Storage storage;
	
	public PerCorpusIndexLuceneManager(Storage storage, DirectoryFactory directoryFactory) throws CorruptIndexException, IOException {
		super();
		this.storage = storage;
		this.directoryFactory = directoryFactory;
		indexMap = new HashMap<String, LuceneManager>();
	}
	
	private synchronized LuceneManager getLuceneManager(String corpus) throws CorruptIndexException, IOException {
		if (indexMap.containsKey(corpus)==false) {
			indexMap.put(corpus, new SingleIndexLuceneManager(storage, directoryFactory));
			Set<String> forCleanUp = new HashSet<String>();
			if (indexMap.size()>1) {
				long now = System.currentTimeMillis();
				for (Map.Entry<String, LuceneManager> entry : indexMap.entrySet()) {
					String id = entry.getKey();
					if (now-entry.getValue().getLastAccessed()>30000) {
						forCleanUp.add(id);
					}
				}
			}
			for (String id : forCleanUp) {
				LuceneManager manager = indexMap.get(id);
				manager.close(id);
				manager = null;
				indexMap.remove(id);
			}
		}
		return indexMap.get(corpus);
	}
	
	public DirectoryReader getDirectoryReader(String corpus) throws CorruptIndexException, IOException {
		return getLuceneManager(corpus).getDirectoryReader(corpus);
	}
		
	public void addDocument(String corpus, Document document) throws CorruptIndexException, IOException {
		getLuceneManager(corpus).addDocument(corpus, document);
	}
	
	// TODO: make this block across threads so that only one writer can exist at a time
	public synchronized IndexWriter getIndexWriter(String corpus) throws CorruptIndexException, LockObtainFailedException, IOException {
		return getLuceneManager(corpus).getIndexWriter(corpus);
	}

	public Analyzer getAnalyzer(String corpus) {
		LuceneManager luceneManager;
		try {
			luceneManager = getLuceneManager(corpus);
		} catch (Exception e) {
			throw new RuntimeException("Unable to load analyzer.", e);
		}
		return luceneManager.getAnalyzer(corpus);
	}
	
	public boolean directoryExists(String corpus) throws IOException {
		return getLuceneManager(corpus).directoryExists(corpus);
	}

	public void setDirectoryReader(String corpus, DirectoryReader indexReader) {
		LuceneManager luceneManager;
		try {
			luceneManager = getLuceneManager(corpus);
		} catch (Exception e) {
			throw new RuntimeException("Unable to load analyzer.", e);
		}
		luceneManager.setDirectoryReader(corpus, indexReader);
	}
	
	public void close(String corpus) throws IOException {
		getLuceneManager(corpus).close(corpus);
	}
	
	public void closeAll() throws IOException {
		for (Map.Entry<String, LuceneManager> entry : indexMap.entrySet()) {
			entry.getValue().close(entry.getKey());
		}
	}

}

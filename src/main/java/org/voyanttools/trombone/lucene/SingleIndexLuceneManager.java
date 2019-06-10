package org.voyanttools.trombone.lucene;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.voyanttools.trombone.lucene.analysis.KitchenSinkPerFieldAnalyzerWrapper;
import org.voyanttools.trombone.storage.DirectoryFactory;
import org.voyanttools.trombone.storage.Storage;

public class SingleIndexLuceneManager extends AbstractLuceneManager {

	private Directory directory;
	
	private DirectoryReader directoryReader = null;
	
	private IndexWriter indexWriter = null;
	
	private IndexSearcher indexSearcher = null;
	
	private Analyzer analyzer;
	
	private Storage storage;
	
	private DirectoryFactory directoryFactory;
	
	public SingleIndexLuceneManager(Storage storage, DirectoryFactory directoryFactory) throws CorruptIndexException, IOException {
		super();
		this.storage = storage;
		this.directoryFactory = directoryFactory;
		analyzer = new KitchenSinkPerFieldAnalyzerWrapper(storage);
	}
	
	private Directory getDirectory(String corpus) throws IOException {
		if (directory==null) {
			directory = directoryFactory.getDirectory(corpus);
			access();
		}
		return directory;
	}
	
	public DirectoryReader getDirectoryReader(String corpus) throws CorruptIndexException, IOException {
		if (directoryReader == null) {
			directoryReader = DirectoryReader.open(getDirectory(corpus));
		}
		access();
		return directoryReader;
	}
		
	public void addDocument(String corpus, Document document) throws CorruptIndexException, IOException {
		IndexWriter writer = getIndexWriter(corpus);
		writer.addDocument(document);
		writer.commit();
		setDirectoryReader(corpus, DirectoryReader.open(writer));
	}
	
	// TODO: make this block across threads so that only one writer can exist at a time
	public synchronized IndexWriter getIndexWriter(String corpus) throws CorruptIndexException, LockObtainFailedException, IOException {
		if (indexWriter==null) {
			indexWriter = new IndexWriter(getDirectory(corpus), new IndexWriterConfig(analyzer));
		}
		access();
		return indexWriter;
	}

	public Analyzer getAnalyzer(String corpus) {
		return analyzer;
	}
	
	public boolean directoryExists(String corpus) throws IOException {
		return DirectoryReader.indexExists(getDirectory(corpus));
	}

	public void setDirectoryReader(String corpus, DirectoryReader indexReader) {
		this.directoryReader = indexReader;
		this.indexSearcher = new IndexSearcher(directoryReader);
		access();
	}

	public void close(String corpus) throws IOException {
		try {
			getIndexWriter(corpus).close();
		} catch (Exception e) {
			if (e instanceof IOException) {
				throw e;
			} else {
				throw new IOException("Error closing index writer: "+corpus, e);
			}
		}
	}
	
	public void closeAll() throws IOException {
		close(RandomStringUtils.randomAlphabetic(10));
	}
	
}


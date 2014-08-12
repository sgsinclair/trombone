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
package org.voyanttools.trombone.lucene;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.voyanttools.trombone.input.index.LuceneIndexer;
import org.voyanttools.trombone.lucene.analysis.KitchenSinkPerFieldAnalyzerWrapper;

/**
 * @author sgs
 *
 */
public class LuceneManager {
	
	private Directory directory;
	
	private DirectoryReader directoryReader = null;
	
	private IndexWriter indexWriter = null;
	
	private IndexSearcher indexSearcher = null;
	
	public static Version VERSION = Version.LUCENE_4_9;
	
	private float luceneDocumentVersion = 4.1f;

	
	private Analyzer analyzer = new KitchenSinkPerFieldAnalyzerWrapper();
	
	private IndexWriterConfig indexWriterConfig = new IndexWriterConfig(VERSION, analyzer);
	
	public LuceneManager(Directory directory) throws CorruptIndexException, IOException {
		this.directory = directory;
	}
	
	public Query getCorpusDocumentQuery(String corpusId, String documentId) throws IOException {
		BooleanQuery query = new BooleanQuery();
		if (corpusId!=null) {query.add(new TermQuery(new Term("corpus", corpusId)), Occur.MUST);}
		query.add(new TermQuery(new Term("id", documentId)), Occur.MUST);
		query.add(new TermQuery(new Term("version", String.valueOf(VERSION))), Occur.MUST);
		return query;
	}
	
	private int getLuceneDocumentId(Query query) throws IOException {
		if (DirectoryReader.indexExists(directory)==false) {return -1;}
		TopDocs topDocs = getIndexSearcher().search(query, 1);
		return topDocs.totalHits==1 ? topDocs.scoreDocs[0].doc : -1;
	}
	
	public Document getLuceneDocument(String corpusId, String documentId) throws IOException {
		int id = getLuceneDocumentId(corpusId, documentId);
		return id > -1 ? getIndexSearcher().doc(id) : null;
	}

	public int getLuceneDocumentId(String corpusId, String documentId) throws IOException {
		Query query = getCorpusDocumentQuery(corpusId, documentId);
		return getLuceneDocumentId(query);
	}
	
	public int getLuceneDocumentId(String documentId) throws IOException {
		Query query = getCorpusDocumentQuery(null, documentId);
		return getLuceneDocumentId(query);
	}
	
	public IndexSearcher getIndexSearcher() throws CorruptIndexException, IOException {
		if (indexSearcher == null) {
			indexSearcher = new IndexSearcher(getDirectoryReader());
		}
		return indexSearcher;
	}
	
	private DirectoryReader getDirectoryReader() throws CorruptIndexException, IOException {
		if (directoryReader == null) {
			directoryReader = DirectoryReader.open(directory);
		}
		return directoryReader;
		
	}
	

	public void commit() throws CorruptIndexException, LockObtainFailedException, IOException {
		getIndexWriter().commit();
		
	}
	
	public void addDocument(Document document) throws CorruptIndexException, IOException {
		document.add(new StringField("version", VERSION.name(), Field.Store.YES));
		IndexWriter writer = getIndexWriter();
		writer.addDocument(document);
		writer.commit();
		directoryReader = DirectoryReader.open(writer, false);
		indexSearcher = new IndexSearcher(directoryReader);
	}

	public void updateDocument(Term term, Document document) throws CorruptIndexException, IOException {
		document.add(new FloatField("version", luceneDocumentVersion, Field.Store.YES));
		IndexWriter writer = getIndexWriter();
		writer.addDocument(document);
		writer.commit();
		directoryReader = DirectoryReader.open(writer, false);
		indexSearcher = new IndexSearcher(directoryReader);
	}
	
	public IndexWriter getIndexWriter() throws CorruptIndexException, LockObtainFailedException, IOException {
		if (indexWriter == null) {
			indexWriter = new IndexWriter(directory , indexWriterConfig);
		}
		return indexWriter;
	}

	public IndexReader getIndexReader() throws IOException {
		return SlowCompositeReaderWrapper.wrap(directoryReader);
	}
	
	public Analyzer getAnalyzer() {
		return analyzer;
	}


}

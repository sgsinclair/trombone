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
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.CharReader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.voyanttools.trombone.lucene.analysis.KitchenSinkPerFieldAnalyzerWrapper;
import org.voyanttools.trombone.lucene.analysis.LexicalAnalyzer;
import org.voyanttools.trombone.lucene.analysis.MultiLingualStemAnalyzer;

/**
 * @author sgs
 *
 */
public class LuceneManager {
	
	private Directory directory;
	
	private DirectoryReader directoryReader = null;
	
	private IndexReader reader = null;
	
	private IndexWriter indexWriter = null;
	
	private IndexSearcher indexSearcher = null;
	
	private Version version = Version.LUCENE_40;
	private float luceneDocumentVersion = 4.0f;

	
	private Analyzer analyzer = new KitchenSinkPerFieldAnalyzerWrapper();
	
	private IndexWriterConfig indexWriterConfig = new IndexWriterConfig(version, analyzer);
	
	public LuceneManager(Directory directory) throws CorruptIndexException, IOException {
		this.directory = directory;
	}
	
	public int getLuceneDocumentId(String documentId) throws IOException {
		if (DirectoryReader.indexExists(directory)==false) {return -1;}
		TopDocs topDocs = getIndexSearcher().search(new TermQuery(new Term("id", documentId)), 1);
		return topDocs.totalHits==1 ? topDocs.scoreDocs[0].doc : -1;
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
	public int addDocument(Document document) throws CorruptIndexException, IOException {
		document.add(new FloatField("version", luceneDocumentVersion, Field.Store.YES));
		IndexWriter writer = getIndexWriter();
		writer.addDocument(document);
		reader = directoryReader.open(writer, false);
		return getLuceneDocumentId(document.getField("id").stringValue());
	}

	public IndexWriter getIndexWriter() throws CorruptIndexException, LockObtainFailedException, IOException {
		if (indexWriter == null) {
			indexWriter = new IndexWriter(directory , indexWriterConfig);
		}
		return indexWriter;
	}


}
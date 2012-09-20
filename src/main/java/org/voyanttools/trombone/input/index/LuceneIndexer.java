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
package org.voyanttools.trombone.input.index;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.CharReader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.lucene.analysis.StemmableLanguage;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class LuceneIndexer implements Indexer {
	
	private Storage storage;
	private FlexibleParameters parameters;

	public LuceneIndexer(Storage storage, FlexibleParameters parameters) {
		this.storage = storage;
		this.parameters = parameters;
	}

	public void index(List<StoredDocumentSource> storedDocumentSources) throws IOException {
		storage.getLuceneManager().getIndexWriter(); // make sure this has been initialized
		ExecutorService executor = Executors.newCachedThreadPool();
		for (StoredDocumentSource storedDocumentSource : storedDocumentSources) {
			Runnable worker = new Indexer(storage, storedDocumentSource);
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
//			executor.
		}
		storage.getLuceneManager().commit();
	}
	
	/*
	private List<ProtoIndexedDocument> getProtoIndexedDocuments(List<StoredDocumentSource> storedDocumentSources) throws CorruptIndexException, LockObtainFailedException, IOException {
		List<ProtoIndexedDocument> protoIndexedDocuments = new ArrayList<ProtoIndexedDocument>();
		storage.getLuceneManager().getIndexWriter(); // make sure this has been initialized
		ExecutorService executor = Executors.newCachedThreadPool();
		List<Future<ProtoIndexedDocument>> list = new ArrayList<Future<ProtoIndexedDocument>>();
		for (StoredDocumentSource storedDocumentSource : storedDocumentSources) {
			Callable<ProtoIndexedDocument> worker = new Indexer(storage, storedDocumentSource);
			Future<ProtoIndexedDocument> submit = executor.submit(worker);
			list.add(submit);	
		}
		try {
			for (Future<ProtoIndexedDocument> future : list) {
				protoIndexedDocuments.add(future.get());
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException("An error occurred during multi-threaded document expansion.", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("An error occurred during multi-threaded document expansion.", e);
		}
		executor.shutdown();
		storage.getLuceneManager().commit();
		return protoIndexedDocuments;
		
	}

	private List<IndexedDocument> getIndexedDocuments(List<ProtoIndexedDocument> protoIndexedDocuments) throws IOException {
		List<IndexedDocument> indexedDocuments = new ArrayList<IndexedDocument>();
		ExecutorService executor = Executors.newCachedThreadPool();
		List<Future<IndexedDocument>> list = new ArrayList<Future<IndexedDocument>>();
		for (ProtoIndexedDocument protoIndexedDocument : protoIndexedDocuments) {		
			Callable<IndexedDocument> worker = new IndexDocumentMaker(storage, protoIndexedDocument);
			Future<IndexedDocument> submit = executor.submit(worker);
			list.add(submit);	
		}
		try {
			for (Future<IndexedDocument> future : list) {
				indexedDocuments.add(future.get());
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException("An error occurred during multi-threaded document expansion.", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("An error occurred during multi-threaded document expansion.", e);
		}
		executor.shutdown();
		return indexedDocuments;
		
	}
	
	private class ProtoIndexedDocument {
		private int doc;
		private StoredDocumentSource storedDocumentSource;
		private ProtoIndexedDocument(Integer doc, StoredDocumentSource storedDocumentSource) {
			this.doc = doc;
			this.storedDocumentSource = storedDocumentSource;
		}
	}
	
	private class IndexDocumentMaker implements Callable<IndexedDocument> {

		private Storage storage;
		private ProtoIndexedDocument protoIndexedDocument;
		public IndexDocumentMaker(Storage storage,
				ProtoIndexedDocument protoIndexedDocument) {
			this.storage = storage;
			this.protoIndexedDocument = protoIndexedDocument;
		}

		@Override
		public IndexedDocument call() throws Exception {
			String id = protoIndexedDocument.storedDocumentSource.getId();
			StoredDocumentSourceStorage documentStorage = storage.getStoredDocumentSourceStorage();
			if (documentStorage.hasIndexedDocument(id)) {
				return documentStorage.getIndexedDocument(id);
			}
			int docId = protoIndexedDocument.doc > -1 ? protoIndexedDocument.doc : storage.getLuceneManager().getLuceneDocumentId(id);
			IndexReader reader = storage.getLuceneManager().getIndexReader();
			Document document = reader.document(docId)
			DocumentStoredFieldVisitor
			document.getField("a").
			storage.getLuceneManager().
			return new IndexedDocument();
		}
		
	}
	*/
	
	
	private class Indexer implements Runnable {

		private Storage storage;
		private StoredDocumentSource storedDocumentSource;
		private LuceneManager luceneManager;
		private String id;
		private String string = null;
		public Indexer(Storage storage,
				StoredDocumentSource storedDocumentSource) throws IOException {
			this.storage = storage;
			this.storedDocumentSource = storedDocumentSource;
			this.luceneManager = storage.getLuceneManager();
			this.id = storedDocumentSource.getId();
		}
		
		private String getString() throws IOException {
			if (this.string == null) {
				InputStream is = null;
				try {
					is = storage.getStoredDocumentSourceStorage().getStoredDocumentSourceInputStream(id);
					StringWriter sw = new StringWriter();
					IOUtils.copy(is, sw);
					string = sw.toString();
				}
				finally {
					if (is!=null) is.close();
				}
			}
			return string;
		}
		
		@Override
		public void run()  {
			
			try {
				int index = luceneManager.getLuceneDocumentId(id);
				if (index>-1) return; // new ProtoIndexedDocument(index, storedDocumentSource); // got it, let's bail
					

				FieldType ft = new FieldType(TextField.TYPE_STORED);
				ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
				ft.setStoreTermVectors(true);
				ft.setStoreTermVectorOffsets(true);
				ft.setStoreTermVectorPositions(true);
				
				Document document = new Document();

				// create lexical document
				document = new Document();
				document.add(new StoredField("id", id+"-lexical"));
				document.add(new Field("lexical", getString(), ft));
				
				
				if (storedDocumentSource.getMetadata().getLanguageCode().equals("en")) {
					// FIXME: deal with other lemmatization languages
					document.add(new Field("lemmatized-en", getString(), ft));
				}
				else {
					// next look for stemmed index if needed
					String lang = storedDocumentSource.getMetadata().getLanguageCode();
					StemmableLanguage stemmableLanguage = StemmableLanguage.fromCode(lang);
					if (stemmableLanguage!=null) {
						document.add(new Field("stemmed-"+lang, getString(), ft));		
					}
				}
				
				luceneManager.addDocument(document);
			}
			catch (IOException e) {
				throw new RuntimeException("Unable to index stored document: "+storedDocumentSource, e);
			}
		}
		
	}
}

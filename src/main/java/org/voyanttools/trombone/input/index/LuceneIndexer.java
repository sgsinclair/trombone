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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
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

	public String index(List<StoredDocumentSource> storedDocumentSources) throws IOException {
		
		List<String> ids = new ArrayList<String>();
		for (StoredDocumentSource storedDocumentSource : storedDocumentSources) {
			ids.add(storedDocumentSource.getId());
		}
		String corpusId = storage.storeStrings(ids);
		
		// determine if we need to modify the Lucene index
		boolean directoryExists = storage.getLuceneManager().directoryExists();
		IndexSearcher indexSearcher = directoryExists ? storage.getLuceneManager().getIndexSearcher() : null;
		TopDocs topDocs;
		Collection<StoredDocumentSource> storedDocumentSourceForLucene = new ArrayList<StoredDocumentSource>();
		for (StoredDocumentSource storedDocumentSource : storedDocumentSources) {
			if (!directoryExists) {
				storedDocumentSourceForLucene.add(storedDocumentSource);
			}
			else {
				// look for corpus and document (if corpus isn't there, we still need to update doc)
				topDocs = indexSearcher.search(LuceneManager.getCorpusDocumentQuery(corpusId, storedDocumentSource.getId()), 1);
				if (topDocs.totalHits==0) { // not found
					storedDocumentSourceForLucene.add(storedDocumentSource);
				}
			}
		}
		
		if (storedDocumentSourceForLucene.isEmpty()==false) {
			
			// index documents (or at least add corpus to document if not already there), we need to get a new writer
			IndexWriter indexWriter = storage.getLuceneManager().getIndexWriter();
			DirectoryReader indexReader = DirectoryReader.open(indexWriter, true);
			indexSearcher = new IndexSearcher(indexReader);		
			boolean verbose = parameters.getParameterBooleanValue("verbose");
			int processors = Runtime.getRuntime().availableProcessors();
			ExecutorService executor;
			
			// index
			executor = Executors.newFixedThreadPool(processors);
			for (StoredDocumentSource storedDocumentSource : storedDocumentSourceForLucene) {
				Runnable worker = new StoredDocumentSourceIndexer(storage, indexWriter, indexSearcher, storedDocumentSource, corpusId, verbose);
				executor.execute(worker);
			}
			executor.shutdown();
			try {
				executor.awaitTermination(10, TimeUnit.MINUTES); // max 10 minutes run time
			} catch (InterruptedException e) {
				throw new RuntimeException("Lucene indexing has run out of time", e);
			}
			finally {
				indexWriter.commit();
				indexReader = DirectoryReader.open(indexWriter, true);
				storage.getLuceneManager().setDirectoryReader(indexReader); // make sure it's available afterwards				
			}
			
			// now determine which documents need to be analyzed
			Collection<StoredDocumentSource> storedDocumentSourceForAnalysis = new ArrayList<StoredDocumentSource>();
			for (StoredDocumentSource storedDocumentSource : storedDocumentSourceForLucene) {
				if (storedDocumentSource.getMetadata().getLastTokenPositionIndex(TokenType.lexical)==0) { // don't re-analyze
					storedDocumentSourceForAnalysis.add(storedDocumentSource);
				}
			}
			
			if (storedDocumentSourceForAnalysis.isEmpty()==false) {
				indexSearcher = new IndexSearcher(indexReader);
				executor = Executors.newFixedThreadPool(processors);
				for (StoredDocumentSource storedDocumentSource : storedDocumentSourceForAnalysis) {
					if (storedDocumentSource.getMetadata().getLastTokenPositionIndex(TokenType.lexical)==0) { // don't re-analyze
						Runnable worker = new IndexedDocumentAnalyzer(storage, indexSearcher, storedDocumentSource, corpusId, verbose);
						executor.execute(worker);
					}
				}
				executor.shutdown();
				try {
					executor.awaitTermination(100, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					throw new RuntimeException("Lucene document analysis run out of time", e);
				}
			}
			
			
		}
		
		return corpusId;
		
	}
	
	private class IndexedDocumentAnalyzer implements Runnable {
		
		private Storage storage;
		private StoredDocumentSource storedDocumentSource;
		private IndexReader indexReader;
		private IndexSearcher indexSearcher;
		private String corpusId;
		private String id;
		private boolean verbose;
		public IndexedDocumentAnalyzer(Storage storage, IndexSearcher indexSearcher,
				StoredDocumentSource storedDocumentSource, String corpusId, boolean verbose) throws IOException {
			this.storage = storage;
			this.indexReader = indexSearcher.getIndexReader();
			this.indexSearcher = indexSearcher;
			this.storedDocumentSource = storedDocumentSource;
			this.corpusId = corpusId;
			this.id = storedDocumentSource.getId();
			this.verbose = verbose;
		}

		@Override
		public void run() {

			
			if (verbose) {
				System.out.println("analyzing indexed document "+storedDocumentSource.getMetadata());
			}
			
			Query query = LuceneManager.getCorpusDocumentQuery(corpusId,  id);
			TopDocs topDocs;
			
			try {
				topDocs = indexSearcher.search(query, 1);
				int docId = topDocs.scoreDocs[0].doc;
				Terms terms = indexReader.getTermVector(docId, "lexical");
				int totalTokens = 0;
				int totalTypes =  0;
				int lastOffset = 0;
				int lastPosition = 0;
				if (terms!=null) {
					TermsEnum termsEnum = terms.iterator(null);
					DocsAndPositionsEnum docsAndPositionsEnum = null;
					while (true) {
						BytesRef term = termsEnum.next();
						if (term!=null) {
							totalTypes++;
							docsAndPositionsEnum = termsEnum.docsAndPositions(new Bits.MatchAllBits(indexReader.maxDoc()), docsAndPositionsEnum, DocsAndPositionsEnum.FLAG_OFFSETS);
							while (true) {
								int doc = docsAndPositionsEnum.nextDoc();
								if (doc!=DocsAndPositionsEnum.NO_MORE_DOCS) {
									int freq = docsAndPositionsEnum.freq();
									totalTokens+=freq;
									for (int i=0; i<freq; i++) {
										int pos = docsAndPositionsEnum.nextPosition();
										if (pos>lastPosition) {lastPosition=pos;}
										int offset = docsAndPositionsEnum.startOffset();
										if (offset>lastOffset) {lastOffset=offset;}
									}
								}
								else {break;}
							}
						}
						else {break;}
					}
				}
				DocumentMetadata metadata = storedDocumentSource.getMetadata();
				metadata.setTypesCount(TokenType.lexical, totalTypes);
				metadata.setTokensCount(TokenType.lexical, totalTokens);
				metadata.setLastTokenPositionIndex(TokenType.lexical, lastPosition);
				metadata.setLastTokenOffsetIndex(TokenType.lexical, lastOffset);
				storage.getStoredDocumentSourceStorage().updateStoredDocumentSourceMetadata(id, metadata);
			} catch (IOException e) {
				throw new RuntimeException("Unable to query document during index analysis.", e);
			}
		}
		
	}
	
	private class StoredDocumentSourceIndexer implements Runnable {

		private Storage storage;
		private StoredDocumentSource storedDocumentSource;
		private IndexWriter indexWriter;
		private IndexSearcher indexSearcher;
		private LuceneManager luceneManager;
		private String corpusId;
		private String id;
		private String string = null;
		private boolean verbose;
		public StoredDocumentSourceIndexer(Storage storage, IndexWriter indexWriter, IndexSearcher indexSearcher,
				StoredDocumentSource storedDocumentSource, String corpusId, boolean verbose) throws IOException {
			this.storage = storage;
			this.indexWriter = indexWriter;
			this.indexSearcher = indexSearcher;
			this.storedDocumentSource = storedDocumentSource;
			this.luceneManager = storage.getLuceneManager();
			this.corpusId = corpusId;
			this.id = storedDocumentSource.getId();
			this.verbose = verbose;
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
			
			if (verbose) {
				System.out.println("indexing "+storedDocumentSource.getMetadata());
			}
			
			try {
				
				Query query = LuceneManager.getDocumentQuery(id);
				TopDocs topDocs = indexSearcher.search(query, 1);
				Document document;
				if (topDocs.totalHits>0) {
					document = indexSearcher.doc(topDocs.scoreDocs[0].doc);
					// check to see if this corpus is already part of the document, and add it if not
					if (!Arrays.asList(document.getValues("corpus")).contains(corpusId)) {
						document.add(new StringField("corpus", corpusId, Field.Store.YES));
						indexWriter.updateDocument(new Term("id", id), document);
					}
					return;
				}
					

				FieldType ft = new FieldType(TextField.TYPE_STORED);
				ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
				ft.setStoreTermVectors(true);
				ft.setStoreTermVectorOffsets(true);
				ft.setStoreTermVectorPositions(true);
				
				document = new Document();

				// create lexical document
				document = new Document();
				document.add(new StringField("id", id, Field.Store.YES));
				document.add(new StringField("corpus", corpusId, Field.Store.YES));
				document.add(new StringField("version", LuceneManager.VERSION.name(), Field.Store.YES));
				document.add(new Field("lexical", getString(), ft));
//				System.err.println(id+": "+getString());
				
				for (Map.Entry<Object, Object> entries : storedDocumentSource.getMetadata().getProperties().entrySet()) {
					String key = (String) entries.getKey();
					String value = (String) entries.getValue();
					if (key!=null && value!=null && value.isEmpty()==false) {
						document.add(new TextField(key, value, Field.Store.YES));
					}
				}
				
				
				// TODO: add lemmatization
				/*
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
				*/
				
				indexWriter.addDocument(document);
				
			}
			catch (IOException e) {
				throw new RuntimeException("Unable to index stored document: "+storedDocumentSource, e);
			}
		}
		
	}
}

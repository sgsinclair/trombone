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
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.lucene.analysis.StemmableLanguage;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.model.TokenType;
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
		
		int processors = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(processors);
		boolean verbose = parameters.getParameterBooleanValue("verbose");
		for (StoredDocumentSource storedDocumentSource : storedDocumentSources) {
			Runnable worker = new Indexer(storage, storedDocumentSource, verbose);
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
//			executor.
		}
		storage.getLuceneManager().commit();
	}
	
	private class Indexer implements Runnable {

		private Storage storage;
		private StoredDocumentSource storedDocumentSource;
		private LuceneManager luceneManager;
		private String id;
		private String string = null;
		private boolean verbose;
		public Indexer(Storage storage,
				StoredDocumentSource storedDocumentSource, boolean verbose) throws IOException {
			this.storage = storage;
			this.storedDocumentSource = storedDocumentSource;
			this.luceneManager = storage.getLuceneManager();
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
				document.add(new StringField("id", id, Field.Store.YES));
				document.add(new Field("lexical", getString(), ft));
//				System.err.println(id+": "+getString());
				
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
				
				luceneManager.addDocument(document);
				
				int docId = luceneManager.getLuceneDocumentId(id);
				IndexReader reader = luceneManager.getIndexReader();
				Terms terms = reader.getTermVector(docId, "lexical");
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
							docsAndPositionsEnum = termsEnum.docsAndPositions(MultiFields.getLiveDocs(reader), docsAndPositionsEnum, DocsAndPositionsEnum.FLAG_OFFSETS);
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
				

			}
			catch (IOException e) {
				throw new RuntimeException("Unable to index stored document: "+storedDocumentSource, e);
			}
		}
		
	}
}

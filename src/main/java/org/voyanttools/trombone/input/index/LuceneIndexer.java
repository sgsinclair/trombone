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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.lucene.LucenePackage;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.InputStreamInputSource;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TextUtils;

/**
 * @author sgs
 *
 */
public class LuceneIndexer implements Indexer {
	
	private static int VERSION = 4; // helpful for setting unique version of
	// document based not only on Lucene version but also this code,
	// the actual number doesn't matter but will usually just
	// increment to uniqueness

	
	private Storage storage;
	private FlexibleParameters parameters;

	public LuceneIndexer(Storage storage, FlexibleParameters parameters) {
		this.storage = storage;
		this.parameters = parameters;
	}

	public String index(List<StoredDocumentSource> storedDocumentSources) throws IOException {
		
		// let's check if we need to create new sources because of tokenization parameters
		if (parameters.getParameterValue("tokenization", "").isEmpty()==false || parameters.getParameterValue("language", "").isEmpty()==false) {
			StoredDocumentSourceStorage sourceDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
			String tokenizationParam = parameters.getParameterValue("tokenization","");
			String langParam = parameters.getParameterValue("language","");
			for (int i=0, len=storedDocumentSources.size(); i<len; i++) {
				StoredDocumentSource storedDocumentSource = storedDocumentSources.get(i);
				String id = storedDocumentSource.getId();
				String newId = DigestUtils.md5Hex(id+tokenizationParam+langParam);
				InputStream inputStream = sourceDocumentSourceStorage.getStoredDocumentSourceInputStream(id);
				DocumentMetadata metadata = storedDocumentSource.getMetadata();
				metadata.setLastTokenPositionIndex(TokenType.lexical, 0); // this is crucial to ensure that document is re-analyzed and metadata re-rewritten
				InputSource inputSource = new InputStreamInputSource(newId, metadata, inputStream);
				storedDocumentSources.set(i, sourceDocumentSourceStorage.getStoredDocumentSource(inputSource));
				inputStream.close();
			}
		}
		
		List<String> ids = new ArrayList<String>();
		for (StoredDocumentSource storedDocumentSource : storedDocumentSources) {
			ids.add(storedDocumentSource.getId());
		}
		String corpusId = storage.storeStrings(ids, Storage.Location.object);
		
		// determine if we need to modify the Lucene index
		Collection<StoredDocumentSource> storedDocumentSourceForLucene = new ArrayList<StoredDocumentSource>();
		if (storage.getLuceneManager().directoryExists(corpusId)) {
			DirectoryReader dr = storage.getLuceneManager().getDirectoryReader(corpusId);
			
			// collect the id terms from each leaf
			List<TermsEnum> idTerms = new ArrayList<TermsEnum>();
			for (LeafReaderContext rc : dr.leaves()) {
				LeafReader reader = rc.reader();
				Terms terms = reader.terms("id");
				if (terms!=null) {
					idTerms.add(terms.iterator());
				}
			}
			
			// test new ids against leaf ids
			if (idTerms.isEmpty()) {
				storedDocumentSourceForLucene.addAll(storedDocumentSources);
			} else {
				for (StoredDocumentSource storedDocumentSource : storedDocumentSources) {
					String id = storedDocumentSource.getId();
					boolean match = false;
					for (TermsEnum termsEnum : idTerms) {
						if (termsEnum.seekExact(new BytesRef(id))) {
							match = true;
							break;
						}
					}
					if (!match) {
						storedDocumentSourceForLucene.add(storedDocumentSource);
					}
				}	
			}
		}
		else {
			storedDocumentSourceForLucene.addAll(storedDocumentSources);
		}
		
		if (storedDocumentSourceForLucene.isEmpty()==false) {
//			Temporal start = Instant.now();
//			System.out.println(start);
//			if (parameters.getParameterBooleanValue("stream")) {
//				System.out.println("stream");
//				indexStream(storedDocumentSourceForLucene, corpusId);
//			} else {
//				System.out.println("executor");
				indexExecutorService(storedDocumentSourceForLucene, corpusId);
//			}
//			Temporal end = Instant.now();
//			ChronoUnit.SECONDS.between(start, end);
//			System.out.println(ChronoUnit.SECONDS.between(start, end));
		}
		
		
		return corpusId;
		
	}
	private void indexStream(Collection<StoredDocumentSource> storedDocumentSourceForLucene, String corpusId) throws CorruptIndexException, LockObtainFailedException, IOException {
		// index documents (or at least add corpus to document if not already there), we need to get a new writer
		IndexWriter indexWriter = storage.getLuceneManager().getIndexWriter(corpusId);
		DirectoryReader indexReader = DirectoryReader.open(indexWriter);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);		
		boolean verbose = parameters.getParameterBooleanValue("verbose");
		try {
			storedDocumentSourceForLucene.parallelStream().forEach(storedDocumentSource -> {
				Runnable runnable;
				try {
					runnable = new StoredDocumentSourceIndexer(storage, indexWriter, indexSearcher, storedDocumentSource, corpusId, verbose);
					runnable.run();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (parameters.containsKey("forceMerge")) {
			indexWriter.forceMerge(parameters.getParameterIntValue("forceMerge"));
		}
		
		indexReader = DirectoryReader.open(indexWriter);
		storage.getLuceneManager().setDirectoryReader(corpusId, indexReader); // make sure it's available afterwards				

		
		// now determine which documents need to be analyzed
		Collection<StoredDocumentSource> storedDocumentSourceForAnalysis = new ArrayList<StoredDocumentSource>();
		for (StoredDocumentSource storedDocumentSource : storedDocumentSourceForLucene) {
			if (storedDocumentSource.getMetadata().getLastTokenPositionIndex(TokenType.lexical)==0) { // don't re-analyze
				storedDocumentSourceForAnalysis.add(storedDocumentSource);
			}
		}
		
		if (storedDocumentSourceForAnalysis.isEmpty()==false) {
			IndexSearcher indexSearcher2 = new IndexSearcher(indexReader);		
			try {
				storedDocumentSourceForAnalysis.parallelStream().forEach(storedDocumentSource -> {
					if (storedDocumentSource.getMetadata().getLastTokenPositionIndex(TokenType.lexical)==0) { // don't re-analyze
						Runnable worker;
						try {
							worker = new IndexedDocumentAnalyzer(storage, indexSearcher2, storedDocumentSource, corpusId, verbose);
							worker.run();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	private void indexExecutorService(Collection<StoredDocumentSource> storedDocumentSourceForLucene, String corpusId) throws CorruptIndexException, LockObtainFailedException, IOException {
		// index documents (or at least add corpus to document if not already there), we need to get a new writer
		IndexWriter indexWriter = storage.getLuceneManager().getIndexWriter(corpusId);
		DirectoryReader indexReader = DirectoryReader.open(indexWriter);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);		
		boolean verbose = parameters.getParameterBooleanValue("verbose");
		int processors = Runtime.getRuntime().availableProcessors();
		ExecutorService executor;
		
		// index
		executor = Executors.newFixedThreadPool(processors);
		for (StoredDocumentSource storedDocumentSource : storedDocumentSourceForLucene) {
			Runnable worker = new StoredDocumentSourceIndexer(storage, indexWriter, indexSearcher, storedDocumentSource, corpusId, verbose);
			try {
				executor.execute(worker);
			} catch (Exception e) {
				executor.shutdown();
				throw e;
			}
		}
		executor.shutdown();
		try {
			if (!executor.awaitTermination(parameters.getParameterIntValue("luceneIndexingTimeout", 60*10), TimeUnit.SECONDS)) { // default 10 minutes
				executor.shutdownNow();
				throw new InterruptedException("Lucene indexing has run out of time.");
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
			throw new RuntimeException("Lucene indexing has been interrupted.", e);
		}
		finally {
			
			try {
				indexWriter.commit();
			}
			catch (IOException e) {
				indexWriter.close(); // this may also throw an exception, but docs say to close on commit error
				throw e;
			}
		}

		if (parameters.containsKey("forceMerge")) {
			indexWriter.forceMerge(parameters.getParameterIntValue("forceMerge"));
		}
		
		indexReader = DirectoryReader.open(indexWriter);
		storage.getLuceneManager().setDirectoryReader(corpusId, indexReader); // make sure it's available afterwards				

		
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
				if (!executor.awaitTermination(parameters.getParameterIntValue("luceneAnalysisTimeout", 60*10), TimeUnit.SECONDS)) { // default 10 minutes
					throw new InterruptedException("Lucene analysis has run out of time.");
				}
			} catch (InterruptedException e) {
				throw new RuntimeException("Lucene document analysis run out of time", e);
			}
		}
	
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
//				System.out.println("analyzing indexed document "+storedDocumentSource.getMetadata());
			}
			
			Query query = new TermQuery(new Term("id", id)); 
			TopDocs topDocs;
			
			try {
				topDocs = indexSearcher.search(query, 1); // there may be multiple documents in the index but they should have the same text
				int docId = topDocs.scoreDocs[0].doc;
				Terms terms = indexReader.getTermVector(docId, "lexical");
				int totalTokens = 0;
				int totalTypes =  0;
				int lastOffset = 0;
				int lastPosition = 0;
				DescriptiveStatistics stats = new DescriptiveStatistics();
				if (terms!=null) {
					TermsEnum termsEnum = terms.iterator();
					while (true) {
						BytesRef term = termsEnum.next();
						if (term!=null) {
							totalTypes++;
							PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.OFFSETS);
							while (true) {
								int doc = postingsEnum.nextDoc();
								if (doc!=PostingsEnum.NO_MORE_DOCS) {
									int freq = postingsEnum.freq();
									stats.addValue(freq);
									totalTokens+=freq;
									for (int i=0; i<freq; i++) {
										int pos = postingsEnum.nextPosition();
										if (pos>lastPosition) {lastPosition=pos;}
										int offset = postingsEnum.startOffset();
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
				metadata.setTypesCountMean(TokenType.lexical, (float) stats.getMean());
				metadata.setTypesCountStdDev(TokenType.lexical, (float) stats.getStandardDeviation());
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
//				System.out.println("indexing "+storedDocumentSource.getMetadata());
			}
			
			try {
				
				TopDocs topDocs = indexSearcher.search(new TermQuery(new Term("id", id)), 1);
				if (topDocs.totalHits.value>0) { // already indexed
					return;
				}
					

				// this is used by lexical and the metadata (expecting term vectors to be present)
				FieldType ft = new FieldType(TextField.TYPE_NOT_STORED);
				ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
				ft.setStoreTermVectors(true);
				ft.setStoreTermVectorOffsets(true);
				ft.setStoreTermVectorPositions(true);
				
				Document document = new Document();

				// create lexical document
				document = new Document();
				document.add(new StringField("id", id, Field.Store.NO));
//				document.add(new StringField("corpus", corpusId, Field.Store.NO));
				document.add(new StringField("version",  LucenePackage.get().getImplementationVersion()+"-"+String.valueOf(LuceneIndexer.VERSION), Field.Store.YES));
				
				FlexibleParameters p = new FlexibleParameters();
				p.setParameter("language", storedDocumentSource.getMetadata().getLanguageCode());
				if (parameters.getParameterValue("tokenization", "").isEmpty()==false) {
					p.setParameter("tokenization", parameters.getParameterValue("tokenization"));
				}
				document.add(new Field("lexical", getString() + "<!-- "+ p.getAsQueryString()+" -->", ft));
//				System.err.println(id+": "+getString());
				
				FlexibleParameters params = storedDocumentSource.getMetadata().getFlexibleParameters();
				FacetsConfig config = new FacetsConfig();
				for (String key : params.getKeys()) {
					// store term vector so that we can build term DB, combine multiple values into one
					String v = StringUtils.join(params.getParameterValues(key), " ");
					if (v!=null && v.trim().isEmpty()==false) {
						document.add(new Field(key, v, ft));
					}
					for (String value : params.getParameterValues(key)) {
						String facet = "facet."+key;
						config.setMultiValued(facet, true);
						config.setIndexFieldName(key, facet);
						if (value.trim().isEmpty()==false) {
							// store as facet field
							document.add(new SortedSetDocValuesFacetField(facet, value));
						}
					}
				}
				
				if (parameters.getParameterBooleanValue("lemmatize")) {
					// pass in parameters, including language, used by lemmatizer
					document.add(new Field("lemma", getString() + "<!-- "+ p.getAsQueryString()+" -->", ft));
				}
				
				if (parameters.getParameterBooleanValue("stem")) {
					// pass in parameters, including language, used by lemmatizer
					document.add(new Field("stem", getString() + "<!-- "+ p.getAsQueryString()+" -->", ft));
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
				
				// approximate the number of sentences
				List<String> sentences = TextUtils.getSentences(getString(), storedDocumentSource.getMetadata().getLanguageCode());
				storedDocumentSource.getMetadata().setSentencesCount(sentences.size());
				
				indexWriter.addDocument(config.build(document));
				
			}
			catch (IOException e) {
				throw new RuntimeException("Unable to index stored document: "+storedDocumentSource, e);
			}
		}
		
	}
}

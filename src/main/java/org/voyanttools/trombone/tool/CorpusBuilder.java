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
package org.voyanttools.trombone.tool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.index.IndexReader;
import org.voyanttools.trombone.document.Metadata;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.InputStreamInputSource;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class CorpusBuilder extends AbstractTool {

	private String storedId = null;

	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusBuilder(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.RunnableTool#run()
	 */
	@Override
	public void run() throws IOException {
		String sid = parameters.getParameterValue("storedId");
		List<String> ids = storage.retrieveStrings(sid);
		StoredDocumentSourceStorage storedDocumentStorage = storage.getStoredDocumentSourceStorage();
		List<StoredDocumentSource> indexableStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		for (String id : ids) {
			Metadata metadata = storedDocumentStorage.getStoredDocumentSourceMetadata(id);
			StoredDocumentSource storedDocumentSource = new StoredDocumentSource(id, metadata);
			indexableStoredDocumentSources.add(storedDocumentSource);
		}
		run(indexableStoredDocumentSources);
	}
	
	void run(List<StoredDocumentSource> storedDocumentSources) throws IOException {
		
		// first see if we can load an existing corpus
		if (parameters.containsKey("corpus")) {
			Corpus corpus = storage.getCorpus(parameters.getParameterValue("corpus"));
			if (corpus!=null) {
				
				// build a hash set of the ids to check against the corpus
				Set<String> ids = new HashSet<String>();
				for (StoredDocumentSource sds : storedDocumentSources) {
					ids.add(sds.getId());
				}
				
				// add documents that aren't in the corpus already
				List<StoredDocumentSource> corpusStoredDocumentSources = new ArrayList<StoredDocumentSource>();
				boolean overlap = true;
				for (IndexedDocument document : corpus) {
					if (ids.contains(document.getId())==false) {
						overlap = false;
						corpusStoredDocumentSources.add(document.asStoredDocumentSource());
					}
				}
				
				// we have overlap and the two sets are the same size, so just use the current corpus
				if (overlap && ids.size() == corpus.size()) {
					storedId = parameters.getParameterValue("corpus");
					return;
				}
				
				// we're adding document to an existing corpus, so prepend the corpus documents that aren't here
				storedDocumentSources.addAll(0, corpusStoredDocumentSources);
			}
		}
		
		// now let's try to create a single document from all stored documents
		StringBuilder idsString = new StringBuilder("corpus");
		for (StoredDocumentSource storedDocumentSource : storedDocumentSources) {
			idsString.append(storedDocumentSource.getId());
		}
		
		String storedId = DigestUtils.md5Hex(idsString.toString());
		Corpus corpus = storage.getCorpus(storedId);
		
		// corpus doesn't exist, so create it
		if (corpus.size()==0) {
			StoredDocumentSourceStorage docStorage = storage.getStoredDocumentSourceStorage();
			StringWriter writer = new StringWriter();
			writer.write("<corpus>");
			for (StoredDocumentSource storedDocumentSource : storedDocumentSources) {
				InputStream is = null;
				try {
					is = docStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
					IOUtils.copy(is, writer);
				}
				finally {
					if (is!=null) is.close();
				}
			}
			writer.write("</corpus>");
			
			InputStream stream = new ByteArrayInputStream(writer.toString().getBytes("UTF-8"));
			Metadata metadata = new Metadata();
			InputSource inputSource = new InputStreamInputSource(storedId, metadata, stream);
			
			// store document
			StoredDocumentSource storedDocumentSource = docStorage.getStoredDocumentSource(inputSource);
			
			// store list of documents
			docStorage.setMultipleExpandedStoredDocumentSources(storedId, storedDocumentSources);
			
			// index
			List<StoredDocumentSource> indexableStoredDocumentSources = new ArrayList<StoredDocumentSource>();
			indexableStoredDocumentSources.add(storedDocumentSource);
			DocumentIndexer indexer = new DocumentIndexer(storage, parameters);
			indexer.run(indexableStoredDocumentSources);
			
			
		}
		
		
		
//		
//		IndexReader reader = storage.getLuceneManager().getIndexReader();
//		reader.d
//		ExecutorService executor = Executors.newCachedThreadPool();
//		List<Future<IndexedDocument>> list = new ArrayList<Future<IndexedDocument>>();
//		for (StoredDocumentSource storedDocumentSource : storedDocumentSources) {		
//			Callable<Map<String, Integer>> worker = new IndexDocumentMaker(storage, protoIndexedDocument);
//			Future<IndexedDocument> submit = executor.submit(worker);
//			list.add(submit);	
//		}
//		try {
//			for (Future<IndexedDocument> future : list) {
//				indexedDocuments.add(future.get());
//			}
//		} catch (InterruptedException e) {
//			throw new IllegalStateException("An error occurred during multi-threaded document expansion.", e);
//		} catch (ExecutionException e) {
//			throw new IllegalStateException("An error occurred during multi-threaded document expansion.", e);
//		}
//		executor.shutdown();

	}

	public String getStoredId() {
		// TODO Auto-generated method stub
		return null;
	}

}

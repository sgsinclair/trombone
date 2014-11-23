package org.voyanttools.trombone.model;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.input.index.LuceneIndexer;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class CorpusTermMinimalsDBTest {

	@Test
	public void test() throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();
		InputSource one = new StringInputSource("dark and stormy night in document one");
		InputSource two = new StringInputSource("It was a dark and stormy night.");
		InputSource three = new StringInputSource("It was the best of times it was the worst of times.");
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		List<StoredDocumentSource> storedDocumentSources = new ArrayList<StoredDocumentSource>();
		storedDocumentSources.add(storedDocumentSourceStorage.getStoredDocumentSource(one));
		LuceneIndexer luceneIndexer = new LuceneIndexer(storage, new FlexibleParameters());
		luceneIndexer.index(storedDocumentSources);
		storedDocumentSources.clear();
		storedDocumentSources.add(storedDocumentSourceStorage.getStoredDocumentSource(two));
		storedDocumentSources.add(storedDocumentSourceStorage.getStoredDocumentSource(three));
		String id = luceneIndexer.index(storedDocumentSources);
		CorpusMetadata metadata = new CorpusMetadata(id);
		List<String> ids = new ArrayList<String>();
		for (StoredDocumentSource storedDocumentSource : storedDocumentSources) {ids.add(storedDocumentSource.getId());}
		metadata.setDocumentIds(ids);
		Corpus corpus = new Corpus(storage, metadata);
		CorpusMapper corpusMapper = new CorpusMapper(storage, corpus);
		CorpusTermMinimalsDB corpusTermMinimalsDB = null;
		try {
			corpusTermMinimalsDB = CorpusTermMinimalsDB.getInstance(corpusMapper, TokenType.lexical);
			assertNull(corpusTermMinimalsDB.get("document")); // from first document added, not in this corpus
			assertEquals(1, corpusTermMinimalsDB.get("night").getRawFreq());
			assertEquals(3, corpusTermMinimalsDB.get("was").getRawFreq());
		}
		finally {
			if (corpusTermMinimalsDB!=null) {corpusTermMinimalsDB.close();}
			
		}
	}

}

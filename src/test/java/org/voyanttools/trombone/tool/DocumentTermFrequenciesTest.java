package org.voyanttools.trombone.tool;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.junit.Test;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.model.DocumentTermFrequencyStats;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

public class DocumentTermFrequenciesTest {

	@Test
	public void test() throws IOException {
		Storage storage = new MemoryStorage();
		Document document;
		LuceneManager luceneManager = storage.getLuceneManager();
		document = new Document();
		document.add(new TextField("lexical", "dark and stormy night in document one", Field.Store.YES));
		luceneManager.addDocument(document);
		DocumentTermFrequencyStats stats;
		
		FlexibleParameters parameters;
		
		parameters = new FlexibleParameters();
		parameters.addParameter("string",  "It was a dark and stormy night.");
		parameters.addParameter("string", "It was the best of times it was the worst of times.");
		parameters.addParameter("tool", "StepEnabledIndexedCorpusCreator");

		StepEnabledIndexedCorpusCreator creator = new StepEnabledIndexedCorpusCreator(storage, parameters);
		creator.run();
		parameters.setParameter("corpus", creator.getStoredId());
		
		parameters.setParameter("tool", "DocumentTermFrequencies");
		
		DocumentTermFrequencies documentTermFrequencies;
		List<DocumentTermFrequencyStats> statsList;
		
		parameters.setParameter("query", "dar*");
		documentTermFrequencies = new DocumentTermFrequencies(storage, parameters);
		documentTermFrequencies.run();		
		statsList = documentTermFrequencies.getDocumentTermFrequencyStats();
		assertEquals(1, statsList.size());
		stats = statsList.get(0);
		assertEquals("dark", stats.getTerm());
		assertEquals(1, stats.getRawFrequency());
		assertEquals(0, stats.getDocumentIndex());
		
		parameters.setParameter("query", "it was");
		documentTermFrequencies = new DocumentTermFrequencies(storage, parameters);
		documentTermFrequencies.run();		
		// we sort by reverse frequency by default
		statsList = documentTermFrequencies.getDocumentTermFrequencyStats();
		assertEquals(2, statsList.size());
		stats = statsList.get(0);
		assertEquals(1, stats.getDocumentIndex());
		assertEquals("it was", stats.getTerm());
		assertEquals(2, stats.getRawFrequency());
		stats = statsList.get(1);
		assertEquals(0, stats.getDocumentIndex());
		assertEquals("it was", stats.getTerm());
		assertEquals(1, stats.getRawFrequency());
		
		parameters.removeParameter("query");
		documentTermFrequencies = new DocumentTermFrequencies(storage, parameters);
		documentTermFrequencies.run();		
		// we sort by reverse frequency by default
		statsList = documentTermFrequencies.getDocumentTermFrequencyStats();
//		assertEquals(2, statsList.size());
//		stats = statsList.get(0);
//		assertEquals(1, stats.getDocumentIndex());
//		assertEquals("it was", stats.getTerm());
//		assertEquals(2, stats.getRawFrequency());
//		stats = statsList.get(1);
//		assertEquals(0, stats.getDocumentIndex());
//		assertEquals("it was", stats.getTerm());
//		assertEquals(1, stats.getRawFrequency());
		
		
	}

}

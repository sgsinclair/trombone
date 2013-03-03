package org.voyanttools.trombone.tool;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.junit.Test;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

public class CorpusTermFrequenciesTest {

	@Test
	public void test() throws IOException {
		Storage storage = new MemoryStorage();
		Document document;
		LuceneManager luceneManager = storage.getLuceneManager();
		document = new Document();
		document.add(new TextField("lexical", "dark and stormy night in document one", Field.Store.YES));
		luceneManager.addDocument(document);
		
		FlexibleParameters parameters;
		
		parameters = new FlexibleParameters();
		parameters.addParameter("string",  "It was a dark and stormy night.");
		parameters.addParameter("string", "It was the best of times it was the worst of times.");
		parameters.addParameter("tool", "StepEnabledIndexedCorpusCreator");

		StepEnabledIndexedCorpusCreator creator = new StepEnabledIndexedCorpusCreator(storage, parameters);
		creator.run();
		parameters.setParameter("corpus", creator.getStoredId());
		
		parameters.setParameter("tool", "CorpusTermFrequencies");
		
		CorpusTerm stats;
		CorpusTermsCounter corpusTermFrequencies;
		List<CorpusTerm> statsList;
		
		// all terms 
		corpusTermFrequencies = new CorpusTermsCounter(storage, parameters);
		corpusTermFrequencies.run();
		statsList = corpusTermFrequencies.getCorpusTermFrequencyStats();
		assertEquals(15, statsList.size());
		stats = statsList.get(0);
		assertEquals("it", stats.getTerm());
		assertEquals(3, stats.getRawFrequency());
		
		// limit 1 (top frequency word)
		parameters.setParameter("limit", "1");
		corpusTermFrequencies = new CorpusTermsCounter(storage, parameters);
		corpusTermFrequencies.run();
		statsList = corpusTermFrequencies.getCorpusTermFrequencyStats();
		assertEquals(1, statsList.size());
		stats = statsList.get(0);
		assertEquals("it", stats.getTerm());
		assertEquals(3, stats.getRawFrequency());

		// start 1, limit 1
		parameters.setParameter("start", "1");
		corpusTermFrequencies = new CorpusTermsCounter(storage, parameters);
		corpusTermFrequencies.run();
		statsList = corpusTermFrequencies.getCorpusTermFrequencyStats();
		assertEquals(1, statsList.size());
		stats = statsList.get(0);
		assertEquals("was", stats.getTerm());
		assertEquals(3, stats.getRawFrequency());

		// start 50, limit 1 (empty)
		parameters.setParameter("start", "50");
		corpusTermFrequencies = new CorpusTermsCounter(storage, parameters);
		corpusTermFrequencies.run();
		statsList = corpusTermFrequencies.getCorpusTermFrequencyStats();
		assertEquals(0, statsList.size());
		
		storage.destroy();
		
	}

}

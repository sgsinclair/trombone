package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.junit.Test;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.corpus.CorpusCreator;
import org.voyanttools.trombone.tool.corpus.DocumentContexts;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class KwicsTest {
	
	@Test
	public void test() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test(storage);
		}
	}


	public void test(Storage storage) throws IOException {

		Document document;
		LuceneManager luceneManager = storage.getLuceneManager();
		document = new Document();
		document.add(new TextField("lexical", "dark and stormy night in document one", Field.Store.YES));
		luceneManager.addDocument(RandomStringUtils.randomAlphabetic(10), document);
		
		FlexibleParameters parameters;
		
		parameters = new FlexibleParameters();
		parameters.addParameter("string",  "It was a dark and stormy night.");
		parameters.addParameter("string", "It was the best of times it was the worst of times.");
		parameters.addParameter("tool", "StepEnabledIndexedCorpusCreator");

		CorpusCreator creator = new CorpusCreator(storage, parameters);
		creator.run();
		parameters.setParameter("corpus", creator.getStoredId());
		
		DocumentContexts kwics;
		
		parameters.setParameter("tool", "Kwics");
		parameters.setParameter("query", "it was");
		kwics = new DocumentContexts(storage, parameters);
		kwics.run();
		
		storage.destroy();
	}

}

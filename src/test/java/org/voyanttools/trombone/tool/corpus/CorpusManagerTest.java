/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

/**
 * @author sgs
 *
 */
public class CorpusManagerTest {

	@Test
	public void test() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test(storage);
		}
	}

	public void test(Storage storage) throws IOException {

		String[] strings = new String[]{"It was a dark and stormy night.", "It was the best of times it was the worst of times."};
		FlexibleParameters parameters;
		parameters = new FlexibleParameters();
		parameters.addParameter("string",  strings[0]);
		parameters.addParameter("string", strings[1]);
		parameters.addParameter("tool", "StepEnabledIndexedCorpusCreator");
		parameters.addParameter("noCache", 1);
		RealCorpusCreator creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		String corpusId = creator.getStoredId();
		
		// make sure our initial corpus has two documents
		parameters = new FlexibleParameters();
		parameters.setParameter("corpus", corpusId);
		Corpus corpus = CorpusManager.getCorpus(storage, parameters);
		assertEquals(2, corpus.size());
		
		// try to remove one of the documents
		parameters.setParameter("removeDocuments", "true");
		parameters.setParameter("docIndex", 0);		
		CorpusManager corpusManager;
		corpusManager = new CorpusManager(storage, parameters);
		corpusManager.run();
		corpus = corpusManager.getCorpus();
		assertEquals(1, corpus.size());
		assertTrue(IOUtils.toString(storage.getStoredDocumentSourceStorage().getStoredDocumentSourceInputStream(corpus.getDocument(0).getId())).contains(strings[1]));
		
		// try to keep only one document
		parameters.clear();
		parameters.setParameter("corpus", corpusId);
		parameters.setParameter("keepDocuments", "true");
		parameters.setParameter("docIndex", 0);
		corpusManager = new CorpusManager(storage, parameters);
		corpusManager.run();
		corpus = corpusManager.getCorpus();
		assertEquals(1, corpus.size());
		assertTrue(IOUtils.toString(storage.getStoredDocumentSourceStorage().getStoredDocumentSourceInputStream(corpus.getDocument(0).getId())).contains(strings[0]));
		
		// reverse the order
		parameters.clear();
		parameters.setParameter("corpus", corpusId);
		parameters.setParameter("reorderDocuments", "true");
		parameters.setParameter("docIndex", new int[]{1,0});
		corpusManager = new CorpusManager(storage, parameters);
		corpusManager.run();
		corpus = corpusManager.getCorpus();
		assertEquals(2, corpus.size());
		assertTrue(IOUtils.toString(storage.getStoredDocumentSourceStorage().getStoredDocumentSourceInputStream(corpus.getDocument(0).getId())).contains(strings[1]));
		
		// make sure we still have two documents in our original corpus
		parameters.clear();
		parameters.setParameter("corpus", corpusId);
		assertEquals(2, CorpusManager.getCorpus(storage, parameters).size());

		storage.destroy();
		
	}
}

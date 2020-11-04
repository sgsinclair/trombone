package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.model.CorpusNgram;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class CorpusNgramsTest {
	
	@Test
	public void test() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test(storage);
		}
	}

	public void test(Storage storage) throws IOException {
		
		// add another file to the storage
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("udhr")});
		CorpusCreator creator = new CorpusCreator(storage, parameters);
		creator.run();
		
		parameters.setParameter("corpus", creator.getStoredId());

		List<CorpusNgram> ngrams;
		
		CorpusNgrams corpusNgrams;
		
		parameters.setParameter("query", "la");
		parameters.setParameter("minLength", 4);
		parameters.setParameter("maxLength", 4);
		
		corpusNgrams = new CorpusNgrams(storage, parameters);
		corpusNgrams.run();
		
		ngrams = corpusNgrams.getNgrams();
		assertEquals(2, ngrams.size());
		for (CorpusNgram ngram : ngrams) {
			assertTrue(ngram.getLength()==4);
		}

		// try phrases
		parameters.setParameter("query", "\"toute personne\"");
		parameters.removeParameter("minLength");
		parameters.removeParameter("maxLength");
		corpusNgrams = new CorpusNgrams(storage, parameters);
		corpusNgrams.run();
		
		ngrams = corpusNgrams.getNgrams();
		for (CorpusNgram ngram : ngrams) {
			assertEquals(ngram.toString(), ngram.getTerm().split("\\s+").length, ngram.getLength());
//			System.out.println(ngram);
		}
		
		storage.destroy();
	}

}

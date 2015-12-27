package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.model.DocumentNgram;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class DocumentNgramsTest {

	@Test
	public void test() throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();
		
		// add another file to the storage
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("udhr")});
		CorpusCreator creator = new CorpusCreator(storage, parameters);
		creator.run();
		
		parameters.setParameter("corpus", creator.getStoredId());

		DocumentNgrams documentNgrams;
		List<DocumentNgram> ngrams;
		
		/*
		documentNgrams = new DocumentNgrams(storage, parameters);
		documentNgrams.run();
		ngrams = documentNgrams.getNgrams();
		for (DocumentNgram ngram : ngrams) {System.out.println(ngram);}
		*/
		
		parameters.setParameter("query", "toute");
		parameters.setParameter("minLength", 4);
		parameters.setParameter("maxLength", 4);
		documentNgrams = new DocumentNgrams(storage, parameters);
		documentNgrams.run();
		ngrams = documentNgrams.getNgrams();
		assertEquals(2, ngrams.size());
		for (DocumentNgram ngram : ngrams) {
			assertTrue(ngram.getLength()==4);
		}

		// try phrases
		parameters.setParameter("query", "\"toute personne\"");
		parameters.removeParameter("minLength");
		parameters.removeParameter("maxLength");
		documentNgrams = new DocumentNgrams(storage, parameters);
		documentNgrams.run();
		ngrams = documentNgrams.getNgrams();
		for (DocumentNgram ngram : ngrams) {
			assertEquals(ngram.toString(), ngram.getTerm().split("\\s+").length, ngram.getLength());
//			System.out.println(ngram);
		}
	}

}

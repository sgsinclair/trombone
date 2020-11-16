package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.model.CorpusTermsCorrelation;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.util.ToolSerializer;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class CorpusTermCorrelationsTest {

	@Test
	public void testLexical() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			testLexical(storage);
		}
	}
	
	public void testLexical(Storage storage) throws IOException {
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("udhr")});
		CorpusCreator creator = new CorpusCreator(storage, parameters);
		creator.run();
		
		parameters.setParameter("corpus", creator.getStoredId());
		parameters.setParameter("query", "la");
		parameters.setParameter("limit", 3);
		
		CorpusTermCorrelations ctc = new CorpusTermCorrelations(storage, parameters);
		ctc.run();
		
		List<CorpusTermsCorrelation> correlations = ctc.getCorrelations();
		assertEquals("should", correlations.get(0).getCorpusTerms()[0].getTerm());
	}

}

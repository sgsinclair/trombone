package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.model.CorpusCollocate;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class CorpusCollocatesTest {

	@Test
	public void test() throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();
		
		// add another file to the storage
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("udhr/udhr-es.txt")});
		CorpusCreator creator = new CorpusCreator(storage, parameters);
		creator.run();
		
		// add the testing files
		parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("udhr/udhr-en.txt"),"file="+TestHelper.getResource("udhr/udhr-fr.txt")});
		creator = new CorpusCreator(storage, parameters);
		creator.run();
		
		
		parameters.setParameter("corpus", creator.getStoredId());
		parameters.setParameter("query", "human");
		
		CorpusCollocates corpusCollocates;
		List<CorpusCollocate> corpusCollocatesList;
		CorpusCollocate corpusCollocate;
		
		parameters.removeParameter("limit"); // make sure no limit
		corpusCollocates = new CorpusCollocates(storage, parameters);
		corpusCollocates.run();
		corpusCollocatesList = corpusCollocates.getCorpusCollocates();
		corpusCollocate = corpusCollocatesList.get(0);
		assertEquals(corpusCollocate.getContextTerm(), "should");
		
		parameters.setParameter("limit", 10); // try with limit
		corpusCollocates = new CorpusCollocates(storage, parameters);
		corpusCollocates.run();
		corpusCollocatesList = corpusCollocates.getCorpusCollocates();
		corpusCollocate = corpusCollocatesList.get(0);
		assertEquals(corpusCollocate.getContextTerm(), "should");
		
		
	}

}

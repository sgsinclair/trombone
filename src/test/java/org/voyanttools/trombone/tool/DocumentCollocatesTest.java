package org.voyanttools.trombone.tool;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.model.DocumentCollocate;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.corpus.CorpusCreator;
import org.voyanttools.trombone.tool.corpus.DocumentCollocates;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class DocumentCollocatesTest {

	@Test
	public void test() throws IOException {
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("udhr.txt")});
		Storage storage = TestHelper.getDefaultTestStorage();		
		CorpusCreator creator = new CorpusCreator(storage, parameters);
		creator.run();
		parameters.setParameter("corpus", creator.getStoredId());
		parameters.setParameter("query", "human");
		
		
		DocumentCollocates documentCollocates;
		List<DocumentCollocate> documentCollocatesList;
		DocumentCollocate documentCollocate;
		
		// run with default sort contextDocumentRelativeDifferenceDescending
		documentCollocates = new DocumentCollocates(storage, parameters);
		documentCollocates.run();
		documentCollocatesList = documentCollocates.getDocumentCollocates();
		documentCollocate = documentCollocatesList.get(0);
		assertEquals(documentCollocate.getTerm(), "should");

		// run with terms sort
		parameters.setParameter("sort", "termAsc");
		documentCollocates = new DocumentCollocates(storage, parameters);
		documentCollocates.run();
		documentCollocatesList = documentCollocates.getDocumentCollocates();
		documentCollocate = documentCollocatesList.get(documentCollocatesList.size()-1);
		assertEquals(documentCollocate.getTerm(), "world");
		
		// run with terms relative frequency of context terms sort
		parameters.setParameter("sort", "relDesc");
		documentCollocates = new DocumentCollocates(storage, parameters);
		documentCollocates.run();
		documentCollocatesList = documentCollocates.getDocumentCollocates();
		documentCollocate = documentCollocatesList.get(0);
		assertEquals(documentCollocate.getTerm(), "should");
		documentCollocate = documentCollocatesList.get(documentCollocatesList.size()-1);
		assertEquals(documentCollocate.getTerm(), "world");
		
		// run with terms relative frequency of context terms sort
		parameters.setParameter("sort", "rawDesc");
		documentCollocates = new DocumentCollocates(storage, parameters);
		documentCollocates.run();
		documentCollocatesList = documentCollocates.getDocumentCollocates();
		documentCollocate = documentCollocatesList.get(0);
		assertEquals(documentCollocate.getTerm(), "should");
		documentCollocate = documentCollocatesList.get(documentCollocatesList.size()-1);
		assertEquals(documentCollocate.getTerm(), "world");

		// run with terms relative frequency of context terms sort
		parameters.setParameter("sort", "docRelDesc");
		documentCollocates = new DocumentCollocates(storage, parameters);
		documentCollocates.run();
		documentCollocatesList = documentCollocates.getDocumentCollocates();
		documentCollocate = documentCollocatesList.get(0);
		assertEquals(documentCollocate.getTerm(), "to");
		documentCollocate = documentCollocatesList.get(documentCollocatesList.size()-1);
		assertEquals(documentCollocate.getTerm(), "world");
		
		// run with terms relative frequency of context terms sort
		parameters.setParameter("sort", "docRawDesc");
		documentCollocates = new DocumentCollocates(storage, parameters);
		documentCollocates.run();
		documentCollocatesList = documentCollocates.getDocumentCollocates();
		documentCollocate = documentCollocatesList.get(0);
		assertEquals(documentCollocate.getTerm(), "to");
		documentCollocate = documentCollocatesList.get(documentCollocatesList.size()-1);
		assertEquals(documentCollocate.getTerm(), "world");
	}

}

package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.model.DocumentToken;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class DocumentTokensTest {

	@Test
	public void test() throws IOException {
		
		Storage storage = TestHelper.getDefaultTestStorage();

		FlexibleParameters parameters;
		
		parameters = new FlexibleParameters();
		parameters.addParameter("string",  "It was a dark and stormy night.");
		parameters.addParameter("string", "It was the best of times it was the worst of times.");

		RealCorpusCreator creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		parameters.setParameter("corpus", creator.getStoredId());
		
		DocumentTokens docTokens;
		List<DocumentToken> tokens;
		
		docTokens = new DocumentTokens(storage, parameters);
		docTokens.run();
		tokens = docTokens.getDocumentTokens();
		assertEquals(44, tokens.size());
		assertEquals("It", tokens.get(2).getTerm());

		parameters.setParameter("withPosLemmas", "true");
		docTokens = new DocumentTokens(storage, parameters);
		docTokens.run();
		tokens = docTokens.getDocumentTokens();
		assertEquals(44, tokens.size());
		assertEquals("it", tokens.get(2).getLemma());
		
	}

}

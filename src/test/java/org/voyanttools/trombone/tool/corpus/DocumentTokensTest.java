package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.model.DocumentToken;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class DocumentTokensTest {
	
	@Test
	public void test() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test(storage);
		}
	}

	public void test(Storage storage) throws IOException {
		
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
		
		storage.destroy();
		
	}
	
	@Test
	public void testLanguages() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			testLanguages(storage);
		}
	}

	public void testLanguages(Storage storage) throws IOException {

		FlexibleParameters parameters;
		
		parameters = new FlexibleParameters();
		parameters.addParameter("file",  new String[]{TestHelper.getResource("udhr/udhr-en.txt").getPath(),TestHelper.getResource("udhr/udhr-es.txt").getPath(),TestHelper.getResource("udhr/udhr-fr.txt").getPath()});
		parameters.addParameter("string", "我们第一届全国人民代表大会第一次会议");
		
		RealCorpusCreator creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		parameters.setParameter("corpus", creator.getStoredId());
		
		DocumentTokens docTokens;
		List<DocumentToken> tokens;
		
		parameters.setParameter("withPosLemmas", "true");
		parameters.setParameter("noOthers", "true");
		parameters.setParameter("docIndex", "0");
		docTokens = new DocumentTokens(storage, parameters);
		docTokens.run();
		tokens = docTokens.getDocumentTokens();
		assertEquals(50, tokens.size());
		assertEquals("universal", tokens.get(0).getLemma());
		
//		parameters.setParameter("docIndex", "1");
//		docTokens = new DocumentTokens(storage, parameters);
//		docTokens.run();
//		tokens = docTokens.getDocumentTokens();
//		assertEquals(50, tokens.size());
//		assertEquals("todo", tokens.get(2).getLemma());
		
		parameters.setParameter("docIndex", "2");
		docTokens = new DocumentTokens(storage, parameters);
		docTokens.run();
		tokens = docTokens.getDocumentTokens();
		assertEquals(50, tokens.size());
		assertEquals("article", tokens.get(6).getLemma());
		
		parameters.setParameter("docIndex", "3");
		docTokens = new DocumentTokens(storage, parameters);
		boolean hasException = false;
		try {
			docTokens.run();
		} catch (Exception e) {
			hasException = true;
		}
		assertTrue(hasException);
		
		storage.destroy();
	}

}

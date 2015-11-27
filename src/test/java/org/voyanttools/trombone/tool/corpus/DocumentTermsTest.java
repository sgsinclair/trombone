package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.junit.Test;
import org.voyanttools.trombone.model.DocumentTerm;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;

public class DocumentTermsTest {

	@Test
	public void test() throws CorruptIndexException, IOException {
		Storage storage = new MemoryStorage();
		
		// add an additional document to the corpus
		Document document = new Document();
		document.add(new TextField("lexical", "dark and stormy night in document one", Field.Store.YES));
		storage.getLuceneManager().addDocument(document);
		
		FlexibleParameters parameters;
		
		parameters = new FlexibleParameters();
		parameters.addParameter("string",  "It was a dark and stormy night.");
		parameters.addParameter("string", "It was the best of times it was the worst of times.");
		parameters.addParameter("tool", "StepEnabledIndexedCorpusCreator");
		parameters.addParameter("noCache", 1);

		RealCorpusCreator creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		parameters.setParameter("corpus", creator.getStoredId());
		
		parameters.setParameter("tool", "DocumentTermFrequencies");
		
		DocumentTerms documentTermsTool = new DocumentTerms(storage, parameters);
		documentTermsTool.run();
		List<DocumentTerm> documentTerms = documentTermsTool.getDocumentTerms();
		assertEquals(14, documentTerms.size());
		
		parameters.setParameter("docIndex", 0);
		documentTermsTool = new DocumentTerms(storage, parameters);
		documentTermsTool.run();
		documentTerms = documentTermsTool.getDocumentTerms();
		assertEquals(7, documentTerms.size());
		
		parameters.setParameter("query", "it");
		documentTermsTool = new DocumentTerms(storage, parameters);
		documentTermsTool.run();
		documentTerms = documentTermsTool.getDocumentTerms();
		assertEquals(1, documentTerms.size());
		parameters.setParameter("query", "it");
		
		parameters.removeParameter("docIndex");
		documentTermsTool = new DocumentTerms(storage, parameters);
		documentTermsTool.run();
		documentTerms = documentTermsTool.getDocumentTerms();
		assertEquals(2, documentTerms.size());
	}

}

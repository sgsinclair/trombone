package org.voyanttools.trombone.input.extract;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusAccess;
import org.voyanttools.trombone.model.CorpusMetadata;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.tool.corpus.CorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class JsonFeaturesTest {

	@Test
	public void test() throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();
		StoredDocumentSourceStorage storeDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		FlexibleParameters parameters = new FlexibleParameters();
		
		parameters.setParameter("file", TestHelper.getResource("formats/rnadnatinysample.jsonl.zip").getAbsolutePath());
		parameters.setParameter("inputFormat", "JSONLINESFEATURES");
		
		CorpusCreator creator = new CorpusCreator(storage, parameters);
		creator.run();
		
		Corpus corpus = storage.getCorpusStorage().getCorpus(creator.getStoredId());
		assertEquals(2, corpus.size());
		CorpusMetadata corpusMetadata = corpus.getCorpusMetadata();
		assertEquals(67838, corpusMetadata.getTokensCount(TokenType.lexical));
		assertEquals(7865, corpusMetadata.getTypesCount(TokenType.lexical));
		assertEquals(CorpusAccess.NONCONSUMPTIVE, corpusMetadata.getNoPasswordAccess());
		assertEquals(1, corpusMetadata.getAccessPasswords(CorpusAccess.ACCESS).length);
		DocumentMetadata documentMetadata = corpus.getDocument(0).getMetadata();
		assertEquals("Acta Biotechnologica volume 18 issue 3", documentMetadata.getTitle());
		assertEquals("rnadnatinysample.jsonl (1)", documentMetadata.getLocation());
		assertEquals(DocumentFormat.JSONFEATURES, documentMetadata.getDocumentFormat());

	}

}

package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class DocumentsMetadataTest {
	
	@Test
	public void test() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test(storage);
		}
	}

	public void test(Storage storage) throws IOException {

		FlexibleParameters parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("udhr")});
		CorpusCreator creator = new CorpusCreator(storage, parameters);
		creator.run();
		parameters.removeParameter("file");
		parameters.setParameter("corpus", creator.getStoredId());
		
		DocumentsMetadata documentsMetadata = new DocumentsMetadata(storage, parameters);
		documentsMetadata.run();
		List<IndexedDocument> documents = documentsMetadata.getDocuments();
		IndexedDocument document = documents.get(0);
		DocumentMetadata metadata = document.getMetadata();
		assertEquals(28, metadata.getSentencesCount()); // newer doc, should have sentences
		
		// now we'll test recovering from older document without sentences count
		metadata.setSentencesCount(0);
		storage.getStoredDocumentSourceStorage().updateStoredDocumentSourceMetadata(document.getId(), metadata);
		
		// check if we have really removed the value
		metadata = storage.getStoredDocumentSourceStorage().getStoredDocumentSourceMetadata(document.getId());
		assertEquals(0, metadata.getSentencesCount());
		
		// now re-run documents metadata which should magically restore sentences
		documentsMetadata = new DocumentsMetadata(storage, parameters);
		documentsMetadata.run();
		documents = documentsMetadata.getDocuments();
		document = documents.get(0);
		metadata = document.getMetadata();
		assertEquals(28, metadata.getSentencesCount());
		
		storage.destroy();
	}

}

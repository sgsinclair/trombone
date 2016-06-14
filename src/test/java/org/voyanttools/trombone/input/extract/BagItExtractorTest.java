package org.voyanttools.trombone.input.extract;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class BagItExtractorTest {

	@Test
	public void test() throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();
		StoredDocumentSourceStorage storeDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		FlexibleParameters parameters = new FlexibleParameters();
		StoredDocumentSourceExtractor extractor = new StoredDocumentSourceExtractor(storeDocumentSourceStorage, parameters);
		
		InputSource inputSource;
		StoredDocumentSource storedDocumentSource;
		StoredDocumentSource extractedStoredDocumentSource;
		DocumentMetadata metadata;
		
		
		inputSource = new FileInputSource(TestHelper.getResource("formats/Bag-cwrc_b901f23a_e7a2_4d7e_8db7_8c9b6dbf283a.zip"));
		inputSource.getMetadata().setDocumentFormat(DocumentFormat.BAGIT); // will normally be set by expander
		storedDocumentSource = storeDocumentSourceStorage.getStoredDocumentSource(inputSource);
		extractedStoredDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		metadata = extractedStoredDocumentSource.getMetadata();
		assertEquals("A Beautiful Possibility", metadata.getTitle());
		assertEquals("Edith Ferguson Black", metadata.getAuthor());
		InputStream is = storeDocumentSourceStorage.getStoredDocumentSourceInputStream(extractedStoredDocumentSource.getId());
		String contents = IOUtils.toString(is);
		assertTrue(contents.contains("In one of the fairest"));
		is.close();
		
		storage.destroy();
	}

}

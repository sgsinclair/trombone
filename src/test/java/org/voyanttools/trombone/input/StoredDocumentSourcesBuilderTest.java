/**
 * 
 */
package org.voyanttools.trombone.input;


import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

/**
 * @author sgs
 *
 */
public class StoredDocumentSourcesBuilderTest {

	@Test
	public void test() throws IOException, URISyntaxException {
		File tempDirectory = TestHelper.getTemporaryTestStorageDirectory();
		Storage storage = new FileStorage(tempDirectory);
		FlexibleParameters parameters = new FlexibleParameters();
		parameters.addParameter("string", "this is a test");
		parameters.addParameter("file", TestHelper.getResource("xml/rss.xml").toString());
		parameters.addParameter("file", TestHelper.getResource("xml/").toString());
		ExpandedStoredDocumentSourcesBuilder builder = new ExpandedStoredDocumentSourcesBuilder(storage.getStoredDocumentSourceStorage(), parameters);
		List<StoredDocumentSource> storedDocumentSources = builder.getStoredDocumentSources();
		assertEquals("we should have three documents", 3, storedDocumentSources.size());
		FileUtils.deleteDirectory(tempDirectory);
	}

}

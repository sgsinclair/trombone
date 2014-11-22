/**
 * 
 */
package org.voyanttools.trombone.input.index;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

/**
 * @author sgs
 *
 */
public class LuceneIndexerTest {

	@Test
	public void testDuplicateAdd() throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();
		InputSource one = new StringInputSource("one");
		InputSource two = new StringInputSource("two");
		InputSource three = new StringInputSource("three");
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		List<StoredDocumentSource> storedDocumentSources = new ArrayList<StoredDocumentSource>();
		storedDocumentSources.add(storedDocumentSourceStorage.getStoredDocumentSource(one));
		storedDocumentSources.add(storedDocumentSourceStorage.getStoredDocumentSource(two));
		LuceneIndexer luceneIndexer = new LuceneIndexer(storage, new FlexibleParameters());
		luceneIndexer.index(storedDocumentSources);
		
		// make sure we have exactly two documents in the lucene index
		assertEquals(2, storage.getLuceneManager().getDirectoryReader().numDocs());
		storedDocumentSources.add(storedDocumentSourceStorage.getStoredDocumentSource(three));
		luceneIndexer.index(storedDocumentSources);
		
		// make sure we have exactly three documents in the lucene index (no duplicates from the first time we added)
		assertEquals(3, storage.getLuceneManager().getDirectoryReader().numDocs());
	}

}

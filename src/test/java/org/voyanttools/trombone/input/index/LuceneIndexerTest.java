/**
 * 
 */
package org.voyanttools.trombone.input.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;
import org.voyanttools.trombone.input.extract.StoredDocumentSourceExtractor;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

/**
 * @author sgs
 *
 */
public class LuceneIndexerTest {

	//@Test
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
		
		storage.destroy();
	}
	
	/**
	 * The code below is a bit hard to follow, but essentially we're wanting to use the usual extraction
	 * workflow (which produces a guessed language code), then Lucene analysis to double-check the
	 * number of words yielded by different tokenization processes:
	 * 
	 * * i18n/zh_utf8.txt: 我们第一届全国人民代表大会第一次会议
	 * 		* built-in tokenizer: 10 tokens
	 * 		* word boundaries tokenizer: 1 token
	 * * i18n/zh_segmented_utf8.txt: 我们 第一 届 全国人民代表大会 第 一次 会议
	 * 		* built-in tokenizer: 9 tokens
	 * 		* word boundaries tokenizer: 7 tokens
	 * 
	 * With thanks to David Lawrence for nudging improvements and providing an example text.
	 * @throws IOException
	 */
	@Test
	public void testI18n() throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();
		
		Map<String, Integer> docsToTokensMap = new HashMap<String, Integer>();
		
		// extract and index with no parameters
		FlexibleParameters parameters = new FlexibleParameters();
		InputSource originalInputSource = new FileInputSource(TestHelper.getResource("i18n/zh_utf8.txt")); // 10 tokens
		InputSource segmentedInputSource = new FileInputSource(TestHelper.getResource("i18n/zh_segmented_utf8.txt")); // 9 tokens
		List<StoredDocumentSource> storedDocumentSources = new ArrayList<StoredDocumentSource>();
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		storedDocumentSources.add(storedDocumentSourceStorage.getStoredDocumentSource(originalInputSource));
		storedDocumentSources.add(storedDocumentSourceStorage.getStoredDocumentSource(segmentedInputSource));
		StoredDocumentSourceExtractor extractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, parameters);
		List<StoredDocumentSource> extractedDocumentSources = extractor.getExtractedStoredDocumentSources(storedDocumentSources);
		LuceneIndexer luceneIndexer = new LuceneIndexer(storage, parameters);
		String id = luceneIndexer.index(extractedDocumentSources);
		List<String> ids = storage.retrieveStrings(id);
		docsToTokensMap.put(ids.get(0), 9);
		docsToTokensMap.put(ids.get(1), 10);
		
		// now re-extract and index with tokenization parameter
		parameters.addParameter("tokenization", "wordBoundaries");
		luceneIndexer = new LuceneIndexer(storage, parameters);
		// indexer should create new documents in index because of parameters
		id = luceneIndexer.index(extractedDocumentSources);
		ids = storage.retrieveStrings(id);
		docsToTokensMap.put(ids.get(0), 1);
		docsToTokensMap.put(ids.get(1), 7);
		// make sure we have new metadata
		assertEquals(0, storedDocumentSourceStorage.getStoredDocumentSourceMetadata(ids.get(0)).getLastTokenPositionIndex(TokenType.lexical));

		
		// finally, go through and check our token counts
		LeafReader reader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getDirectoryReader());
		assertEquals(4, reader.maxDoc());
		IndexSearcher searcher = new IndexSearcher(reader);
		for (Map.Entry<String, Integer> entry : docsToTokensMap.entrySet()) {
			TopDocs topDocs = searcher.search(new TermQuery(new Term("id", entry.getKey())), 1);
			int doc = topDocs.scoreDocs[0].doc;
			assertEquals((int) entry.getValue(), (int) reader.getTermVector(doc, TokenType.lexical.name()).size());
		}
		
		storage.destroy();
	}
	
	private void outputTerms(TermsEnum termsEnum) throws IOException {
		BytesRef bytesRef = termsEnum.next();
		while(bytesRef!=null) {
			System.out.println(bytesRef.utf8ToString());
			bytesRef = termsEnum.next();
		}
	}

}

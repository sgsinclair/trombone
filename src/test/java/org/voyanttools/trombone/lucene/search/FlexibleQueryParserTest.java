package org.voyanttools.trombone.lucene.search;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TotalHitCountCollector;
import org.junit.Test;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;

public class FlexibleQueryParserTest {

	@Test
	public void test() throws IOException {

//		File storageDirectory = TestHelper.getTemporaryTestStorageDirectory();
//		Storage storage = new FileStorage(storageDirectory);
		Storage storage = new MemoryStorage();
		Document document;
		LuceneManager luceneManager = storage.getLuceneManager();

		document = new Document();
		document.add(new TextField("lexical", "It was a dark and stormy night.", Field.Store.YES));
		luceneManager.addDocument(document);
		document = new Document();
		document.add(new TextField("lexical", "It was the best of times it was the worst of times.", Field.Store.YES));
		document.add(new TextField("author", "me", Field.Store.NO));
		luceneManager.addDocument(document);	
		
		AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getIndexReader());
		IndexSearcher indexSearcher = luceneManager.getIndexSearcher();
		FlexibleQueryParser queryParser = new FlexibleQueryParser(atomicReader, storage.getLuceneManager().getAnalyzer());
		
		
		Map<String, Query> queriesMap;
		Query query;
		TotalHitCountCollector collector;
		
		// single term
		queriesMap = queryParser.getQueriesMap(new String[]{"dark"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());

		// single term with case (this gets converted to lower case)
		queriesMap = queryParser.getQueriesMap(new String[]{"It"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("It");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(2, collector.getTotalHits());
		
		// single term (ignore quotes)
		queriesMap = queryParser.getQueriesMap(new String[]{"\"dark\""}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());
		
		// two separate terms (not collapsed)
		queriesMap = queryParser.getQueriesMap(new String[]{"dark","best"}, TokenType.lexical, true);
		assertEquals(2, queriesMap.size());
		query = queriesMap.get("dark");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());
		query = queriesMap.get("best");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());

		// two separate terms (not collapsed)
		queriesMap = queryParser.getQueriesMap(new String[]{"dark;best"}, TokenType.lexical, true);
		assertEquals(2, queriesMap.size());
		query = queriesMap.get("dark");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());
		query = queriesMap.get("best");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());
		
		// two separate terms (not collapsed), with spaces
		queriesMap = queryParser.getQueriesMap(new String[]{" dark ; best "}, TokenType.lexical, true);
		assertEquals(2, queriesMap.size());
		query = queriesMap.get("dark");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());
		query = queriesMap.get("best");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());
		
		// comma-separated terms (collapased)
		queriesMap = queryParser.getQueriesMap(new String[]{"dark,best"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark,best");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(2, collector.getTotalHits());
		
		// wildcards
		queriesMap = queryParser.getQueriesMap(new String[]{"dar*,b*t"}, TokenType.lexical, true); // dark and best
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dar*,b*t");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(2, collector.getTotalHits());

		// two separate wildcards (not collapsed)
		queriesMap = queryParser.getQueriesMap(new String[]{"dar*;bes*"}, TokenType.lexical, true);
		assertEquals(2, queriesMap.size());
		query = queriesMap.get("dar*");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());
		query = queriesMap.get("bes*");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());

		// phrase
		queriesMap = queryParser.getQueriesMap(new String[]{"dark and"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark and");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());
		
		// phrase with wildcards
		queriesMap = queryParser.getQueriesMap(new String[]{"dar* an*"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dar* an*");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());

		// phrase with wildcards
		queriesMap = queryParser.getQueriesMap(new String[]{"dark stormy~2"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark stormy~2");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());

		// phrase with wildcards (ignored quotes)
		queriesMap = queryParser.getQueriesMap(new String[]{"\"dark stormy\"~2"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark stormy~2");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());
		
		// two terms, both of which must occur
		queriesMap = queryParser.getQueriesMap(new String[]{"+it,+dark"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("+it,+dark");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());

		// two terms, including one from another field
		queriesMap = queryParser.getQueriesMap(new String[]{"+author:me,+it"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("+author:me,+it");
		collector = new TotalHitCountCollector();
		indexSearcher.search(query, collector);
		assertEquals(1, collector.getTotalHits());

	}

}

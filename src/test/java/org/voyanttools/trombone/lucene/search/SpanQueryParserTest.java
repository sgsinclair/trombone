/*******************************************************************************
 * Trombone is a flexible text processing and analysis library used
 * primarily by Voyant Tools (voyant-tools.org).
 * 
 * Copyright (©) 2007-2012 Stéfan Sinclair & Geoffrey Rockwell
 * 
 * This file is part of Trombone.
 * 
 * Trombone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.lucene.search;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.junit.Test;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.lucene.search.SpanQueryParser;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;

/**
 * @author sgs
 *
 */
public class SpanQueryParserTest {

	@Test
	public void test() throws IOException {
		
//		File storageDirectory = TestHelper.getTemporaryTestStorageDirectory();
//		Storage storage = new FileStorage(storageDirectory);
		Storage storage = new MemoryStorage();
		Document document;
		LuceneManager luceneManager = storage.getLuceneManager();
		Bits bits = new Bits.MatchAllBits(2);
		Map<Term,TermContext> termsMap = new HashMap<Term,TermContext>();

		document = new Document();
		document.add(new TextField("lexical", "It was a dark and stormy night.", Field.Store.YES));
		luceneManager.addDocument(document);
		document = new Document();
		document.add(new TextField("lexical", "It was the best of times it was the worst of times.", Field.Store.YES));
		luceneManager.addDocument(document);	
		
		AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getIndexReader());
		
		SpanQueryParser spanQueryParser = new SpanQueryParser(atomicReader, storage.getLuceneManager().getAnalyzer());
		
		
		Map<String, SpanQuery> queriesMap;
		SpanQuery query;
		Spans spans;
		
		// single term
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dark"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());

		// single term with case (this gets converted to lower case)
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"It"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("It");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(0,spans.start());
		spans.next();
		assertEquals(1,spans.doc());
		assertEquals(0,spans.start());
		spans.next();
		assertEquals(6,spans.start());
		
		// single term (ignore quotes)
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"\"dark\""}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		
		// two separate terms (not collapsed)
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dark","best"}, TokenType.lexical, true);
		assertEquals(2, queriesMap.size());
		query = queriesMap.get("dark");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		query = queriesMap.get("best");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(1,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());

		// two separate terms (not collapsed)
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dark;best"}, TokenType.lexical, true);
		assertEquals(2, queriesMap.size());
		query = queriesMap.get("dark");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		query = queriesMap.get("best");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(1,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		
		// two separate terms (not collapsed), with spaces
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{" dark ; best "}, TokenType.lexical, true);
		assertEquals(2, queriesMap.size());
		query = queriesMap.get("dark");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		query = queriesMap.get("best");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(1,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		
		// comma-separated terms (collapased)
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dark,best"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark,best");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		spans.next();
		assertEquals(1,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		
		// wildcards
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dar*,b*t"}, TokenType.lexical, true); // dark and best
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dar*,b*t");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		spans.next();
		assertEquals(1,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());

		// two separate wildcards (not collapsed)
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dar*;bes*"}, TokenType.lexical, true);
		assertEquals(2, queriesMap.size());
		query = queriesMap.get("dar*");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		query = queriesMap.get("bes*");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(1,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());

		// phrase
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dark and"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark and");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertEquals(5,spans.end());
		assertFalse(spans.next());
		
		// phrase with wildcards
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dar* an*"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dar* an*");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertEquals(5,spans.end());
		assertFalse(spans.next());

		// phrase with wildcards
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dark stormy~2"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark stormy~2");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertEquals(6,spans.end());
		assertFalse(spans.next());

		// phrase with wildcards (ignored quotes)
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"\"dark stormy\"~2"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark stormy~2");
		spans = query.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertEquals(6,spans.end());
		assertFalse(spans.next());
	}

}

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
package org.voyanttools.trombone.tool.analysis;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
		
		SpanQueryParser spanQueryParser = new SpanQueryParser();
		
		AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getIndexReader());
		
		Map<String, SpanQuery> spanQueriesMap;
		SpanQuery spanQuery;
		Spans spans;
		
		// single term
		spanQueriesMap = spanQueryParser.getSpanQueries(atomicReader, new String[]{"dark"}, TokenType.lexical, true);
		assertEquals(1, spanQueriesMap.size());
		spanQuery = spanQueriesMap.get("dark");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());

		// single term (ignore quotes)
		spanQueriesMap = spanQueryParser.getSpanQueries(atomicReader, new String[]{"\"dark\""}, TokenType.lexical, true);
		assertEquals(1, spanQueriesMap.size());
		spanQuery = spanQueriesMap.get("dark");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		
		// two separate terms (not collapsed)
		spanQueriesMap = spanQueryParser.getSpanQueries(atomicReader, new String[]{"dark","best"}, TokenType.lexical, true);
		assertEquals(2, spanQueriesMap.size());
		spanQuery = spanQueriesMap.get("dark");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		spanQuery = spanQueriesMap.get("best");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(1,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());

		// two separate terms (not collapsed)
		spanQueriesMap = spanQueryParser.getSpanQueries(atomicReader, new String[]{"dark;best"}, TokenType.lexical, true);
		assertEquals(2, spanQueriesMap.size());
		spanQuery = spanQueriesMap.get("dark");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		spanQuery = spanQueriesMap.get("best");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(1,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		
		// two separate terms (not collapsed), with spaces
		spanQueriesMap = spanQueryParser.getSpanQueries(atomicReader, new String[]{" dark ; best "}, TokenType.lexical, true);
		assertEquals(2, spanQueriesMap.size());
		spanQuery = spanQueriesMap.get("dark");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		spanQuery = spanQueriesMap.get("best");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(1,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		
		// comma-separated terms (collapased)
		spanQueriesMap = spanQueryParser.getSpanQueries(atomicReader, new String[]{"dark,best"}, TokenType.lexical, true);
		assertEquals(1, spanQueriesMap.size());
		spanQuery = spanQueriesMap.get("dark,best");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		spans.next();
		assertEquals(1,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		
		// wildcards
		spanQueriesMap = spanQueryParser.getSpanQueries(atomicReader, new String[]{"dar*,b*t"}, TokenType.lexical, true); // dark and best
		assertEquals(1, spanQueriesMap.size());
		spanQuery = spanQueriesMap.get("dar*,b*t");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		spans.next();
		assertEquals(1,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());

		// two separate wildcards (not collapsed)
		spanQueriesMap = spanQueryParser.getSpanQueries(atomicReader, new String[]{"dar*;bes*"}, TokenType.lexical, true);
		assertEquals(2, spanQueriesMap.size());
		spanQuery = spanQueriesMap.get("dar*");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());
		spanQuery = spanQueriesMap.get("bes*");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(1,spans.doc());
		assertEquals(3,spans.start());
		assertFalse(spans.next());

		// phrase
		spanQueriesMap = spanQueryParser.getSpanQueries(atomicReader, new String[]{"dark and"}, TokenType.lexical, true);
		assertEquals(1, spanQueriesMap.size());
		spanQuery = spanQueriesMap.get("dark and");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertEquals(5,spans.end());
		assertFalse(spans.next());
		
		// phrase with wildcards
		spanQueriesMap = spanQueryParser.getSpanQueries(atomicReader, new String[]{"dar* an*"}, TokenType.lexical, true);
		assertEquals(1, spanQueriesMap.size());
		spanQuery = spanQueriesMap.get("dar* an*");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertEquals(5,spans.end());
		assertFalse(spans.next());

		// phrase with wildcards
		spanQueriesMap = spanQueryParser.getSpanQueries(atomicReader, new String[]{"dark stormy~2"}, TokenType.lexical, true);
		assertEquals(1, spanQueriesMap.size());
		spanQuery = spanQueriesMap.get("dark stormy~2");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertEquals(6,spans.end());
		assertFalse(spans.next());

		// phrase with wildcards (ignored quotes)
		spanQueriesMap = spanQueryParser.getSpanQueries(atomicReader, new String[]{"\"dark stormy\"~2"}, TokenType.lexical, true);
		assertEquals(1, spanQueriesMap.size());
		spanQuery = spanQueriesMap.get("dark stormy~2");
		spans = spanQuery.getSpans(atomicReader.getContext(), bits, termsMap);
		spans.next();
		assertEquals(0,spans.doc());
		assertEquals(3,spans.start());
		assertEquals(6,spans.end());
		assertFalse(spans.next());
	}

}

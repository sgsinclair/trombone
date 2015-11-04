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
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanWeight;
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
		
		LeafReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getDirectoryReader());
		IndexSearcher indexSearcher = new IndexSearcher(atomicReader);
		
		SpanQueryParser spanQueryParser = new SpanQueryParser(atomicReader, storage.getLuceneManager().getAnalyzer());
		
		
		Map<String, SpanQuery> queriesMap;
		SpanQuery query;
		SpanWeight weight;
		Spans spans;
		
		// single term
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dark"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		spans.nextDoc();
		assertEquals(0,spans.docID());
		spans.nextStartPosition();
		assertEquals(3,spans.startPosition());
		assertEquals(spans.nextStartPosition(), Spans.NO_MORE_POSITIONS);
		assertEquals(spans.nextDoc(), Spans.NO_MORE_DOCS);
		
		// single term with case (this gets converted to lower case)
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"It"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("It");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(0,spans.nextDoc());
		assertEquals(0,spans.nextStartPosition());
		assertEquals(1,spans.nextDoc());
		assertEquals(0,spans.nextStartPosition());
		assertEquals(6,spans.nextStartPosition());
		
		// single term (ignore quotes)
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"\"dark\""}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(0,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(spans.NO_MORE_DOCS, spans.nextDoc());
		
		// two separate terms (not collapsed)
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dark","best"}, TokenType.lexical, true);
		assertEquals(2, queriesMap.size());
		
		query = queriesMap.get("dark");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(0,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(spans.NO_MORE_DOCS, spans.nextDoc());
		
		query = queriesMap.get("best");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(1,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(spans.NO_MORE_DOCS, spans.nextDoc());
		
		// two separate terms (not collapsed)
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dark;best"}, TokenType.lexical, true);
		assertEquals(2, queriesMap.size());
		
		query = queriesMap.get("dark");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(0,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(spans.NO_MORE_DOCS, spans.nextDoc());

		query = queriesMap.get("best");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(1,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(spans.NO_MORE_DOCS, spans.nextDoc());
		
		// two separate terms (not collapsed), with spaces
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{" dark ; best "}, TokenType.lexical, true);
		assertEquals(2, queriesMap.size());

		query = queriesMap.get("dark");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(0,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(spans.NO_MORE_DOCS, spans.nextDoc());

		query = queriesMap.get("best");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(1,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(spans.NO_MORE_DOCS, spans.nextDoc());

		
		// comma-separated terms (collapased)
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dark,best"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());

		query = queriesMap.get("dark,best");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(0,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(1,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(spans.NO_MORE_DOCS, spans.nextDoc());
		
		// wildcards
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dar*,b*t"}, TokenType.lexical, true); // dark and best
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dar*,b*t");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(0,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(1,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(spans.NO_MORE_DOCS, spans.nextDoc());
		
		// two separate wildcards (not collapsed)
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dar*;bes*"}, TokenType.lexical, true);
		assertEquals(2, queriesMap.size());

		query = queriesMap.get("dar*");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(0,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(spans.NO_MORE_DOCS, spans.nextDoc());

		query = queriesMap.get("bes*");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(1,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(spans.NO_MORE_DOCS, spans.nextDoc());
		

		// phrase
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dark and"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark and");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(0,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(5,spans.endPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(spans.NO_MORE_DOCS, spans.nextDoc());

		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"it was"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("it was");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(0,spans.nextDoc());
		assertEquals(0,spans.nextStartPosition());
		assertEquals(1,spans.nextDoc());
		assertEquals(0,spans.nextStartPosition());
		assertEquals(6,spans.nextStartPosition());
		
		// phrase with wildcards
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dar* an*"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dar* an*");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(0,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(5,spans.endPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(spans.NO_MORE_DOCS, spans.nextDoc());

		// phrase with wildcards
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"dark stormy~2"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark stormy~2");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(0,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(6,spans.endPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(spans.NO_MORE_DOCS, spans.nextDoc());

		// phrase with wildcards (ignored quotes)
		queriesMap = spanQueryParser.getSpanQueriesMap(new String[]{"\"dark stormy\"~2"}, TokenType.lexical, true);
		assertEquals(1, queriesMap.size());
		query = queriesMap.get("dark stormy~2");
		weight = query.createWeight(indexSearcher, false);
		spans = weight.getSpans(atomicReader.getContext(), SpanWeight.Postings.POSITIONS);
		assertEquals(0,spans.nextDoc());
		assertEquals(3,spans.nextStartPosition());
		assertEquals(6,spans.endPosition());
		assertEquals(spans.NO_MORE_POSITIONS, spans.nextStartPosition());
		assertEquals(spans.NO_MORE_DOCS, spans.nextDoc());
		
		storage.destroy();
	}

}

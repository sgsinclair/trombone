package org.voyanttools.trombone.lucene.search;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.search.Query;
import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.TestHelper;

public class FieldPrefixAwareSimpleQueryParserTest {

	@Test
	public void test() throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();
		FieldPrefixAwareSimpleQueryParser parser = new FieldPrefixAwareSimpleQueryParser(storage.getLuceneManager().getAnalyzer());
		Query query;
		
		// simple default TokenType
		query = parser.parse("test");
		assertEquals("lexical:test",query.toString());
		
		// simple prefix
		query = parser.parse("author:me+test");
		assertEquals("+author:me +lexical:test", query.toString());
		
		// phrase
		query = parser.parse("author:\"me test\"");
		assertEquals("author:\"me test\"", query.toString());
	}
}

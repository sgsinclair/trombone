package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.CorruptIndexException;
import org.junit.Test;
import org.voyanttools.trombone.model.Kwic;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;

public class DocumentContextsTest {

//	@Test
	public void testBoundaries() throws CorruptIndexException, IOException {
		Storage storage = new MemoryStorage();
		
		FlexibleParameters parameters;
		parameters = new FlexibleParameters();
		parameters.addParameter("string", "Test this is a \"test\" and this is a test");

		RealCorpusCreator creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		
		parameters = new FlexibleParameters();
		parameters.setParameter("corpus", creator.getStoredId());
		parameters.addParameter("query",  "test");
		parameters.setParameter("tool", "corpus.DocumentContexts");
		
		DocumentContexts documentContexts;
		List<Kwic> contexts;
		Kwic kwic;
		
		// no overlap handling
		documentContexts = new DocumentContexts(storage, parameters);
		documentContexts.run();
		contexts = documentContexts.getContexts();
		assertEquals(3, contexts.size());
		kwic = contexts.get(1);
		assertEquals("Test this is a \"", kwic.getLeft());
		assertEquals("\" and this is a test", kwic.getRight());
		assertEquals("test", kwic.getMiddle());
		
		storage.destroy();
	}
	
//	@Test
	public void testOverlapping() throws CorruptIndexException, IOException {
		Storage storage = new MemoryStorage();
		
		FlexibleParameters parameters;
		parameters = new FlexibleParameters();
		parameters.addParameter("string", "one two three four five six seven eight keywordOne nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eighteen nineteen twenty twentyone twentytwo twentythree twentyfour twentyfive twentysix twentyseven keywordTwo twentyeight twentynine thirty thirtyone thirtytwo thirtythree thirtyfour keywordThree thirtyfive thirtysix thirtyseven thirtyeight thirtynine forty fortyone fortytwo fortythree fortyfour fortyfive fortysix fortyseven fortyeight fortynine fifty fiftyone keywordFour fiftytwo keywordFive fiftythree fiftyfour fiftyfive keywordSix fiftysix fiftyseven fiftyeight keywordSeven keywordEight fiftynine sixty sixtyone sixtytwo sixtythree sixtyfour sixtyfive sixtysix sixtyseven keywordNine");

		RealCorpusCreator creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		
		parameters = new FlexibleParameters();
		parameters.setParameter("corpus", creator.getStoredId());
		parameters.addParameter("query",  "keyword*");
		parameters.setParameter("tool", "corpus.DocumentContexts");
		parameters.setParameter("sortBy", "positionAsc");
		
		DocumentContexts documentContexts;
		List<Kwic> contexts;
		
		// no overlap handling
		documentContexts = new DocumentContexts(storage, parameters);
		documentContexts.run();
		contexts = documentContexts.getContexts();
		//output(contexts, "\nNo overlapping strategy, show all:");
		assertEquals(9, contexts.size());
		
		// first overlap handling
		parameters.setParameter("overlapStrategy", "first");
		documentContexts = new DocumentContexts(storage, parameters);
		documentContexts.run();
		contexts = documentContexts.getContexts();
		//output(contexts, "\nFirst come, first served (simple elimination of overlapping occurrences in document order):");
		assertEquals(5, contexts.size());
		
		// merge overlap handling
		parameters.setParameter("overlapStrategy", "merge");
		documentContexts = new DocumentContexts(storage, parameters);
		documentContexts.run();
		contexts = documentContexts.getContexts();
		//output(contexts, "\nTry to merge occurrences:");
		assertEquals(5, contexts.size());
		
		storage.destroy();
	}
	
	@Test
	public void testResults() throws IOException {
		Storage storage = new MemoryStorage();
		
		FlexibleParameters parameters;
		parameters = new FlexibleParameters();
		parameters.addParameter("string", "one two three four five six seven eight keywordOne nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eighteen nineteen twenty twentyone twentytwo twentythree twentyfour twentyfive twentysix twentyseven keywordTwo twentyeight twentynine thirty thirtyone thirtytwo thirtythree thirtyfour keywordThree thirtyfive thirtysix thirtyseven thirtyeight thirtynine forty fortyone fortytwo fortythree fortyfour fortyfive fortysix fortyseven fortyeight fortynine fifty fiftyone keywordFour fiftytwo keywordFive fiftythree fiftyfour fiftyfive keywordSix fiftysix fiftyseven fiftyeight keywordSeven keywordEight fiftynine sixty sixtyone sixtytwo sixtythree sixtyfour sixtyfive sixtysix sixtyseven keywordNine");

		RealCorpusCreator creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		
		parameters = new FlexibleParameters();
		parameters.setParameter("corpus", creator.getStoredId());
		parameters.addParameter("query",  "keyword*");
		parameters.setParameter("tool", "corpus.DocumentContexts");
		
		DocumentContexts documentContexts;
		List<Kwic> contexts;
		
		// all
		documentContexts = new DocumentContexts(storage, parameters);
		documentContexts.run();
		contexts = documentContexts.getContexts();
		assertEquals(9, contexts.size());
		
		// start
		parameters.setParameter("start", 5);
		documentContexts = new DocumentContexts(storage, parameters);
		documentContexts.run();
		contexts = documentContexts.getContexts();
		assertEquals(4, contexts.size());

		// start & limit
		parameters.setParameter("limit", 2);
		documentContexts = new DocumentContexts(storage, parameters);
		documentContexts.run();
		contexts = documentContexts.getContexts();
		assertEquals(2, contexts.size());
		
		parameters.removeParameter("start");
		parameters.setParameter("limit", 1);
		parameters.setParameter("position", 54);
		documentContexts = new DocumentContexts(storage, parameters);
		documentContexts.run();
		contexts = documentContexts.getContexts();
		assertEquals(1, contexts.size());
		
		storage.destroy();
	}

	private void output(List<Kwic> kwics, String message) {
		if (message!=null && message.isEmpty()==false) {
			System.out.println(message);
		}
		for (Kwic kwic : kwics) {
			System.out.println(kwic);
		}
	}

}

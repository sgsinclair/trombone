/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

/**
 * @author sgs
 *
 */
public class CorpusSegmentTermsTest {

	@Test
	public void test() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test(storage);
		}
	}

	public void test(Storage storage) throws IOException {
		
		// add another file to the storage
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("udhr")});
		CorpusCreator creator = new CorpusCreator(storage, parameters);
		creator.run();
		
		parameters.setParameter("corpus", creator.getStoredId());
		
		CorpusSegmentTerms corpusSegmentTerms;
		
		// default values
		/*
		corpusSegmentTerms = new CorpusSegmentTerms(storage, parameters);
		corpusSegmentTerms.run();
		assertEquals(3, corpusSegmentTerms.segmentMarkers.size());
		assertEquals("de", corpusSegmentTerms.sortedSegmentTerms.get(0).getKey());
		assertEquals("procès", corpusSegmentTerms.sortedSegmentTerms.get(corpusSegmentTerms.sortedSegmentTerms.size()-1).getKey());
		*/

		parameters.setParameter("stopList", "auto");
		parameters.setParameter("limit", 10);
		parameters.setParameter("segments", 6);
		corpusSegmentTerms = new CorpusSegmentTerms(storage, parameters);
		corpusSegmentTerms.run();
		assertEquals(5, corpusSegmentTerms.segmentMarkers.size()); // corpus is lop-sided so we get 4 segments instead of 6
		assertEquals("droit", corpusSegmentTerms.sortedSegmentTerms.get(0).getKey());
		assertEquals("derechos", corpusSegmentTerms.sortedSegmentTerms.get(corpusSegmentTerms.sortedSegmentTerms.size()-1).getKey());
	
		storage.destroy();
	}

}

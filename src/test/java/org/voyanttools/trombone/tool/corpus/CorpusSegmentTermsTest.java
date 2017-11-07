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
		Storage storage = TestHelper.getDefaultTestStorage();
		
		// add another file to the storage
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("udhr")});
		CorpusCreator creator = new CorpusCreator(storage, parameters);
		creator.run();
		
		parameters.setParameter("corpus", creator.getStoredId());
		
		CorpusSegmentTerms corpusSegmentTerms;
		
		corpusSegmentTerms = new CorpusSegmentTerms(storage, parameters);
		corpusSegmentTerms.run();
	}

}

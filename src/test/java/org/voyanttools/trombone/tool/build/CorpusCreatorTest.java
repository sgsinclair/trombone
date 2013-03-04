package org.voyanttools.trombone.tool.build;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

import com.thoughtworks.xstream.XStream;

public class CorpusCreatorTest {

	@Test
	public void testSteps() throws IOException {
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"string=test","file="+TestHelper.getResource("formats/chars.rtf"),"steps=1"});
		Storage storage = TestHelper.getDefaultTestStorage();

		String nextStep;
		
		// do a first pass one step at a time and make sure we get the right next steps
		
		// store
		RealCorpusCreator creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		nextStep = creator.getNextCorpusCreatorStep();
		assertEquals("expand", nextStep);
		String storedStoredId = creator.getStoredId();
		
		// expand		
		parameters.setParameter("nextCorpusCreatorStep", nextStep);
		parameters.setParameter("storedId", storedStoredId);
		creator.run();
		nextStep = creator.getNextCorpusCreatorStep();
		assertEquals("extract", nextStep);
		String expandedStoredId = creator.getStoredId();
		
		// extract
		parameters.setParameter("nextCorpusCreatorStep", nextStep);
		parameters.setParameter("storedId", expandedStoredId);
		creator.run();
		nextStep = creator.getNextCorpusCreatorStep();
		assertEquals("index", nextStep);
		String extractedStoredId = creator.getStoredId();
		
		// index
		parameters.setParameter("nextCorpusCreatorStep", nextStep);
		parameters.setParameter("storedId", extractedStoredId);
		creator.run();
		nextStep = creator.getNextCorpusCreatorStep();
		assertEquals("corpus", nextStep);
		String indexedStoredId = creator.getStoredId();

		// corpus
		parameters.setParameter("nextCorpusCreatorStep", nextStep);
		parameters.setParameter("storedId", extractedStoredId);
		creator.run();
		nextStep = creator.getNextCorpusCreatorStep();
		assertEquals("done", nextStep);
		String storedCorpusId = creator.getStoredId();
		
		// do a second pass one step at a time and make sure we get the same IDs
		String storedId;
		parameters.removeParameter("nextCorpusCreatorStep");
		
		// store
		creator.run();
		assertEquals(storedStoredId, creator.getStoredId());
		parameters.setParameter("nextCorpusCreatorStep", creator.getNextCorpusCreatorStep());
		parameters.setParameter("storedId", storedStoredId);
		
		// expand
		creator.run();
		assertEquals(expandedStoredId, creator.getStoredId());
		parameters.setParameter("nextCorpusCreatorStep", creator.getNextCorpusCreatorStep());
		parameters.setParameter("storedId", expandedStoredId);

		// extract
		creator.run();
		assertEquals(extractedStoredId, creator.getStoredId());
		parameters.setParameter("nextCorpusCreatorStep", creator.getNextCorpusCreatorStep());
		parameters.setParameter("storedId", extractedStoredId);

		// index
		creator.run();
		assertEquals(indexedStoredId, creator.getStoredId());
		parameters.setParameter("nextCorpusCreatorStep", creator.getNextCorpusCreatorStep());
		parameters.setParameter("storedId", indexedStoredId);
		
		// corpus
		creator.run();
		assertEquals(storedCorpusId, creator.getStoredId());
		
		
		// now do a full pass with a new text
		parameters = new FlexibleParameters(new String[]{"string=test","file="+TestHelper.getResource("formats/chars.rtf")});
		creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		assertEquals(storedCorpusId, creator.getStoredId());

//		XStream xstream;
//		
//		// serialize to XML
//		xstream = new XStream();
//		xstream.autodetectAnnotations(true);
//		String xml = xstream.toXML(creator);
//		System.err.println(xml);
		
		storage.destroy();

	}

}

package org.voyanttools.trombone.tool;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

import com.thoughtworks.xstream.XStream;

public class StepEnabledCorpusCreatorTest {

	@Test
	public void testSteps() throws IOException {
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("formats/chars.rtf"),"steps=1"});
		Storage storage = TestHelper.getDefaultTestStorage();

		String nextStep;
		
		// do a first pass one step at a time and make sure we get the right next steps
		StepEnabledCorpusCreator creator = new StepEnabledCorpusCreator(storage, parameters);
		creator.run();
		nextStep = creator.getNextCorpusCreatorStep();
		assertEquals("expand", nextStep);
		String storedStoredId = creator.getStoredId();
		
		parameters.setParameter("nextCorpusCreatorStep", nextStep);
		parameters.setParameter("storedId", storedStoredId);
		creator.run();
		nextStep = creator.getNextCorpusCreatorStep();
		assertEquals("extract", nextStep);
		String expandedStoredId = creator.getStoredId();
		
		parameters.setParameter("nextCorpusCreatorStep", nextStep);
		parameters.setParameter("storedId", expandedStoredId);
		creator.run();
		nextStep = creator.getNextCorpusCreatorStep();
		assertEquals("index", nextStep);
		String extractedStoredId = creator.getStoredId();
		
		// do a second pass one step at a time and make sure we get the same IDs
		String storedId;
		parameters.removeParameter("nextCorpusCreatorStep");
		
		// store
		creator.run();
		storedId = creator.getStoredId();
		assertEquals(storedId, storedStoredId);
		parameters.setParameter("nextCorpusCreatorStep", creator.getNextCorpusCreatorStep());
		
		// expand
		creator.run();
		storedId = creator.getStoredId();
		assertEquals(storedId, expandedStoredId);
		parameters.setParameter("nextCorpusCreatorStep", creator.getNextCorpusCreatorStep());

		// extract
		creator.run();
		storedId = creator.getStoredId();
		assertEquals(storedId, extractedStoredId);
		parameters.setParameter("nextCorpusCreatorStep", creator.getNextCorpusCreatorStep());

		// now do a full pass with a new text
		parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("formats/chars_utf8.txt")});
		creator = new StepEnabledCorpusCreator(storage, parameters);
		creator.run();
		
		XStream xstream;
		
		// serialize to XML
		xstream = new XStream();
		xstream.autodetectAnnotations(true);
		String xml = xstream.toXML(creator);
		System.err.println(xml);
		
		
		
		
		storage.destroy();

	}

}

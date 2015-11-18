package org.voyanttools.trombone.input.expand;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class XslExpanderTest {

	@Test
	public void test() throws IOException {
		
		InputSource inputSource;
		
		inputSource = new FileInputSource(TestHelper.getResource("formats/chars.xlsx"));
		test(inputSource);

		inputSource = new FileInputSource(TestHelper.getResource("formats/chars.xls"));
		test(inputSource);
		

	}

	private void test(InputSource inputSource) throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		
		StoredDocumentSource storedDocumentSource;
		List<StoredDocumentSource> expandedSourceDocumentSources;

		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		StoredDocumentSourceExpander storedDocumentSourceExpander;
		FlexibleParameters parameters;
		String contents;
		
		// no parameters
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandXsl(storedDocumentSource);
		assertEquals(1, expandedSourceDocumentSources.size());
		
		// zero column tableDocumentsColumns
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocumentsColumns", 0);
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandXsl(storedDocumentSource);
		assertEquals(0, expandedSourceDocumentSources.size());
		
		// first column tableDocumentsColumns, default header column
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocumentsColumns", 1);
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandXsl(storedDocumentSource);
		assertEquals(1, expandedSourceDocumentSources.size());
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedSourceDocumentSources.get(0).getId()));
		assertEquals(1, contents.split("\n").length);
		
		// first column tableDocumentsColumns, no header column
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocumentsColumns", 1);
		parameters.setParameter("tableNoHeadersRow", "true");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandXsl(storedDocumentSource);
		assertEquals(1, expandedSourceDocumentSources.size());
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedSourceDocumentSources.get(0).getId()));
		assertEquals(2, contents.split("\n").length);
		
		// first and second columns tableDocumentsColumns
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocumentsColumns", "1, 2");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandXsl(storedDocumentSource);
		assertEquals(2, expandedSourceDocumentSources.size());

		// first and second columns tableDocumentsColumns
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocumentsColumns", "1; 2");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandXsl(storedDocumentSource);
		assertEquals(2, expandedSourceDocumentSources.size());

		// first and second columns (third doesn't is empty or doesn't exist) tableDocumentsColumns
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocumentsColumns", "1; 2, 3");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandXsl(storedDocumentSource);
		assertEquals(2, expandedSourceDocumentSources.size());
		
		// first and second columns (third doesn't is empty or doesn't exist) tableDocumentsColumns
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocumentsColumns", "a; 2, 3");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		boolean caught = false;
		try {
			expandedSourceDocumentSources = storedDocumentSourceExpander.expandXsl(storedDocumentSource);
			fail("We should have had an illegal argument exception.");
		}
		catch (IllegalArgumentException e) {
		}
		
		storage.destroy();
	}
}

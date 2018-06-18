package org.voyanttools.trombone.input.expand;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.voyanttools.trombone.input.expand.StoredDocumentSourceExpander;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class HtmlExpanderTest {

	@Test
	public void test() throws IOException {

		
		Storage storage = TestHelper.getDefaultTestStorage();
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		
		StoredDocumentSourceExpander storedDocumentSourceExpander;
		List<StoredDocumentSource> expandedSourceDocumentSources;
		StoredDocumentSource expandedStoredDocumentSource;
		String contents;
		
		FlexibleParameters parameters = new FlexibleParameters();

		List<StoredDocumentSource> sources = new ArrayList<StoredDocumentSource>();
		for (String string : new String[] {"short","nobody","malformed","twobodies"}) {
			sources.add(storedDocumentSourceStorage.getStoredDocumentSource(new FileInputSource(TestHelper.getResource("html/"+string+".html"))));
		}
		
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		for (StoredDocumentSource source : sources) {
			expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(source);
			assertEquals("HTML without expansion parameters should have one doc", 1, expandedSourceDocumentSources.size());
			expandedStoredDocumentSource = expandedSourceDocumentSources.get(0);
			assertEquals("HTML without expansion parameters should be original source", expandedStoredDocumentSource.getId(), source.getId());
			contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedStoredDocumentSource.getId()));
			assertTrue(contents.contains("résumé"));
		}
		
		parameters.setParameter("htmlDocumentsQuery", "p");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		for (StoredDocumentSource source : sources) {
			expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(source);
			assertEquals("HTML p selector should have two docs",2, expandedSourceDocumentSources.size());
			expandedStoredDocumentSource = expandedSourceDocumentSources.get(0);
			contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedStoredDocumentSource.getId()));
			assertTrue(contents.contains("résumé"));
		}

		// group by using valid value
		parameters.setParameter("htmlDocumentsQuery", "article article");
		parameters.setParameter("htmlGroupByQuery", "header p[class=author]");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		StoredDocumentSource storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(new FileInputSource(TestHelper.getResource("html/longer.html")));
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		assertEquals("2 docs (grouped by author)", 2, expandedSourceDocumentSources.size());
		expandedStoredDocumentSource = expandedSourceDocumentSources.get(0);
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedStoredDocumentSource.getId()));
		assertEquals("2 occurrences of author 1", 2, StringUtils.countMatches(contents, "Author 1"));
		
		// group by using valid value as @attr
		parameters.setParameter("htmlDocumentsQuery", "article article");
		parameters.setParameter("htmlGroupByQuery", "header p[class=author] @author");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(new FileInputSource(TestHelper.getResource("html/longer.html")));
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		assertEquals("2 docs (grouped by author)", 2, expandedSourceDocumentSources.size());
		expandedStoredDocumentSource = expandedSourceDocumentSources.get(0);
		contents = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedStoredDocumentSource.getId()));
		assertEquals("2 occurrences of author 1", 2, StringUtils.countMatches(contents, "Author 1"));
		
		// group by using invalid value
		parameters.setParameter("htmlDocumentsQuery", "article article");
		parameters.setParameter("htmlGroupByQuery", "header p[class=authorship]");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(new FileInputSource(TestHelper.getResource("html/longer.html")));
		expandedSourceDocumentSources = storedDocumentSourceExpander.getExpandedStoredDocumentSources(storedDocumentSource);
		assertEquals("3 docs (grouping by author failed)", 3, expandedSourceDocumentSources.size());
		storage.destroy();
	}

}

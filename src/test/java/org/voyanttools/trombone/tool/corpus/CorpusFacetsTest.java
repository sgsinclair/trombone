package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;

import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class CorpusFacetsTest {

	@Test
	public void test() throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();
		FlexibleParameters parameters = new FlexibleParameters();
		parameters.setParameter("file", TestHelper.getResource("xml/rss.xml").toString());
		parameters.setParameter("inputFormat", "RSS");
		parameters.setParameter("splitDocuments", "true");
		RealCorpusCreator creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		parameters.clear();
		parameters.setParameter("corpus", creator.getStoredId());
		
		CorpusFacets corpusFacets;
		parameters.setParameter("facet", "facet.author");
		corpusFacets = new CorpusFacets(storage, parameters);
		corpusFacets.run();

		parameters.setParameter("query", "Joe");
		corpusFacets = new CorpusFacets(storage, parameters);
		corpusFacets.run();

	}

}

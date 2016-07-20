package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.junit.Assert;
import org.junit.Test;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;

public class CorpusTermsTest {

	@Test
	public void test() throws IOException {
		Storage storage = new MemoryStorage();
		
		// add an additional document to the corpus
		Document document = new Document();
		document.add(new TextField("lexical", "dark and stormy night in document one", Field.Store.YES));
		storage.getLuceneManager().addDocument(document);
		
		RealCorpusCreator creator;
		FlexibleParameters parameters;
		
		parameters = new FlexibleParameters();
		parameters.addParameter("string",  "dark and stormy night in document one");
		parameters.addParameter("tool", "StepEnabledIndexedCorpusCreator");
		parameters.addParameter("noCache", 1);

		creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		String comparisonCorpusId = creator.getStoredId();

		parameters = new FlexibleParameters();
		parameters.addParameter("string",  "It was a dark and stormy night.");
		parameters.addParameter("string", "It was the best of times it was the worst of times.");
		parameters.addParameter("tool", "StepEnabledIndexedCorpusCreator");
		parameters.addParameter("noCache", 1);
		creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		String keepCorpusId = creator.getStoredId();
		
		parameters = new FlexibleParameters();
		parameters.addParameter("string",  "dark and stormy night in document three");
		parameters.addParameter("tool", "StepEnabledIndexedCorpusCreator");
		parameters.addParameter("noCache", 1);
		creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		
		parameters = new FlexibleParameters();
		parameters.setParameter("corpus", keepCorpusId);
		parameters.setParameter("tool", "CorpusTermFrequencies");
		
		CorpusTerm corpusTerm;
		CorpusTerms corpusTermFrequencies;
		List<CorpusTerm> corpusTerms;
		
		// we're expanding the term here
		parameters.setParameter("query", "dar*");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();		
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(1, corpusTerms.size());
		corpusTerm = corpusTerms.get(0);
		assertEquals("dar*", corpusTerm.getTerm());
		assertEquals(1, corpusTerm.getRawFreq());
		
		parameters.setParameter("query", "\"it was\"");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();		
		// we sort by reverse frequency by default
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(1, corpusTerms.size());
		corpusTerm = corpusTerms.get(0);
//		assertEquals(1, corpusTerm.getDocumentIndex());
		assertEquals("\"it was\"", corpusTerm.getTerm());
		assertEquals(3, corpusTerm.getRawFreq());

		// phrase with no quotes (treated as a phrase)
		parameters.setParameter("query", "it was");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();		
		// we sort by reverse frequency by default
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(1, corpusTerms.size());
		corpusTerm = corpusTerms.get(0);
//		assertEquals(1, corpusTerm.getDocumentIndex());
		assertEquals("\"it was\"", corpusTerm.getTerm());
		assertEquals(3, corpusTerm.getRawFreq());


		// we don't want "document" from the first document
		parameters.setParameter("query", "document");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();		
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(1, corpusTerms.size());
		corpusTerm = corpusTerms.get(0);
		assertEquals(0, corpusTerm.getRawFreq());
		
		parameters.setParameter("withDistributions", "true");
		parameters.setParameter("query", "document");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();		
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(1, corpusTerms.size());
		corpusTerm = corpusTerms.get(0);
		assertEquals(0, corpusTerm.getRawFreq());
		
		parameters.setParameter("query", "it");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();		
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(1, corpusTerms.size());
		corpusTerm = corpusTerms.get(0);
		assertEquals(3, corpusTerm.getRawFreq());
		parameters.removeParameter("withDistributions");
		
		parameters.setParameter("query", "dar*");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();		
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(1, corpusTerms.size());
		corpusTerm = corpusTerms.get(0);
		assertEquals("dar*", corpusTerm.getTerm());
		assertEquals(1, corpusTerm.getRawFreq());

		parameters.setParameter("query", "\"it was\"");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();		
		// we sort by reverse frequency by default
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(1, corpusTerms.size());
		corpusTerm = corpusTerms.get(0);
//		assertEquals(1, corpusTerm.getDocumentIndex());
		assertEquals("\"it was\"", corpusTerm.getTerm());
		assertEquals(3, corpusTerm.getRawFreq());

		// phrase with no quotes (treated as a phrase)
		parameters.setParameter("query", "it was");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();		
		// we sort by reverse frequency by default
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(1, corpusTerms.size());
		corpusTerm = corpusTerms.get(0);
//		assertEquals(1, corpusTerm.getDocumentIndex());
		assertEquals("\"it was\"", corpusTerm.getTerm());
		assertEquals(3, corpusTerm.getRawFreq());
		
		parameters.setParameter("query", "d[a-z]rk");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();		
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(1, corpusTerms.size());
		corpusTerm = corpusTerms.get(0);
		assertEquals("d[a-z]rk", corpusTerm.getTerm());
		assertEquals(1, corpusTerm.getRawFreq());

		// regex with wildcard
		parameters.setParameter("query", "d[a-z]rk*");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();		
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(1, corpusTerms.size());
		corpusTerm = corpusTerms.get(0);
		assertEquals("d[a-z]rk*", corpusTerm.getTerm());
		assertEquals(1, corpusTerm.getRawFreq());

		// two terms must occur
		parameters.setParameter("query", "+best +times");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();		
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(1, corpusTerms.size());
		corpusTerm = corpusTerms.get(0);
		assertEquals("+best +times", corpusTerm.getTerm());
		assertEquals(1, corpusTerm.getRawFreq());
		
		// all terms 
		parameters.removeParameter("query");
		parameters.removeParameter("limit");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(12, corpusTerms.size());
		for (CorpusTerm ct : corpusTerms) {
			if (ct.getTerm().equals("document")) { // make sure we don't have "document" from first doc
				Assert.assertFalse(ct.getTerm().equals("document"));
			}
		}
		
		corpusTerm = corpusTerms.get(0);
		assertEquals("it", corpusTerm.getTerm());
		assertEquals(3, corpusTerm.getRawFreq());

		// make sure same thing with distributions
		parameters.setParameter("withDistributions", "true");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(12, corpusTerms.size());
		for (CorpusTerm ct : corpusTerms) {
			if (ct.getTerm().equals("document")) { // make sure we don't have "document" from first doc
				Assert.assertFalse(ct.getTerm().equals("document"));
			}
		}
		parameters.removeParameter("withDistributions");
		

		// limit 1 (top frequency word)
		parameters.setParameter("limit", 1);
//		parameters.removeParameter("limit");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(1, corpusTerms.size());
		corpusTerm = corpusTerms.get(0);
		assertEquals("it", corpusTerm.getTerm());
		assertEquals(3, corpusTerm.getRawFreq());

		// start 1, limit 1
		parameters.setParameter("start", "1");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(1, corpusTerms.size());
		corpusTerm = corpusTerms.get(0);
		assertEquals("was", corpusTerm.getTerm());
		assertEquals(3, corpusTerm.getRawFreq());

		// start 50, limit 1 (empty)
		parameters.setParameter("start", "50");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(0, corpusTerms.size());
		
		// with stopwords
		parameters.removeParameter("start");
		parameters.removeParameter("limit");
		parameters.setParameter("stopList", "stop.en.taporware.txt");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(6, corpusTerms.size());
		
		// with bins, no query, more bins than tokens
		parameters.removeParameter("start");
		parameters.removeParameter("limit");
		parameters.removeParameter("stopList");
		parameters.setParameter("bins", 1000);
		parameters.setParameter("withDistributions", "true");
		parameters.removeParameter("query");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(parameters.getParameterIntValue("bins"), corpusTerms.get(0).getRawDistributions().length);
		assertEquals(12, corpusTerms.size());
		
		// with bins and query, more bins than tokens
		parameters.setParameter("withDistributions", "true");
		parameters.setParameter("query", "dark");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(parameters.getParameterIntValue("bins"), corpusTerms.get(0).getRawDistributions().length);
		assertEquals(1, corpusTerms.size());
		
		// testing with not
		parameters.setParameter("query", "-was,-dark,-light");
		parameters.removeParameter("withDistributions");
		parameters.setParameter("inDocumentsCountOnly", "true");
		parameters.removeParameter("bins");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(2, corpusTerms.get(0).getRawFreq()); // neither document has light
		assertEquals(1, corpusTerms.get(1).getRawFreq()); // dark occurs in 1
		assertEquals(0, corpusTerms.get(2).getRawFreq()); // was occurs in both
		assertEquals(3, corpusTerms.size());
		
		// testing with not – for now only concerned with inDocumentsCountOnly
		parameters.setParameter("query", "-was,-dark,-light");
		parameters.removeParameter("withDistributions");
		parameters.setParameter("inDocumentsCountOnly", "true");
		parameters.removeParameter("bins");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(2, corpusTerms.get(0).getRawFreq()); // neither document has light
		assertEquals(1, corpusTerms.get(1).getRawFreq()); // dark occurs in 1
		assertEquals(0, corpusTerms.get(2).getRawFreq()); // was occurs in both
		assertEquals(3, corpusTerms.size());
		
		// testing with not – for now only concerned with inDocumentsCountOnly
		parameters.setParameter("query", "^-wa*,-dar*,-ligh*");
		parameters.removeParameter("withDistributions");
		parameters.setParameter("inDocumentsCountOnly", "true");
		parameters.removeParameter("bins");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(2, corpusTerms.get(0).getRawFreq()); // neither document has light
		assertEquals(1, corpusTerms.get(1).getRawFreq()); // dark occurs in 1
		assertEquals(0, corpusTerms.get(2).getRawFreq()); // was occurs in both
		assertEquals(3, corpusTerms.size());
		
		
		// testing with not – for now only concerned with inDocumentsCountOnly
		parameters.setParameter("query", "string");
		parameters.addParameter("tokenType", "source");
		parameters.removeParameter("withDistributions");
		parameters.setParameter("inDocumentsCountOnly", "true");
		parameters.removeParameter("bins");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(2, corpusTerms.get(0).getRawFreq()); // neither document has light
		assertEquals(1, corpusTerms.size());
		
		
		// testing with not – for now only concerned with inDocumentsCountOnly
		parameters.setParameter("query", "d[a-z]rk");
		parameters.removeParameter("tokenType");
		parameters.removeParameter("withDistributions");
		parameters.setParameter("inDocumentsCountOnly", "true");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(1, corpusTerms.get(0).getRawFreq()); // neither document has light
		assertEquals(1, corpusTerms.size());
		
		// test comparison corpus
		parameters.setParameter("query", "it, document, and");
		parameters.setParameter("comparisonCorpus", comparisonCorpusId);
		parameters.setParameter("sort", "COMPARISONCORPUSRELATIVEFREQ");
		parameters.setParameter("dir", "DESC");
		parameters.removeParameter("inDocumentsCountOnly");
		corpusTermFrequencies = new CorpusTerms(storage, parameters);
		corpusTermFrequencies.run();
		corpusTerms = corpusTermFrequencies.getCorpusTerms();
		assertEquals(3, corpusTerms.size());
		assertEquals(0.157, corpusTerms.get(0).getComparisonCorpusRelativeFrequencyDifference(), .01); // it
		assertEquals(-0.090, corpusTerms.get(1).getComparisonCorpusRelativeFrequencyDifference(), .01); // and
		assertEquals(-0.142, corpusTerms.get(2).getComparisonCorpusRelativeFrequencyDifference(), .01); // document
		
		storage.destroy();
		
	}

}

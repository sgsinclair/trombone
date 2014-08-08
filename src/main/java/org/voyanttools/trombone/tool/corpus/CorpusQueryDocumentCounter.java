/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TotalHitCountCollector;
import org.voyanttools.trombone.lucene.LuceneDocumentFilter;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.lucene.search.FlexibleQueryParser;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class CorpusQueryDocumentCounter extends AbstractTerms {

	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusQueryDocumentCounter(Storage storage,
			FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	protected void runQueries(Corpus corpus,
			StoredToLuceneDocumentsMapper corpusMapper, String[] queries)
			throws IOException {
		AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getIndexReader());
		FlexibleQueryParser queryParser = new FlexibleQueryParser(atomicReader, storage.getLuceneManager().getAnalyzer());
		Map<String, Query> queriesMap = queryParser.getQueriesMap(queries, tokenType, true);

		Collector collector = new TotalHitCountCollector();
		Filter filter = new LuceneDocumentFilter();
		IndexSearcher indexSearcher;
//		indexSearcher.search(query, filter, collector);
	}

	@Override
	protected void runAllTerms(Corpus corpus,
			StoredToLuceneDocumentsMapper corpusMapper) throws IOException {
		// TODO Auto-generated method stub
		
	}

}

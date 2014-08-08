/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TotalHitCountCollector;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.lucene.search.FlexibleQueryParser;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("corpusQueryDocumentCounts")
public class CorpusQueryDocumentCounter extends AbstractTerms {
	
	Map<String, Integer> counts = new HashMap<String, Integer>();

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
		total = corpus.size();
		AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getIndexReader());
		FlexibleQueryParser queryParser = new FlexibleQueryParser(atomicReader, storage.getLuceneManager().getAnalyzer());
		Map<String, Query> queriesMap = queryParser.getQueriesMap(queries, tokenType, true);
		IndexSearcher indexSearcher = storage.getLuceneManager().getIndexSearcher();
		for (Map.Entry<String, Query> entries : queriesMap.entrySet()) {
			TotalHitCountCollector collector = new TotalHitCountCollector();
			indexSearcher.search(entries.getValue(), collector);
			counts.put(entries.getKey(), collector.getTotalHits());
		}
	}

	@Override
	protected void runAllTerms(Corpus corpus,
			StoredToLuceneDocumentsMapper corpusMapper) throws IOException {
		throw new IllegalArgumentException("You need to provide at least one query parameter for this tool");
	}

}

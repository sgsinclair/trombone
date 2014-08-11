/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TotalHitCountCollector;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.lucene.search.FlexibleQueryParser;
import org.voyanttools.trombone.lucene.search.SimpleDocIdsCollector;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author sgs
 *
 */
@XStreamAlias("corpusQueryDocumentFinder")
@XStreamConverter(CorpusQueryDocumentFinder.CorpusQueryDocumentFinderConverter.class)
public class CorpusQueryDocumentFinder extends AbstractTerms {
	
	boolean includeDocIds;
	Map<String, String[]> counts = new HashMap<String, String[]>();

	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusQueryDocumentFinder(Storage storage,
			FlexibleParameters parameters) {
		super(storage, parameters);
		includeDocIds = parameters.getParameterBooleanValue("includeDocIds");
	}
	
	@Override
	public void run() throws IOException {
		Corpus corpus = CorpusManager.getCorpus(storage, parameters);
		if (includeDocIds) {
			StoredToLuceneDocumentsMapper corpusMapper = new StoredToLuceneDocumentsMapper(storage, corpus.getDocumentIds());
			run(corpus,corpusMapper);
		}
		else {
			run(corpus, null);
		}
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
			SimpleDocIdsCollector collector = new SimpleDocIdsCollector();
			indexSearcher.search(entries.getValue(), collector);
			String[] ids = new String[collector.getTotalHits()];
			if (includeDocIds) {
				List<Integer> docIds = collector.getDocIds();
				for(int i=0, len=ids.length; i<len; i++) {
					ids[i] = corpusMapper.getDocumentIdFromLuceneDocumentIndex(docIds.get(i));
				}
			}
			counts.put(entries.getKey(), ids);
		}
	}

	@Override
	protected void runAllTerms(Corpus corpus,
			StoredToLuceneDocumentsMapper corpusMapper) throws IOException {
		throw new IllegalArgumentException("You need to provide at least one query parameter for this tool");
	}

	public static class CorpusQueryDocumentFinderConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return type.isAssignableFrom(CorpusQueryDocumentFinder.class);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer,
				MarshallingContext context) {
			CorpusQueryDocumentFinder finder = (CorpusQueryDocumentFinder) source;
			for (Map.Entry<String, String[]> count : finder.counts.entrySet()) {
				writer.startNode(count.getKey());
				writer.startNode("count");
				writer.setValue(String.valueOf(count.getValue().length));
				writer.endNode();
				writer.endNode();
			}
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader,
				UnmarshallingContext context) {
			// we don't unmarshal
			return null;
		}
		
	}
}

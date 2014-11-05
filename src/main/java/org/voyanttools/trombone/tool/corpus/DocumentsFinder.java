/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.lucene.search.FieldPrefixAwareSimpleQueryParser;
import org.voyanttools.trombone.lucene.search.SimpleDocIdsCollector;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author sgs
 *
 */
@XStreamAlias("documentsFinder")
@XStreamConverter(DocumentsFinder.DocumentsFinderConverter.class)
public class DocumentsFinder extends AbstractTerms {
	
	String corpusId = null;
	boolean includeDocIds;
	Map<String, String[]> queryDocumentidsMap = new HashMap<String, String[]>();

	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentsFinder(Storage storage,
			FlexibleParameters parameters) {
		super(storage, parameters);
		includeDocIds = parameters.getParameterBooleanValue("includeDocIds");
	}
	
	@Override
	public void run(Corpus corpus) throws IOException {
		corpusId = corpus.getId();
		runQueries(corpus, parameters.getParameterValues("query"));
	}

	protected void runQueries(Corpus corpus, String[] queries) throws IOException {
		total = corpus.size();
		IndexSearcher indexSearcher = storage.getLuceneManager().getIndexSearcher();
		SimpleQueryParser queryParser = new FieldPrefixAwareSimpleQueryParser(storage.getLuceneManager().getAnalyzer());
		boolean createNewCorpus = parameters.getParameterBooleanValue("createNewCorpus");
		for (String queryString : queries) {
			Query query = queryParser.parse(queryString);
			BooleanQuery corpusQuery = new BooleanQuery();
			corpusQuery.add(query, Occur.MUST);
			corpusQuery.add(new TermQuery(new Term("corpus", corpus.getId())), Occur.MUST);
			SimpleDocIdsCollector collector = new SimpleDocIdsCollector();
			indexSearcher.search(corpusQuery, collector);
			queryDocumentidsMap.put(query.toString(), collector.getDocIds().toArray(new String[0]));
		}
		if (createNewCorpus) {
			Set<String> ids = new HashSet<String>();
			for(String[] strings : queryDocumentidsMap.values()) {
				ids.addAll(Arrays.asList(strings));
			}
			if (ids.size()<corpus.size()) { // no need if we have all the documents
				List<String> keepers = new ArrayList<String>();
				for (String id : corpus.getDocumentIds()) {
					if (ids.contains(id)) {
						keepers.add(id);
					}
				}
				corpusId = storage.storeStrings(keepers);
				FlexibleParameters params = new FlexibleParameters(new String[]{"storedId="+corpusId,"nextCorpusCreatorStep=index"});
				RealCorpusCreator realCorpusCreator = new RealCorpusCreator(storage, params);
				realCorpusCreator.run(); // make sure to create corpus
				corpusId = realCorpusCreator.getStoredId();
			}
		}
	}
	
	@Override
	protected void runAllTerms(Corpus corpus) throws IOException {
		throw new IllegalArgumentException("You need to provide one or more queries for this tool.");
	}
	

	public static class DocumentsFinderConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return type.isAssignableFrom(DocumentsFinder.class);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer,
				MarshallingContext context) {
			DocumentsFinder finder = (DocumentsFinder) source;
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "corpus", String.class);
			writer.setValue(finder.corpusId);
			writer.endNode();
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "queries", Map.class);
			for (Map.Entry<String, String[]> count : finder.queryDocumentidsMap.entrySet()) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "queries", String.class); // not written in JSON
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "query", String.class);
				writer.setValue(count.getKey());
				writer.endNode();
				writer.startNode("count");
				writer.setValue(String.valueOf(count.getValue().length));
				writer.endNode();
				if (finder.includeDocIds) {
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "docIds", Map.class);
			        context.convertAnother(count.getValue());
			        writer.endNode();
				}
				writer.endNode();
			}
			writer.endNode();
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader,
				UnmarshallingContext context) {
			// we don't unmarshal
			return null;
		}
		
	}


}

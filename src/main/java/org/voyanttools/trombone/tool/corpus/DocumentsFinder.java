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
import java.util.UUID;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.search.FieldPrefixAwareSimpleQueryParser;
import org.voyanttools.trombone.lucene.search.LuceneDocIdsCollector;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.Keywords;
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
	
	private String corpusId = null;
	private boolean includeDocIds;
	private boolean withDistributions;
	private int[] distributions;
	Map<String, String[]> queryDocumentidsMap = new HashMap<String, String[]>();

	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentsFinder(Storage storage,
			FlexibleParameters parameters) {
		super(storage, parameters);
		includeDocIds = parameters.getParameterBooleanValue("includeDocIds");
		withDistributions = parameters.getParameterBooleanValue("withDistributions");
		distributions = new int[parameters.getParameterIntValue("bins", 0)];
	}

	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		total = corpus.size();
		
		IndexSearcher indexSearcher = corpusMapper.getSearcher();
		SimpleQueryParser queryParser = new FieldPrefixAwareSimpleQueryParser(corpusMapper.getAtomicReader(), storage.getLuceneManager().getAnalyzer());
		boolean createNewCorpus = parameters.getParameterBooleanValue("createNewCorpus");
		for (String queryString : getQueries(queries)) {
			Query query = queryParser.parse(queryString);
			LuceneDocIdsCollector collector = new LuceneDocIdsCollector();
			indexSearcher.search(query, corpusMapper, collector);
			if (createNewCorpus || includeDocIds || withDistributions) {
				Set<Integer> docs = collector.getLuceneDocIds();
				String[] ids = new String[docs.size()];
				int i =0;
				for (int doc : docs) {
					ids[i] = corpusMapper.getDocumentIdFromLuceneId(doc);
					i++;
				}
				queryDocumentidsMap.put(query.toString(), ids);
			}
			else {
				queryDocumentidsMap.put(query.toString(), new String[collector.getInDocumentsCount()]);
			}
		}
		if (withDistributions && distributions.length>0) {
			Set<String> matches = new HashSet<String>();
			for (Map.Entry<String, String[]> entry : queryDocumentidsMap.entrySet()) {
				matches.addAll(Arrays.asList(entry.getValue()));
			}
			List<String> ids = corpus.getDocumentIds();
			for (int i=0, len=ids.size(); i<len; i++) {
				if (matches.contains(ids.get(i))) {
					distributions[i*distributions.length/len]++;
				}
			}
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
				if (parameters.getParameterBooleanValue("temporaryCorpus")) {
					corpusId = "tmp."+UUID.randomUUID().toString()+corpusId;
					org.voyanttools.trombone.model.CorpusMetadata corpusMetadata = new org.voyanttools.trombone.model.CorpusMetadata(corpusId);
					corpusMetadata.setDocumentIds(ids);
					Corpus tempCorpus = new Corpus(storage, corpusMetadata);
					storage.getCorpusStorage().storeCorpus(tempCorpus);
				}
				else {
					FlexibleParameters params = new FlexibleParameters(new String[]{"storedId="+corpusId,"nextCorpusCreatorStep=corpus"});
					RealCorpusCreator realCorpusCreator = new RealCorpusCreator(storage, params);
					realCorpusCreator.run(); // make sure to create corpus
					corpusId = realCorpusCreator.getStoredId();
				}
				total = ids.size();
			}
		}
	}
	
	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
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
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "documentsCount", Integer.class);
			writer.setValue(String.valueOf(finder.total));
			writer.endNode();

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
				if (finder.withDistributions) {
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "distributions", Map.class);
			        context.convertAnother(finder.distributions);
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

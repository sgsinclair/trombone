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

import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.search.FieldPrefixAwareSimpleQueryParser;
import org.voyanttools.trombone.lucene.search.LuceneDocIdsCollector;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusAccess;
import org.voyanttools.trombone.model.CorpusAccessException;
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
	public float getVersion() {
		return super.getVersion()+5;
	}

	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		corpusId = corpus.getId();
		total = corpus.size();
		
		IndexSearcher indexSearcher = corpusMapper.getSearcher();
		boolean createNewCorpus = parameters.getParameterBooleanValue("createNewCorpus");
		
		Query query = getFacetAwareQuery(corpusMapper, queries);
		LuceneDocIdsCollector collector = new LuceneDocIdsCollector(corpusMapper);
		indexSearcher.search(query, collector);
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
				
				// make sure we have permissions to do this
				CorpusAccess corpusAccess = corpus.getValidatedCorpusAccess(parameters);
				if (corpusAccess==CorpusAccess.NONCONSUMPTIVE) {
					throw new CorpusAccessException("This tool isn't compatible with the limited access of this corpus.");
				}

				List<String> keepers = new ArrayList<String>();
				for (String id : corpus.getDocumentIds()) {
					if (ids.contains(id)) {
						keepers.add(id);
					}
				}
				corpusId = storage.storeStrings(keepers, Storage.Location.object);
				if (parameters.getParameterBooleanValue("temporaryCorpus")) {
					corpusId = "tmp."+UUID.randomUUID().toString()+corpusId;
					org.voyanttools.trombone.model.CorpusMetadata corpusMetadata = new org.voyanttools.trombone.model.CorpusMetadata(corpusId);
					corpusMetadata.setDocumentIds(ids);
					Corpus tempCorpus = new Corpus(storage, corpusMetadata);
					storage.getCorpusStorage().storeCorpus(tempCorpus, parameters);
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
	
	private Query getFacetAwareQuery(CorpusMapper corpusMapper, String[] queryStrings) throws IOException {
		
		FacetsConfig config = new FacetsConfig();
		SimpleQueryParser queryParser = new FieldPrefixAwareSimpleQueryParser(corpusMapper.getIndexReader(), storage.getLuceneManager().getAnalyzer(corpusMapper.getCorpus().getId()));
		

		Map<String, List<Query>> fieldedQueries = new HashMap<String, List<Query>>();
		for (String queryString : queryStrings) {
			if (queryString.startsWith("facet.") && queryString.contains(":")) {
				String field = queryString.substring(0, queryString.indexOf(":"));
				DrillDownQuery ddq = new DrillDownQuery(config);
				ddq.add(queryString.substring(0, queryString.indexOf(":")), queryString.substring(queryString.indexOf(":")+1));
				if (!fieldedQueries.containsKey(field)) {fieldedQueries.put(field, new ArrayList<Query>());}
				fieldedQueries.get(field).add(ddq);
			}
			else {
				Query query = queryParser.parse(queryString);
				String field = query.toString();
				if (query instanceof TermQuery) {field = ((TermQuery) query).getTerm().field();}
				else if (query instanceof PrefixQuery) {field = ((PrefixQuery) query).getField();}
				else if (query instanceof PhraseQuery) {field = ((PhraseQuery) query).getTerms()[0].field();}
				else {
					System.out.println(query);
				}
				if (!fieldedQueries.containsKey(field)) {fieldedQueries.put(field, new ArrayList<Query>());}
				fieldedQueries.get(field).add(query);
			}
		}

		
		List<Query> queries = new ArrayList<Query>();
		for (List<Query> queriesSet : fieldedQueries.values()) {
			if (queriesSet.size()==1) {queries.add(queriesSet.get(0));}
			else {
				BooleanQuery.Builder builder = new BooleanQuery.Builder();
				for (Query query : queriesSet) {
					builder.add(query, Occur.SHOULD);
				}
				queries.add(builder.build());
			}
		}
		
		if (queries.size()==1) {return queries.get(0);}
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for (Query query : queries) {
			builder.add(query, Occur.MUST);
		}
		return builder.build();
		
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
//	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "corpus", String.class);
//			writer.setValue(finder.corpusId);
//			writer.endNode();
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

/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.json.JSONArray;
import org.json.JSONObject;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.search.FieldPrefixAwareSimpleQueryParser;
import org.voyanttools.trombone.lucene.search.FieldPrefixAwareSimpleSpanQueryParser;
import org.voyanttools.trombone.model.Categories;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusAccess;
import org.voyanttools.trombone.model.CorpusAccessException;
import org.voyanttools.trombone.model.CorpusTermMinimalsDB;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.Storage.Location;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public abstract class AbstractCorpusTool extends AbstractTool {

	public AbstractCorpusTool(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.utils.RunnableTool#run()
	 */
	@Override
	public void run() throws IOException {
		Corpus corpus = CorpusManager.getCorpus(storage, parameters);
		CorpusMapper corpusMapper = getCorpusMapper(corpus);
		run(corpusMapper);
	}
	
	protected CorpusMapper getCorpusMapper(Corpus corpus) throws IOException {
		if (this instanceof CorpusMetadata == false && this instanceof DocumentsMetadata == false) {
			CorpusAccess corpusAccess = corpus.getValidatedCorpusAccess(parameters);
			if (corpusAccess==CorpusAccess.NONCONSUMPTIVE && this instanceof ConsumptiveTool) {
				throw new CorpusAccessException("This tool isn't compatible with the limited access of this corpus.");
			}
		}
		return new CorpusMapper(storage, corpus);
	}
	
	protected List<String> getCorpusStoredDocumentIdsFromParameters(Corpus corpus) throws IOException {
		
		List<String> ids = new ArrayList<String>();
		
		// add IDs
		for (String docId : parameters.getParameterValues("docId")) {
			if (docId.isEmpty()==false) {
				for (String id : docId.split(",")) {
					ids.add(id.trim());
				}
			}
		}
		
		// add indices
		// check first if we have real values
		String[] docIndices = parameters.getParameterValues("docIndex");
		if (docIndices.length>0 && docIndices[0].isEmpty()==false) {
			for (String docIndex : docIndices) {
				for (String index : docIndex.split(",")) {
					int i = Integer.parseInt(index.trim());
					ids.add(corpus.getDocument(i).getId());
				}
			}
		}
		
		// no docs defined, so consider all
		if (ids.isEmpty()) {
			ids.addAll(corpus.getDocumentIds());
		}
		
		return ids;
		
	}
	
	protected CorpusMapper getStoredToLuceneDocumentsMapper(Corpus corpus) throws IOException {
		return new CorpusMapper(storage, corpus);
	}
	
	public abstract void run(CorpusMapper corpusMapper) throws IOException;
	
	protected Map<String, SpanQuery> getCategoriesAwareSpanQueryMap(CorpusMapper corpusMapper, String[] queries) throws IOException {
		
		FieldPrefixAwareSimpleSpanQueryParser parser = new FieldPrefixAwareSimpleSpanQueryParser(corpusMapper.getIndexReader(), storage.getLuceneManager().getAnalyzer(corpusMapper.getCorpus().getId()), parameters.getParameterValue("tokenType", "lexical"));
		Map<String, SpanQuery> queriesMap;
		try {
			queriesMap = parser.getSpanQueriesMap(queries, false);
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse queries: "+StringUtils.join(queries, "; "), e);
		}
		
		String tokenType = parameters.getParameterValue("tokenType", "lexical");
		Map<String, Collection<String>> keysToQueryStrings = getKeyToQueryWords(corpusMapper, queriesMap.keySet());
		
		Pattern categoryPattern = Pattern.compile("^\\^?@(\\p{L}+)$");		
		
		for (Map.Entry<String, Collection<String>> entry : keysToQueryStrings.entrySet()) {
			String key = entry.getKey();
			if (queriesMap.containsKey(key)) {
				Matcher matcher = categoryPattern.matcher(key);
				if (matcher.find()) {
					String categoryMatch = matcher.group(1);
//					if (categories.categoryHasWord(categoryMatch)) {
						SpanQuery q = queriesMap.get(key);
						if (q instanceof SpanTermQuery) {
							if (((SpanTermQuery) q).getTerm().text().equals(categoryMatch)) {
								queriesMap.remove(key);
							}
						}
//					}
				}
			}
			
			List<SpanQuery> newQueries = new ArrayList<SpanQuery>();
			boolean isExpand = key.startsWith("^");
			for (String queryString : entry.getValue()) {
				if (isExpand) {
					queriesMap.put(queryString, new SpanTermQuery(new Term(tokenType, queryString)));
				} else {
					newQueries.add(new SpanTermQuery(new Term(tokenType, queryString)));
				}
			}
			if (newQueries.isEmpty()==false && isExpand==false) {
				queriesMap.put(key, new SpanOrQuery(newQueries.toArray(new SpanQuery[0])));
			}
		}
		
		// remove map items that are simply the category names
		Set<String> toBeRemoved = new HashSet<String>();
		for (String key : queriesMap.keySet()) {
			if (key.contains("@") && queriesMap.get(key) instanceof SpanTermQuery) {
				Matcher matcher = categoryPattern.matcher(key);
				if (matcher.find()) { 
					if (matcher.group(1).equals(((SpanTermQuery) queriesMap.get(key)).getTerm().text())) {
						toBeRemoved.add(key);
					}
				}
			}
		}
		for (String key : toBeRemoved) {
			queriesMap.remove(key);
		}

		return 	queriesMap;
		
	}
	
	protected Map<String, Query> getCategoriesAwareQueryMap(CorpusMapper corpusMapper, String[] queries) throws IOException {
		
		FieldPrefixAwareSimpleQueryParser parser = new FieldPrefixAwareSimpleQueryParser(corpusMapper.getIndexReader(), storage.getLuceneManager().getAnalyzer(corpusMapper.getCorpus().getId()), parameters.getParameterValue("tokenType", "lexical"));
		Map<String, Query> queriesMap;
		try {
			queriesMap = queriesMap = parser.getQueriesMap(queries, false);
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse queries: "+StringUtils.join(queries, "; "), e);
		}
		
		String tokenType = parameters.getParameterValue("tokenType", "lexical");
		Map<String, Collection<String>> keysToQueryStrings = getKeyToQueryWords(corpusMapper, queriesMap.keySet());
		for (Map.Entry<String, Collection<String>> entry : keysToQueryStrings.entrySet()) {
			String key = entry.getKey();
			BooleanQuery.Builder builder = new BooleanQuery.Builder();
			List<Query> newQueries = new ArrayList<Query>();
			for (String queryString : entry.getValue()) {
				builder.add(new TermQuery(new Term(tokenType, queryString)), BooleanClause.Occur.SHOULD);
			}
			if (newQueries.isEmpty()==false) {
				builder.add(queriesMap.get(key), BooleanClause.Occur.SHOULD);
				queriesMap.put(key, builder.build());
			}
		}
		return 	queriesMap;
	}
	
	private Map<String, Collection<String>> getKeyToQueryWords(CorpusMapper corpusMapper, Collection<String> keys) throws IOException {
		
		Map<String, Collection<String>> expandedWords = new HashMap<String, Collection<String>>();
		
		// determine if we need to check for categories
		String categoriesName = parameters.getParameterValue("categories", "");
		
		// no categories in parameters, so bail
		if (categoriesName.isEmpty()) {return expandedWords;}
		
		// no keys containing @ so bail
		if (keys.stream().noneMatch(s -> s.contains("@"))) {return expandedWords;}
		
		Categories categories = Categories.getCategories(storage, corpusMapper.getCorpus(), categoriesName);
		
//		String jsonString;
//		if (this.storage.existsDB(categoriesName)) {
//			jsonString = storage.retrieveString(categoriesName, Location.object);
//		} else {
//			throw new IllegalArgumentException("Unable to find specified categories: "+categoriesName);
//		}
//		
//		JSONObject all = new JSONObject(jsonString);
//		JSONObject cats = all.optJSONObject("categories");
		CorpusTermMinimalsDB corpusTermMinimalsDB = null;
		TokenType tokenType = TokenType.getTokenTypeForgivingly(parameters.getParameterValue("tokenType", "lexical"));

		
		
		Pattern categoryPattern = Pattern.compile("@(\\w+)");
//		Map<String, SpanQuery> newMap = new HashMap<String, SpanQuery>();
//		List<SpanQuery> validatedSpanTermQueries = new ArrayList<SpanQuery>();
		for (String key : keys) {
			if (key.contains("@")) {
				Matcher matcher = categoryPattern.matcher(key);
				while (matcher.find()) {
					if (categories.hasCategory(matcher.group(1))) {
						for (String word : categories.getCategory(matcher.group(1))) {
							if (word!=null & word.isEmpty()==false) {
								if (corpusTermMinimalsDB==null) {corpusTermMinimalsDB=CorpusTermMinimalsDB.getInstance(corpusMapper, tokenType);}
								if  (corpusTermMinimalsDB.exists(word)) {
									if (expandedWords.containsKey(key)==false) {
										expandedWords.put(key, new HashSet<String>());
									}
									expandedWords.get(key).add((String) word); 
								}
							}
						}
					}
				}
			}
		}
		
		// make sure to close if it's been used
		if (corpusTermMinimalsDB!=null) {
			corpusTermMinimalsDB.close();
		}
		
		return expandedWords;
	}

}

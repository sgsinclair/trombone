/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.voyanttools.trombone.lucene.search.FieldPrefixAwareSimpleQueryParser;
import org.voyanttools.trombone.lucene.search.SimpleDocIdsCollector;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.IndexedDocument.IndexedDocumentPriorityQueue;
import org.voyanttools.trombone.model.IndexedDocument.Sort;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("documentsMetadata")
public class DocumentsMetadata extends AbstractCorpusTool {
	
	private int total = 0;
	
	private List<IndexedDocument> documents = new ArrayList<IndexedDocument>();

	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentsMetadata(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	protected void run(Corpus corpus) throws IOException {
		
		Sort sort = IndexedDocument.Sort.getForgivingly(parameters);

		List<String> ids = new ArrayList<String>(); // maintain insertion order, especially for no queries
		if (parameters.containsKey("query")) {
			Map<String, Float> weights = new HashMap<String, Float>();
			weights.put("title", 1f);
			weights.put("author", 1f);
			SimpleQueryParser queryParser = new FieldPrefixAwareSimpleQueryParser(storage.getLuceneManager().getAnalyzer(), weights);
			
			IndexSearcher indexSearcher = storage.getLuceneManager().getIndexSearcher();
			Set<String> idsSet = new HashSet<String>();
			for (String queryString : parameters.getParameterValues("query")) {
				Query query = queryParser.parse(queryString);
				BooleanQuery corpusQuery = new BooleanQuery();
				corpusQuery.add(query, Occur.MUST);
				corpusQuery.add(new TermQuery(new Term("corpus", corpus.getId())), Occur.MUST);
				SimpleDocIdsCollector collector = new SimpleDocIdsCollector();
				indexSearcher.search(corpusQuery, collector);
				idsSet.addAll(collector.getDocIds());
			}
			
			if (sort==Sort.INDEXASC) {
				// insert them in order
				for (String id : corpus.getDocumentIds()) {
					if (idsSet.contains(id)) {ids.add(id);}
				}
			} else {
				ids.addAll(idsSet);
			}
		}
		else {
			ids.addAll(corpus.getDocumentIds());
		}
		total = ids.size();
		
		int start = parameters.getParameterIntValue("start", 0);
		int limit = parameters.getParameterIntValue("limit", Integer.MAX_VALUE);
		int size = start+limit;
		IndexedDocumentPriorityQueue queue = new IndexedDocument.IndexedDocumentPriorityQueue(size, sort);
		for (String id : ids) {
			IndexedDocument document = corpus.getDocument(id);
			document.getMetadata().setIndex(corpus.getDocumentPosition(id)); // make sure index is set
			queue.offer(document);
			if (queue.size()>=size && sort==Sort.INDEXASC) {break;} // we don't need to look any further since docs in order
		}
		
		for (int i=0, len = queue.size()-start; i<len; i++) {
			documents.add(queue.poll());
		}
		Collections.reverse(documents);

	}


}

/*******************************************************************************
 * Trombone is a flexible text processing and analysis library used
 * primarily by Voyant Tools (voyant-tools.org).
 * 
 * Copyright (©) 2007-2012 Stéfan Sinclair & Geoffrey Rockwell
 * 
 * This file is part of Trombone.
 * 
 * Trombone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.queries.CorpusFilter;
import org.voyanttools.trombone.lucene.search.FieldPrefixAwareSimpleQueryParser;
import org.voyanttools.trombone.lucene.search.FieldPrefixAwareSimpleSpanQueryParser;
import org.voyanttools.trombone.lucene.search.LuceneDocIdsCollector;
import org.voyanttools.trombone.lucene.search.SpanQueryParser;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.CorpusTermMinimal;
import org.voyanttools.trombone.model.CorpusTermMinimalsDB;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.FlexibleQueue;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
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
@XStreamAlias("corpusTerms")
@XStreamConverter(CorpusTerms.CorpusTermsConverter.class)
public class CorpusTerms extends AbstractTerms implements Iterable<CorpusTerm> {
	
	private List<CorpusTerm> terms = new ArrayList<CorpusTerm>();
	
	@XStreamOmitField
	private CorpusTerm.Sort corpusTermSort;
	
	@XStreamOmitField
	private Comparator<CorpusTerm> comparator;
	
	@XStreamOmitField
	private boolean withDistributions = false;
	
	@XStreamOmitField
	private int totalTokens = 0; // used to calculate relative frequencies
	
	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusTerms(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		withDistributions = parameters.getParameterBooleanValue("withDistributions");
		corpusTermSort = CorpusTerm.Sort.getForgivingly(parameters);
		comparator = CorpusTerm.getComparator(corpusTermSort);
	}
	
	public int getVersion() {
		return super.getVersion()+4;
	}

	protected void runAllTerms(Corpus corpus) throws IOException {
		
		Keywords stopwords = getStopwords(corpus);
		
		int size = start+limit;
		
		CorpusMapper corpusMapper = getStoredToLuceneDocumentsMapper(corpus);
		Bits docIdSet = corpusMapper.getDocIdBitSet();
		
		// now we look for our term frequencies
		Terms terms = corpusMapper.getAtomicReader().terms(tokenType.name());
		TermsEnum termsEnum = terms.iterator(null);
		DocsEnum docsEnum = null;
		
		FlexibleQueue<CorpusTerm> queue = new FlexibleQueue<CorpusTerm>(comparator, size);
		String termString;
		int tokensCounts[] = corpus.getTokensCounts(tokenType);
		int totalTokens = corpus.getTokensCount(tokenType);
		int corpusSize = corpus.size();
		while(true) {
			
			BytesRef term = termsEnum.next();
			
			if (term != null) {
				termString = term.utf8ToString();
				if (stopwords.isKeyword(termString)) {continue;}
				docsEnum = termsEnum.docs(docIdSet, docsEnum, DocsEnum.FLAG_FREQS);
				int doc = docsEnum.nextDoc();
				int termFreq = 0;
				int[] documentRawFreqs = new int[corpus.size()];
				float[] documentRelativeFreqs = new float[corpus.size()];
				int documentPosition = 0;
				int inDocumentsCount = 0;
				while(doc!=DocsEnum.NO_MORE_DOCS) {
					int freq = docsEnum.freq();
					if (freq>0) {
						termFreq += freq;
						this.totalTokens += freq;
						documentPosition = corpusMapper.getDocumentPositionFromLuceneId(doc);
						documentRawFreqs[documentPosition] = freq;
						documentRelativeFreqs[documentPosition] = (float) freq/tokensCounts[documentPosition];
						inDocumentsCount++;
					}
					doc = docsEnum.nextDoc();
				}
				if (termFreq>0) {
					total++;
					queue.offer(new CorpusTerm(termString, termFreq, totalTokens, inDocumentsCount, corpusSize, documentRawFreqs, documentRelativeFreqs));
				}
			}
			else {
				break; // no more terms
			}
		}
		this.terms.addAll(queue.getOrderedList(start));
	}
	
	@Override
	protected void runQueries(Corpus corpus, String[] queries) throws IOException {
		CorpusMapper corpusMapper = this.getStoredToLuceneDocumentsMapper(corpus);
		FlexibleQueue<CorpusTerm> queue = new FlexibleQueue<CorpusTerm>(comparator, start+limit);
		if (parameters.getParameterBooleanValue("inDocumentsCountOnly")) { // no spans required to count per-document frequencies
			FieldPrefixAwareSimpleQueryParser parser = new FieldPrefixAwareSimpleQueryParser(corpusMapper.getAtomicReader(), storage.getLuceneManager().getAnalyzer());
			Map<String, Query> queriesMap = parser.getQueriesMap(getQueries(), false);
			runQueriesInDocumentsCountOnly(corpusMapper, queue, queriesMap);
		}
		else {
			FieldPrefixAwareSimpleSpanQueryParser parser = new FieldPrefixAwareSimpleSpanQueryParser(corpusMapper.getAtomicReader(), storage.getLuceneManager().getAnalyzer());
			Map<String, SpanQuery> queriesMap = parser.getSpanQueriesMap(queries, false);
			runSpanQueries(corpusMapper, queue, queriesMap);
		}
		terms.addAll(queue.getOrderedList());
	}
		/*
		AtomicReader reader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getDirectoryReader());
		Filter corpusFilter = new CorpusFilter(corpus);
		Bits bits = corpusFilter.getDocIdSet(reader.getContext(), new Bits.MatchAllBits(reader.numDocs())).bits();
		IndexSearcher searcher = new IndexSearcher(reader);
		FieldPrefixAwareSimpleQueryParser parser = 
				parameters.getParameterBooleanValue("inDocumentsCountOnly") ? 
				new FieldPrefixAwareSimpleQueryParser(reader, storage.getLuceneManager().getAnalyzer()) :
				new FieldPrefixAwareSimpleSpanQueryParser(reader, storage.getLuceneManager().getAnalyzer());
		int totalTokens = corpus.getTokensCount(tokenType);
		int corpusSize = corpus.size();
		int size = start+limit;
		FlexibleQueue<CorpusTerm> queue = new FlexibleQueue<CorpusTerm>(comparator, size);
		Map<String, Query> queriesMap = parser.getQueriesMap(getQueries(), false);
		boolean needDistributions = withDistributions || corpusTermSort.needDistributions();
		Map<Term, TermContext> termContexts = new HashMap<Term, TermContext>();
		for (Map.Entry<String, Query> entry : queriesMap.entrySet()) {
			Query query = entry.getValue();
			boolean doSearch = true;
			if (!needDistributions && query instanceof TermQuery) { // we should be able to look it up directly without a search
				Term term = ((TermQuery) query).getTerm();
				if (term.field().equals(TokenType.lexical.name())) {
					CorpusTermMinimalsDB corpusTermMinimalsDB = new CorpusTermMinimalsDB(storage, corpus, TokenType.lexical, true);
					CorpusTermMinimal corpusTermMinimal = corpusTermMinimalsDB.get(term.text());
					CorpusTerm corpusTerm = new CorpusTerm(term.text(), corpusTermMinimal.getRawFreq(), totalTokens, corpusTermMinimal.getInDocumentsCount(), corpusSize);
					if (corpusTerm.getRawFreq()>0) {queue.offer(corpusTerm);}
					corpusTermMinimalsDB.close();
					doSearch = false;
				}
			}
			if (doSearch) {
				if (needDistributions && query instanceof SpanQuery) {
					Spans spans = ((SpanQuery) query).getSpans(reader.getContext(), bits, termContexts);
					addToQueueFromSpans(queue, entry.getKey(), spans, corpus);
//					addToQueueFromSpanQuery(queue, (SpanQuery) query, reader, corpus, corpusMapper);
				}
				else {
					LuceneDocIdsCollector collector = new LuceneDocIdsCollector();
					searcher.search(entry.getValue(), corpusFilter, collector);
					CorpusTerm corpusTerm = new CorpusTerm(entry.getKey(), collector.getRawFreq(), totalTokens, collector.getInDocumentsCount(), corpusSize);
					if (corpusTerm.getRawFreq()>0) {queue.offer(corpusTerm);}
				}
			}
		}
		this.terms.addAll(queue.getOrderedList());
		*/

	private void runSpanQueries(CorpusMapper corpusMapper, FlexibleQueue<CorpusTerm> queue, Map<String, SpanQuery> queriesMap) throws IOException {
		Map<Term, TermContext> termContexts = new HashMap<Term, TermContext>();
		boolean needDistributions = withDistributions || corpusTermSort.needDistributions();
		CorpusTermMinimalsDB corpusTermMinimalsDB = null; // only create it if we need it
		for (Map.Entry<String, SpanQuery> entry : queriesMap.entrySet()) {
			SpanQuery query = entry.getValue();
			String queryString = entry.getKey();
			if (needDistributions) {
				Spans spans = ((SpanQuery) query).getSpans(corpusMapper.getAtomicReader().getContext(), corpusMapper.getDocIdBitSet(), termContexts);
				addToQueueFromSpansWithDistributions(corpusMapper, queue, queryString, spans);
			}
			else if (query instanceof SpanTermQuery) {
				if (corpusTermMinimalsDB==null) {
					corpusTermMinimalsDB = CorpusTermMinimalsDB.getInstance(storage, corpusMapper.getAtomicReader(), corpusMapper.getCorpus(), ((SpanTermQuery) query).getTerm().field());
				}
				addToQueueFromTermWithoutDistributions(queue, queryString, ((SpanTermQuery) query).getTerm(), corpusTermMinimalsDB, corpusMapper.getCorpus().size());
			}
			else {
				addToQueueFromQueryWithoutDistributions(corpusMapper, queue, queryString, query);
			}
		}
		if (corpusTermMinimalsDB!=null) {corpusTermMinimalsDB.close();}
	}
	private void runQueriesInDocumentsCountOnly(CorpusMapper corpusMapper, FlexibleQueue<CorpusTerm> queue, Map<String, Query> queriesMap) throws IOException {
		CorpusTermMinimalsDB corpusTermMinimalsDB = null; // only create it if we need it
		for (Map.Entry<String, Query> entry : queriesMap.entrySet()) {
			Query query = entry.getValue();
			String queryString = entry.getKey();
			if (query instanceof TermQuery) {
				if (corpusTermMinimalsDB==null) {
					corpusTermMinimalsDB = CorpusTermMinimalsDB.getInstance(storage, corpusMapper.getAtomicReader(), corpusMapper.getCorpus(), ((TermQuery) query).getTerm().field());
				}
				addToQueueFromTermWithoutDistributions(queue, queryString, ((TermQuery) query).getTerm(), corpusTermMinimalsDB, corpusMapper.getCorpus().size());
			}
			else {
				addToQueueFromQueryWithoutDistributions(corpusMapper, queue, queryString, query);
			}
		}
		if (corpusTermMinimalsDB!=null) {corpusTermMinimalsDB.close();}
	}
	
	private void addToQueueFromTermWithoutDistributions(FlexibleQueue<CorpusTerm> queue, String queryString, Term term, CorpusTermMinimalsDB corpusTermMinimalsDB, int corpusSize) throws IOException {
		CorpusTermMinimal corpusTermMinimal = corpusTermMinimalsDB.get(term.text());
		if (corpusTermMinimal!=null) { // null indicates that query term doesn't exist
			CorpusTerm corpusTerm = new CorpusTerm(term.text(), corpusTermMinimal.getRawFreq(), totalTokens, corpusTermMinimal.getInDocumentsCount(), corpusSize);
			offer(queue, corpusTerm);
		}
	}

	private void addToQueueFromQueryWithoutDistributions(CorpusMapper corpusMapper, FlexibleQueue<CorpusTerm> queue, String queryString, Query query) throws IOException {
		LuceneDocIdsCollector collector = new LuceneDocIdsCollector();
		corpusMapper.getSearcher().search(query, corpusMapper.getCorpusFilter(), collector);
		CorpusTerm corpusTerm = new CorpusTerm(queryString, collector.getRawFreq(), totalTokens, collector.getInDocumentsCount(), corpusMapper.getCorpus().size());
		offer(queue, corpusTerm);
	}
	
	private void addToQueueFromSpansWithDistributions(CorpusMapper corpusMapper, FlexibleQueue<CorpusTerm> queue, String queryString, Spans spans) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		int docIndexInCorpus = -1; // this should always be changed on the first span
		int tokensCounts[] = corpus.getTokensCounts(TokenType.lexical);
		Map<Integer, AtomicInteger> positionsMap = new HashMap<Integer, AtomicInteger>();
		int lastDoc = -1;
		int totalTokens = corpus.getTokensCount(tokenType);
		while(spans.next()) {
			int doc = spans.doc();
			if (doc != lastDoc) {
				docIndexInCorpus = corpusMapper.getDocumentPositionFromLuceneId(doc);
				lastDoc = doc;
			}
			if (positionsMap.containsKey(docIndexInCorpus)==false) {
				positionsMap.put(docIndexInCorpus, new AtomicInteger(1));
			}
			else {
				positionsMap.get(docIndexInCorpus).incrementAndGet();
			}
		}
		int[] rawFreqs = new int[corpus.size()];
		float[] relativeFreqs = new float[corpus.size()];
		int freq = 0;
		for (Map.Entry<Integer, AtomicInteger> entry : positionsMap.entrySet()) {
			int f = entry.getValue().intValue();
			int documentPosition = entry.getKey();
			freq+=f;
			rawFreqs[documentPosition] = f;
			relativeFreqs[documentPosition] = (float) f/tokensCounts[documentPosition];
		}
		if (freq>0) { // we may have terms from other documents not in this corpus
			CorpusTerm corpusTerm = new CorpusTerm(queryString, freq, totalTokens, corpus.size(), corpus.size(), rawFreqs, relativeFreqs);
			offer(queue, corpusTerm);
		}
	}
	
	private void offer(FlexibleQueue<CorpusTerm> queue, CorpusTerm corpusTerm) {
		if (corpusTerm.getRawFreq()>0) {
			queue.offer(corpusTerm);
			total++;
			totalTokens+=corpusTerm.getRawFreq();
		}
	}

	/*
	protected void runQueriesOld(Corpus corpus, String[] queries) throws IOException {
		AtomicReader reader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getDirectoryReader());
		IndexSearcher searcher = new IndexSearcher(reader);
		StoredToLuceneDocumentsMapper corpusMapper = getStoredToLuceneDocumentsMapper(searcher, corpus);

		FieldPrefixAwareSimpleSpanQueryParser spanQueryParser = new FieldPrefixAwareSimpleSpanQueryParser(reader, storage.getLuceneManager().getAnalyzer());
		Map<String, SpanQuery> spanQueries = spanQueryParser.getSpanQueriesMap(queries, isQueryExpand);
//		SpanQueryParser spanQueryParser = new SpanQueryParser(reader, storage.getLuceneManager().getAnalyzer());
//		Map<String, SpanQuery> spanQueries = spanQueryParser.getSpanQueriesMap(queries, tokenType, isQueryCollapse);
		Map<Term, TermContext> termContexts = new HashMap<Term, TermContext>();
		Map<Integer, AtomicInteger> positionsMap = new HashMap<Integer, AtomicInteger>();
		int size = start+limit;
		FlexibleQueue<CorpusTerm> queue = new FlexibleQueue<CorpusTerm>(comparator, size);
		int lastDoc = -1;
		int docIndexInCorpus = -1; // this should always be changed on the first span
		int tokensCounts[] = corpus.getTokensCounts(TokenType.lexical);
		int totalTokens = corpus.getTokensCount(tokenType);
		for (Map.Entry<String, SpanQuery> spanQueryEntry : spanQueries.entrySet()) {
			String queryString = spanQueryEntry.getKey();
			SpanQuery spanQuery = spanQueryEntry.getValue();
			Spans spans = spanQuery.getSpans(reader.getContext(), corpusMapper.getDocIdOpenBitSet(), termContexts);			
			while(spans.next()) {
				int doc = spans.doc();
				if (doc != lastDoc) {
					docIndexInCorpus = corpusMapper.getDocumentPositionFromLuceneDocumentIndex(doc);
					lastDoc = doc;
				}
				if (positionsMap.containsKey(docIndexInCorpus)==false) {
					positionsMap.put(docIndexInCorpus, new AtomicInteger(1));
				}
				else {
					positionsMap.get(docIndexInCorpus).incrementAndGet();
				}
			}
			int[] rawFreqs = new int[corpus.size()];
			float[] relativeFreqs = new float[corpus.size()];
			int freq = 0;
			for (Map.Entry<Integer, AtomicInteger> entry : positionsMap.entrySet()) {
				int f = entry.getValue().intValue();
				int documentPosition = entry.getKey();
				freq+=f;
				rawFreqs[documentPosition] = f;
				relativeFreqs[documentPosition] = (float) f/tokensCounts[documentPosition];
			}
			if (freq>0) { // we may have terms from other documents not in this corpus
				this.totalTokens += freq;
				total++;
				queue.offer(new CorpusTerm(queryString, freq, (float) freq / totalTokens, rawFreqs, relativeFreqs));
			}
			positionsMap.clear(); // prepare for new entries
		}
		this.terms.addAll(queue.getOrderedList());
	}
	*/

	List<CorpusTerm> getCorpusTerms() {
		return terms;
	}

	@Override
	public Iterator<CorpusTerm> iterator() {
		return terms.iterator();
	}

	public static class CorpusTermsConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return CorpusTerms.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			CorpusTerms corpusTerms = (CorpusTerms) source;
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "total", Integer.class);
			writer.setValue(String.valueOf(corpusTerms.getTotal()));
			writer.endNode();
			
			FlexibleParameters parameters = corpusTerms.getParameters();
			String freqsMode = parameters.getParameterValue("withDistributions");
			
			boolean withRawDistributions = freqsMode != null && freqsMode.equals("raw");
			boolean withRelativeDistributions = freqsMode != null && !withRawDistributions && (freqsMode.equals("relative") || parameters.getParameterBooleanValue("withDistributions"));		
			int bins = parameters.getParameterIntValue("distributionBins");
			boolean inDocumentsCountOnly = parameters.getParameterBooleanValue("inDocumentsCountOnly");
			
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "terms", Map.class);
			for (CorpusTerm corpusTerm : corpusTerms) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "terms", String.class); // not written in JSON
		        
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "term", String.class);
				writer.setValue(corpusTerm.getTerm());
				writer.endNode();
				
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "inDocumentsCount", Integer.class);
				writer.setValue(String.valueOf(corpusTerm.getInDocumentsCount()));
				writer.endNode();
				
				if (!inDocumentsCountOnly) {
					
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
					writer.setValue(String.valueOf(corpusTerm.getRawFreq()));
					writer.endNode();


			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "relativeFreq", Float.class);
					writer.setValue(String.valueOf((float) corpusTerm.getRawFreq() / corpusTerms.totalTokens));
					writer.endNode();
					
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "relativePeakedness", Float.class);
					writer.setValue(String.valueOf(corpusTerm.getPeakedness()));
					writer.endNode();
					
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "relativeSkewness", Float.class);
					writer.setValue(String.valueOf(corpusTerm.getSkewness()));
					writer.endNode();
					
					if (withRawDistributions) {
				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "distributions", List.class);
				        context.convertAnother(corpusTerm.getRawDistributions(bins));
				        writer.endNode();
					}
					
					if (withRelativeDistributions) {
				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "distributions", List.class);
				        context.convertAnother(corpusTerm.getRelativeDistributions(bins));
				        writer.endNode();
					}
				}
				
				writer.endNode();
			}
			writer.endNode();
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader, com.thoughtworks.xstream.converters.UnmarshallingContext)
		 */
		@Override
		public Object unmarshal(HierarchicalStreamReader arg0,
				UnmarshallingContext arg1) {
			// TODO Auto-generated method stub
			return null;
		}

	}
}

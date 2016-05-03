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

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.search.FieldPrefixAwareSimpleQueryParser;
import org.voyanttools.trombone.lucene.search.FieldPrefixAwareSimpleSpanQueryParser;
import org.voyanttools.trombone.lucene.search.LuceneDocIdsCollector;
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
	
	@XStreamOmitField
	private CorpusTermMinimalsDB comparisonCorpusTermMinimals = null;
	
	@XStreamOmitField
	private int comparisonCorpusTotalTokens = 0;
	
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
		return super.getVersion()+13;
	}

	private FlexibleQueue<CorpusTerm> runAllTermsWithDistributionsDocumentTermVectors(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		FlexibleQueue<CorpusTerm> queue = new FlexibleQueue<CorpusTerm>(comparator, start+limit);
		
		LeafReader reader = corpusMapper.getLeafReader();
		Map<String, Map<Integer, Integer>> rawFreqsMap = new HashMap<String, Map<Integer, Integer>>();
		TermsEnum termsEnum = null;
		for (int doc : corpusMapper.getLuceneIds()) {
			Terms terms = reader.getTermVector(doc, tokenType.name());
			if (terms!=null) {
				termsEnum = terms.iterator();
				if (termsEnum!=null) {
					BytesRef bytesRef = termsEnum.next();
					while (bytesRef!=null) {
						String term = bytesRef.utf8ToString();
						if (!stopwords.isKeyword(term)) {
							if (!rawFreqsMap.containsKey(term)) {
								rawFreqsMap.put(term, new HashMap<Integer, Integer>());
							}
							rawFreqsMap.get(term).put(corpusMapper.getDocumentPositionFromLuceneId(doc), (int) termsEnum.totalTermFreq());
						}
						bytesRef = termsEnum.next();
					}
				}
			}
		}
		
		int corpusSize = corpusMapper.getCorpus().size();
		int[] tokensCounts = corpusMapper.getCorpus().getTokensCounts(tokenType);
		int totalCorpusTokens = corpusMapper.getCorpus().getTokensCount(tokenType);
		int bins = parameters.getParameterIntValue("bins", corpusSize);
		int[] documentRawFreqs;
		float[] documentRelativeFreqs;
		int documentPosition;
		int termFreq;
		int freq;
		for (Map.Entry<String, Map<Integer, Integer>> termsMap : rawFreqsMap.entrySet()) {
			String termString = termsMap.getKey();
			documentRawFreqs = new int[corpusSize];
			documentRelativeFreqs = new float[corpusSize];
			termFreq = 0;
			for (Map.Entry<Integer, Integer> docsMap : termsMap.getValue().entrySet()) {
				documentPosition = docsMap.getKey();
				freq = docsMap.getValue();
				termFreq+=freq;
				totalTokens+=freq;
				documentRawFreqs[documentPosition] = freq;
				documentRelativeFreqs[documentPosition] = (float) freq/tokensCounts[documentPosition];
			}
			total++;
			CorpusTerm corpusTerm = new CorpusTerm(termString, termFreq, totalTokens, termsMap.getValue().size(), corpusSize, documentRawFreqs, documentRelativeFreqs, bins);
			offer(queue, corpusTerm);
//			queue.offer(new CorpusTerm(termString, termFreq, totalTokens, termsMap.getValue().size(), corpusSize, documentRawFreqs, documentRelativeFreqs, bins));
		}
		return queue;
	}
	/**
	 * Offer all terms in the corpus without any distribution information (this is very efficient since it uses CorpusTermsMinimalDB map).
	 * @param corpusMapper
	 * @param stopwords
	 * @throws IOException
	 */
	private FlexibleQueue<CorpusTerm> runAllTermsWithoutDistributions(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		FlexibleQueue<CorpusTerm> queue = new FlexibleQueue<CorpusTerm>(comparator, start+limit);
		CorpusTermMinimalsDB corpusTermMinimalsDB = CorpusTermMinimalsDB.getInstance(corpusMapper, tokenType==TokenType.lexical ? tokenType.name() : parameters.getParameterValue("tokenType"));
		int totalTokens = corpusMapper.getCorpus().getTokensCount(tokenType);
		for (CorpusTermMinimal corpusTermMinimal : corpusTermMinimalsDB.values()) {
			if (!stopwords.isKeyword(corpusTermMinimal.getTerm())) {
				total++;
				this.totalTokens+=corpusTermMinimal.getRawFreq();
				CorpusTerm corpusTerm = new CorpusTerm(corpusTermMinimal, totalTokens);
				offer(queue, corpusTerm);
//				queue.offer(corpusTerm);
			}
		}
		corpusTermMinimalsDB.close();
		return queue;
	}
	
	private void createComparisonCorpusTermMinimals() throws IOException {
		String comparisonCorpusId = parameters.getParameterValue("comparisonCorpus", "");
		if (!comparisonCorpusId.isEmpty()) {
			Corpus comparisonCorpus = CorpusManager.getCorpus(storage, new FlexibleParameters(new String[]{"corpus="+comparisonCorpusId}));
			comparisonCorpusTotalTokens = comparisonCorpus.getTokensCount(TokenType.lexical);
			CorpusMapper comparisonCorpusMapper = new CorpusMapper(storage, comparisonCorpus);
			comparisonCorpusTermMinimals = CorpusTermMinimalsDB.getInstance(comparisonCorpusMapper, tokenType==TokenType.lexical ? tokenType.name() : parameters.getParameterValue("tokenType"));
		}
	}

	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		
		if (parameters.containsKey("comparisonCorpus")) {
			createComparisonCorpusTermMinimals();
		}
		try {
			FlexibleQueue<CorpusTerm> queue = withDistributions || corpusTermSort.needDistributions() ?
//					runAllTermsWithDistributionsFromReaderTerms(corpusMapper, stopwords) :
				runAllTermsWithDistributionsDocumentTermVectors(corpusMapper, stopwords) :
				runAllTermsWithoutDistributions(corpusMapper, stopwords);
			this.terms.addAll(queue.getOrderedList(start));
		} finally {
			if (comparisonCorpusTermMinimals!=null) {
				comparisonCorpusTermMinimals.close();
			}
		}

	}
	
	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		if (parameters.containsKey("comparisonCorpus")) {
			createComparisonCorpusTermMinimals();
		}
		try {
			FlexibleQueue<CorpusTerm> queue = new FlexibleQueue<CorpusTerm>(comparator, start+limit);
			if (parameters.getParameterBooleanValue("inDocumentsCountOnly")) { // no spans required to count per-document frequencies
				FieldPrefixAwareSimpleQueryParser parser = new FieldPrefixAwareSimpleQueryParser(corpusMapper.getLeafReader(), storage.getLuceneManager().getAnalyzer(), tokenType==TokenType.other ? parameters.getParameterValue("tokenType") : tokenType.name());
				Map<String, Query> queriesMap = parser.getQueriesMap(queries, false);
				runQueriesInDocumentsCountOnly(corpusMapper, queue, queriesMap);
			}
			else {
				FieldPrefixAwareSimpleSpanQueryParser parser = new FieldPrefixAwareSimpleSpanQueryParser(corpusMapper.getLeafReader(), storage.getLuceneManager().getAnalyzer(), tokenType==TokenType.other ? parameters.getParameterValue("tokenType") : tokenType.name());
				Map<String, SpanQuery> queriesMap = parser.getSpanQueriesMap(queries, false);
				runSpanQueries(corpusMapper, queue, queriesMap);
			}
			terms.addAll(queue.getOrderedList());
		} finally {
			if (comparisonCorpusTermMinimals!=null) {
				comparisonCorpusTermMinimals.close();
			}
		}
	}

	private void runSpanQueries(CorpusMapper corpusMapper, FlexibleQueue<CorpusTerm> queue, Map<String, SpanQuery> queriesMap) throws IOException {
		Map<Term, TermContext> termContexts = new HashMap<Term, TermContext>();
		boolean needDistributions = withDistributions || corpusTermSort.needDistributions();
		CorpusTermMinimalsDB corpusTermMinimalsDB = null; // only create it if we need it
		int totalTokens = corpusMapper.getCorpus().getTokensCount(tokenType);
		for (Map.Entry<String, SpanQuery> entry : queriesMap.entrySet()) {
			SpanQuery query = entry.getValue();
			String queryString = entry.getKey();

			boolean corpusTermOffered = false;
			if (needDistributions) {
				Spans spans = corpusMapper.getFilteredSpans((SpanQuery) query);
				if (spans!=null) {
					addToQueueFromSpansWithDistributions(corpusMapper, queue, queryString, spans);
					corpusTermOffered = true;
				}
			}
			else if (query instanceof SpanTermQuery) {
				if (corpusTermMinimalsDB==null) {
					corpusTermMinimalsDB = CorpusTermMinimalsDB.getInstance(corpusMapper, ((SpanTermQuery) query).getTerm().field());
				}
				Term term = ((SpanTermQuery) query).getTerm();
				CorpusTermMinimal corpusTermMinimal = corpusTermMinimalsDB.get(term.text());
				if (corpusTermMinimal!=null) {
					addToQueueFromTermWithoutDistributions(queue, queryString, term, corpusTermMinimalsDB, corpusMapper.getCorpus().size(), totalTokens);
					corpusTermOffered = true;
				}
			}
			else {
				
				// if we have a long list of SpanTermQueries then let's try to filter them first
				if (query instanceof SpanOrQuery) {
					int count = 0;
					SpanQuery[] queries = ((SpanOrQuery) query).getClauses();
					
					if (queries.length>10) { // 10 is a bit arbitrary, but anyway
						for (SpanQuery spanQuery : queries) {
							if (spanQuery instanceof SpanTermQuery) {count++;}
							else {break;}
						}
						
						// rewrite query to have only terms that occur in this corpus
						if (count==queries.length) {
							
							boolean inDocumentCountNotNeeded = parameters.containsKey("inDocumentsCount") && !parameters.getParameterBooleanValue("inDocumentsCount");
							if (corpusTermMinimalsDB==null) {
								corpusTermMinimalsDB = CorpusTermMinimalsDB.getInstance(corpusMapper, queries[0].getField());
							}
							query = new SpanOrQuery(); // create new query (even if we don't use it)
							count = 0; // reset count for rawFreq
							for (SpanQuery q : queries) {
								Term term = ((SpanTermQuery) q).getTerm(); // we can cast this since we tested earlier
								CorpusTermMinimal corpusTermMinimal = corpusTermMinimalsDB.get(term.text());
								if (corpusTermMinimal!=null) {
									if (inDocumentCountNotNeeded) {
										count+=corpusTermMinimal.getRawFreq();
									}
									else {
										((SpanOrQuery) query).addClause(q);
									}
								}
							}
							if (inDocumentCountNotNeeded) {
								CorpusTerm corpusTerm = new CorpusTerm(queryString, count, totalTokens, 0, corpusMapper.getCorpus().size());
								offer(queue, corpusTerm);
								continue; // we have to skip to next one
							}
							// otherwise use our new query below
							
						}						
					}

				}
				addToQueueFromQueryWithoutDistributions(corpusMapper, queue, queryString, query);
				corpusTermOffered = true;
			}
			if (!corpusTermOffered) { // offer empty term for this query
				CorpusTerm corpusTerm = new CorpusTerm(queryString, 0, totalTokens, 0, corpusMapper.getCorpus().size());
				offer(queue, corpusTerm);
			}
		}
		if (corpusTermMinimalsDB!=null) {corpusTermMinimalsDB.close();}
	}
	private void runQueriesInDocumentsCountOnly(CorpusMapper corpusMapper, FlexibleQueue<CorpusTerm> queue, Map<String, Query> queriesMap) throws IOException {
		Map<String, CorpusTermMinimalsDB> corpusTermMinimalsDBMap = new HashMap<String, CorpusTermMinimalsDB>();
//		CorpusTermMinimalsDB corpusTermMinimalsDB = null; // only create it if we need it
		int totalTokens = corpusMapper.getCorpus().getTokensCount(tokenType);
		for (Map.Entry<String, Query> entry : queriesMap.entrySet()) {
			Query query = entry.getValue();
			String queryString = entry.getKey();
			if (query instanceof TermQuery) {
				String field = ((TermQuery) query).getTerm().field();
				if (corpusTermMinimalsDBMap.containsKey(field)==false) {
					corpusTermMinimalsDBMap.put(field, CorpusTermMinimalsDB.getInstance(corpusMapper, field));
				}
				addToQueueFromTermWithoutDistributions(queue, queryString, ((TermQuery) query).getTerm(), corpusTermMinimalsDBMap.get(field), corpusMapper.getCorpus().size(), totalTokens);
			}
			else {
				addToQueueFromQueryWithoutDistributions(corpusMapper, queue, queryString, query);
			}
		}
		for (CorpusTermMinimalsDB corpusTermMinimalsDB : corpusTermMinimalsDBMap.values()) {
			corpusTermMinimalsDB.close();
		}
	}
	
	private void addToQueueFromTermWithoutDistributions(FlexibleQueue<CorpusTerm> queue, String queryString, Term term, CorpusTermMinimalsDB corpusTermMinimalsDB, int corpusSize, int totalTokens) throws IOException {
		CorpusTermMinimal corpusTermMinimal = corpusTermMinimalsDB.get(term.text());
		CorpusTerm corpusTerm = new CorpusTerm(queryString, corpusTermMinimal==null ? 0 : corpusTermMinimal.getRawFreq(), totalTokens, corpusTermMinimal==null ? 0 :corpusTermMinimal.getInDocumentsCount(), corpusSize);
		offer(queue, corpusTerm);
	}

	private void addToQueueFromQueryWithoutDistributions(CorpusMapper corpusMapper, FlexibleQueue<CorpusTerm> queue, String queryString, Query query) throws IOException {
		totalTokens = corpusMapper.getCorpus().getTokensCount(tokenType);

		LuceneDocIdsCollector collector = new LuceneDocIdsCollector();
		corpusMapper.getSearcher().search(corpusMapper.getFilteredQuery(query), collector);
		CorpusTerm corpusTerm = new CorpusTerm(queryString, collector.getRawFreq(), totalTokens, collector.getInDocumentsCount(), corpusMapper.getCorpus().size());
		offer(queue, corpusTerm);
	}
	
	private void addToQueueFromSpansWithDistributions(CorpusMapper corpusMapper, FlexibleQueue<CorpusTerm> queue, String queryString, Spans spans) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		int docIndexInCorpus = -1; // this should always be changed on the first span
		int tokensCounts[] = corpus.getTokensCounts(tokenType);
		Map<Integer, AtomicInteger> positionsMap = new HashMap<Integer, AtomicInteger>();
		int totalTokens = corpus.getTokensCount(tokenType);
		int doc = spans.nextDoc();
		while(doc!=Spans.NO_MORE_DOCS) {
			docIndexInCorpus = corpusMapper.getDocumentPositionFromLuceneId(doc);
			int pos = spans.nextStartPosition();
			while (pos!=Spans.NO_MORE_POSITIONS) {
				if (positionsMap.containsKey(docIndexInCorpus)==false) {
					positionsMap.put(docIndexInCorpus, new AtomicInteger(1));
				}
				else {
					positionsMap.get(docIndexInCorpus).incrementAndGet();
				}
				pos = spans.nextStartPosition();
			}
			doc = spans.nextDoc();
		}
		int[] rawFreqs = new int[corpus.size()];
		float[] relativeFreqs = new float[corpus.size()];
		int freq = 0;
		int inDocumentsCount = 0;
		for (Map.Entry<Integer, AtomicInteger> entry : positionsMap.entrySet()) {
			int f = entry.getValue().intValue();
			int documentPosition = entry.getKey();
			if (f>0) {
				freq+=f;
				inDocumentsCount++;
			}
			rawFreqs[documentPosition] = f;
			relativeFreqs[documentPosition] = (float) f/tokensCounts[documentPosition];
		}
		CorpusTerm corpusTerm = new CorpusTerm(queryString, freq, totalTokens, inDocumentsCount, corpus.size(), rawFreqs, relativeFreqs, parameters.getParameterIntValue("bins", corpus.size()));
		offer(queue, corpusTerm);
	}
	
	private void offer(FlexibleQueue<CorpusTerm> queue, CorpusTerm corpusTerm) {
		// we need to offer this even if rawfreq is 0 since we want to show query results for non matches
		if (comparisonCorpusTermMinimals!=null) {
			CorpusTermMinimal corpusTermMinimal = comparisonCorpusTermMinimals.get(corpusTerm.getTerm());
			if (corpusTermMinimal!=null && comparisonCorpusTotalTokens>0) {
				corpusTerm.setComparisonRelativeFrequency((float) corpusTermMinimal.getRawFreq() / (float) comparisonCorpusTotalTokens);
			} else {
				corpusTerm.setComparisonRelativeFrequency(0);
			}
		}
		queue.offer(corpusTerm);
		total++;
		totalTokens+=corpusTerm.getRawFreq();
	}

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
					
					if (withRawDistributions || withRelativeDistributions) {
						
				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "relativePeakedness", Float.class);
						writer.setValue(String.valueOf(corpusTerm.getPeakedness()));
						writer.endNode();
						
				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "relativeSkewness", Float.class);
						writer.setValue(String.valueOf(corpusTerm.getSkewness()));
						writer.endNode();
						
						if (withRawDistributions) {
					        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "distributions", List.class);
					        context.convertAnother(corpusTerm.getRawDistributions());
					        writer.endNode();
						}
						
						if (withRelativeDistributions) {
					        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "distributions", List.class);
					        context.convertAnother(corpusTerm.getRelativeDistributions());
					        writer.endNode();
						}
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

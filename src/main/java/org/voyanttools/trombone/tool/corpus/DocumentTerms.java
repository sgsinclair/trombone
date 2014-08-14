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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.lucene.search.SpanQueryParser;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentTerm;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.DocumentTermsQueue;
import org.voyanttools.trombone.util.FlexibleParameters;

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
@XStreamAlias("documentTerms")
@XStreamConverter(DocumentTerms.DocumentTermsConverter.class)
public class DocumentTerms extends AbstractTerms implements Iterable<DocumentTerm> {
	

	private List<DocumentTerm> terms = new ArrayList<DocumentTerm>();
	
	@XStreamOmitField
	private DocumentTerm.Sort documentTermsSort;
	
	@XStreamOmitField
	boolean withDistributions;
	
	@XStreamOmitField
	boolean isNeedsPositions;
	
	@XStreamOmitField
	boolean isNeedsOffsets;
	
	@XStreamOmitField
	int distributionBins;

	
	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentTerms(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		documentTermsSort = DocumentTerm.Sort.relativeFrequencyDesc;		
		withDistributions = parameters.getParameterBooleanValue("withDistributions");
		distributionBins = parameters.getParameterIntValue("bins", 10);
		isNeedsPositions = withDistributions || parameters.getParameterBooleanValue("withPositions");
		isNeedsOffsets = parameters.getParameterBooleanValue("withOffsets");
	}

	
	protected void runQueries(Corpus corpus, StoredToLuceneDocumentsMapper corpusMapper, String[] queries) throws IOException {
	
		AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getIndexReader());
		SpanQueryParser spanQueryParser = new SpanQueryParser(atomicReader, storage.getLuceneManager().getAnalyzer());
		Map<String, SpanQuery> spanQueries = spanQueryParser.getSpanQueriesMap(queries, tokenType, isQueryCollapse);
		Map<Term, TermContext> termContexts = new HashMap<Term, TermContext>();
		Map<Integer, List<Integer>> positionsMap = new HashMap<Integer, List<Integer>>();
		int size = start+limit;
		DocumentTermsQueue queue = new DocumentTermsQueue(size, documentTermsSort);
		int[] totalTokenCounts = corpus.getTokensCounts(tokenType);
		int lastDoc = -1;
		int docIndexInCorpus = -1; // this should always be changed on the first span
		Bits docIdSet = corpusMapper.getDocIdOpenBitSetFromStoredDocumentIds(this.getCorpusStoredDocumentIdsFromParameters(corpus));

		for (Map.Entry<String, SpanQuery> spanQueryEntry : spanQueries.entrySet()) {
			String queryString = spanQueryEntry.getKey();
			Spans spans = spanQueryEntry.getValue().getSpans(atomicReader.getContext(), docIdSet, termContexts);			
			while(spans.next()) {
				int doc = spans.doc();
				if (doc != lastDoc) {
					docIndexInCorpus = corpusMapper.getDocumentPositionFromLuceneDocumentIndex(doc);
					lastDoc = doc;
				}
				int start = spans.start();
				if (positionsMap.containsKey(docIndexInCorpus)==false) {
					positionsMap.put(docIndexInCorpus, new ArrayList<Integer>());
				}
				positionsMap.get(docIndexInCorpus).add(start);
			}
			for (Map.Entry<Integer, List<Integer>> entry : positionsMap.entrySet()) {
				List<Integer> positionsList = entry.getValue();
				int freq = positionsList.size();
				int[] positions = new int[positionsList.size()];
				for (int i=0; i<positions.length; i++) {
					positions[i] = positionsList.get(i);
				}
				int documentPosition = entry.getKey();
				String docId = corpusMapper.getDocumentIdFromDocumentPosition(documentPosition);

				total++;
				queue.offer(new DocumentTerm(documentPosition, docId, queryString, freq, totalTokenCounts[documentPosition], isNeedsPositions ? positions : null, null));
			}
			positionsMap.clear(); // prepare for new entries
		}
		setDocumentTermsFromQueue(queue);
	}

//	public DocumentTerms getAllTerms(Corpus corpus, StoredToLuceneDocumentsMapper corpusMapper) {
//		
//	}
	protected void runAllTerms(Corpus corpus, StoredToLuceneDocumentsMapper corpusMapper) throws IOException {
		
		Keywords stopwords = this.getStopwords(corpus);
		int size = start+limit;
		
		int[] totalTokensCounts = corpus.getTokensCounts(tokenType);
		Bits docIdSet = corpusMapper.getDocIdOpenBitSetFromStoredDocumentIds(this.getCorpusStoredDocumentIdsFromParameters(corpus));
		
		AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getIndexReader());
		
		// now we look for our term frequencies
		Terms terms = atomicReader.terms(tokenType.name());
		TermsEnum termsEnum = terms.iterator(null);
		DocsAndPositionsEnum docsAndPositionsEnum = null;
		DocumentTermsQueue queue = new DocumentTermsQueue(size, documentTermsSort);
		String termString;
		while(true) {
			
			BytesRef term = termsEnum.next();
			
			if (term != null) {
				termString = term.utf8ToString();
				if (stopwords.isKeyword(termString)) {continue;}				
				total+=termsEnum.docFreq();
				docsAndPositionsEnum = termsEnum.docsAndPositions(docIdSet, docsAndPositionsEnum, DocsAndPositionsEnum.FLAG_OFFSETS);
				int doc = docsAndPositionsEnum.nextDoc();
				while (doc != DocIdSetIterator.NO_MORE_DOCS) {
					int documentPosition = corpusMapper.getDocumentPositionFromLuceneDocumentIndex(doc);
					String docId = corpusMapper.getDocumentIdFromLuceneDocumentIndex(doc);
					int totalTokensCount = totalTokensCounts[documentPosition];
					int freq = docsAndPositionsEnum.freq();
//					if (freq>0) {total++;} // make sure we track that this could be a hit
					int[] positions = null;
					int[] offsets = null;
					if (isNeedsPositions || isNeedsOffsets) {
						positions = new int[freq];
						offsets = new int[freq];
						for (int i=0; i<freq; i++) {
							positions[i] = docsAndPositionsEnum.nextPosition();
							offsets[i] = docsAndPositionsEnum.startOffset();
						}
					}					
					queue.offer(new DocumentTerm(documentPosition, docId, termString, freq, totalTokensCount, positions, offsets));
					doc = docsAndPositionsEnum.nextDoc();
				}
			}
			else {
				break; // no more terms
			}
		}
		setDocumentTermsFromQueue(queue);
	}

	private void setDocumentTermsFromQueue(DocumentTermsQueue queue) {
		for (int i=0, len = queue.size()-start; i<len; i++) {
			terms.add(queue.poll());
		}
		Collections.reverse(terms);
	}

	public List<DocumentTerm> getDocumentTerms() {
		return terms;
	}


	@Override
	public Iterator<DocumentTerm> iterator() {
		return terms.iterator();
	}

	public static class DocumentTermsConverter implements Converter {


		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return DocumentTerms.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			DocumentTerms documentTerms = (DocumentTerms) source;
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "total", Integer.class);
			writer.setValue(String.valueOf(documentTerms.getTotal()));
			writer.endNode();
			
			FlexibleParameters parameters = documentTerms.getParameters();
			int bins = parameters.getParameterIntValue("distributionBins", 10);
			boolean withRawDistributions = parameters.getParameterBooleanValue("withDistributions");
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "terms", Map.class);
			for (DocumentTerm documentTerm : documentTerms) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "terms", String.class); // not written in JSON
		        
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "term", String.class);
				writer.setValue(documentTerm.getTerm());
				writer.endNode();
				
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
				writer.setValue(String.valueOf(documentTerm.getRawFrequency()));
				writer.endNode();

		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "relativeFreq", Float.class);
				writer.setValue(String.valueOf(documentTerm.getRelativeFrequency()));
				writer.endNode();
				
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "totalTermsCount", Integer.class);
				writer.setValue(String.valueOf(documentTerm.getTotalTermsCount()));
				writer.endNode();
				
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "docIndex", Integer.class);
				writer.setValue(String.valueOf(documentTerm.getDocIndex()));
				writer.endNode();
				
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "docId", String.class);
				writer.setValue(documentTerm.getDocId());
				writer.endNode();

				if (withRawDistributions) {
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "distributions", List.class);
			        context.convertAnother(documentTerm.getDistributions(bins));
			        writer.endNode();
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

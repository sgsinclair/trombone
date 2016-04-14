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

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.search.FieldPrefixAwareSimpleSpanQueryParser;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusAccess;
import org.voyanttools.trombone.model.CorpusAccessException;
import org.voyanttools.trombone.model.CorpusTermMinimal;
import org.voyanttools.trombone.model.CorpusTermMinimalsDB;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.DocumentTerm;
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
@XStreamAlias("documentTerms")
@XStreamConverter(DocumentTerms.DocumentTermsConverter.class)
public class DocumentTerms extends AbstractTerms implements Iterable<DocumentTerm> {
	

	private List<DocumentTerm> terms = new ArrayList<DocumentTerm>();
	
	@XStreamOmitField
	private DocumentTerm.Sort documentTermsSort;
	
	@XStreamOmitField
	private Comparator<DocumentTerm> comparator;
	
	@XStreamOmitField
	boolean withDistributions;
	
	@XStreamOmitField
	boolean isNeedsPositions;
	
	@XStreamOmitField
	boolean isNeedsOffsets;
	
	@XStreamOmitField
	int distributionBins;
	
	@XStreamOmitField
	int perDocLimit;

	
	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentTerms(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		documentTermsSort = DocumentTerm.Sort.getForgivingly(parameters);
		comparator = DocumentTerm.getComparator(documentTermsSort);
		withDistributions = parameters.getParameterBooleanValue("withDistributions");
		distributionBins = parameters.getParameterIntValue("bins", 10);
		isNeedsPositions = withDistributions || parameters.getParameterBooleanValue("withPositions");
		isNeedsOffsets = parameters.getParameterBooleanValue("withOffsets");
		perDocLimit = parameters.getParameterIntValue("perDocLimit", Integer.MAX_VALUE);
	}
	
	@Override
	public int getVersion() {
		return super.getVersion()+5;
	}

	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		
		FieldPrefixAwareSimpleSpanQueryParser parser = new FieldPrefixAwareSimpleSpanQueryParser(corpusMapper.getLeafReader(), storage.getLuceneManager().getAnalyzer(), tokenType==TokenType.other ? parameters.getParameterValue("tokenType") : tokenType.name());
		Map<String, SpanQuery> queriesMap = parser.getSpanQueriesMap(queries, false);

	
		Corpus corpus = corpusMapper.getCorpus();
		Map<Integer, List<Integer>> positionsMap = new HashMap<Integer, List<Integer>>();
		int size = start+limit;
		FlexibleQueue<DocumentTerm> queue = new FlexibleQueue<DocumentTerm>(comparator, size);
		int[] totalTokenCounts = corpus.getTokensCounts(tokenType);

		CorpusTermMinimalsDB corpusTermMinimalsDB = CorpusTermMinimalsDB.getInstance(corpusMapper, tokenType);
		
		BitSet bitset = corpusMapper.getBitSetFromDocumentIds(this.getCorpusStoredDocumentIdsFromParameters(corpus));
		
		for (Map.Entry<String, SpanQuery> spanQueryEntry : queriesMap.entrySet()) {
			String queryString = spanQueryEntry.getKey();
			CorpusTermMinimal corpusTermMinimal = corpusTermMinimalsDB.get(queryString);
			Spans spans = corpusMapper.getFilteredSpans(spanQueryEntry.getValue(), bitset);
			int doc = spans.nextDoc();
			while(doc!=Spans.NO_MORE_DOCS) {
					int docIndexInCorpus = corpusMapper.getDocumentPositionFromLuceneId(doc);
					positionsMap.put(docIndexInCorpus, new ArrayList<Integer>());
					int pos = spans.nextStartPosition();
					while (pos!=spans.NO_MORE_POSITIONS) {
						positionsMap.get(docIndexInCorpus).add(pos);
						pos = spans.nextStartPosition();
					}
				doc = spans.nextDoc();
			}
			FlexibleQueue<DocumentTerm> docQueue = new FlexibleQueue<DocumentTerm>(comparator, limit);
			for (Map.Entry<Integer, List<Integer>> entry : positionsMap.entrySet()) {
				List<Integer> positionsList = entry.getValue();
				int freq = positionsList.size();
				int[] positions = new int[positionsList.size()];
				for (int i=0; i<positions.length; i++) {
					positions[i] = positionsList.get(i);
				}
				int documentPosition = entry.getKey();
				String docId = corpusMapper.getDocumentIdFromDocumentPosition(documentPosition);
				DocumentMetadata documentMetadata = corpus.getDocument(docId).getMetadata();
				float mean = documentMetadata.getTypesCountMean(tokenType);
				float stdDev = documentMetadata.getTypesCountStdDev(tokenType);

				if (freq>0) {
					total++;
					float zscore = stdDev != 0 ? ((float) freq - mean / stdDev) : Float.NaN;
					DocumentTerm documentTerm = new DocumentTerm(documentPosition, docId, queryString, freq, totalTokenCounts[documentPosition], zscore, positions, null, corpusTermMinimal);
					docQueue.offer(documentTerm);
					
				}
			}
			int i = 0;
			for (DocumentTerm docTerm : docQueue.getOrderedList()) {
				queue.offer(docTerm);
				if (++i>=perDocLimit) {break;}
			}
			positionsMap.clear(); // prepare for new entries
		}
		corpusTermMinimalsDB.close();
		terms.addAll(queue.getOrderedList(start));
	}

	
	private void runAllTermsFromDocumentTermVectors(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		FlexibleQueue<DocumentTerm> queue = new FlexibleQueue<DocumentTerm>(comparator, start+limit);
		LeafReader reader = corpusMapper.getLeafReader();
		Corpus corpus = corpusMapper.getCorpus();
		CorpusTermMinimalsDB corpusTermMinimalsDB = CorpusTermMinimalsDB.getInstance(corpusMapper, tokenType);
		TermsEnum termsEnum = null;
		DocsAndPositionsEnum docsAndPositionsEnum = null;
		Bits docIdBitSet =  corpusMapper.getBitSetFromDocumentIds(this.getCorpusStoredDocumentIdsFromParameters(corpus));
		Bits allBits = new Bits.MatchAllBits(reader.numDocs());
		int[] tokenCounts = corpus.getTokensCounts(tokenType);
		float[] typesCountMeans = corpus.getTypesCountMeans(tokenType);
		float[] typesCountStdDev = corpus.getTypesCountStdDevs(tokenType);
		for (int doc : corpusMapper.getLuceneIds()) {
			if (!docIdBitSet.get(doc)) {continue;}
			FlexibleQueue<DocumentTerm> docQueue = new FlexibleQueue<DocumentTerm>(comparator, limit*docIdBitSet.length());
			int documentPosition = corpusMapper.getDocumentPositionFromLuceneId(doc);
			String docId = corpusMapper.getDocumentIdFromLuceneId(doc);
			DocumentMetadata metadata = corpus.getDocument(docId).getMetadata();
			float mean = typesCountMeans[documentPosition];
			float stdDev = typesCountStdDev[documentPosition];
			int totalTokensCount = tokenCounts[documentPosition];
			Terms terms = reader.getTermVector(doc, "lexical");
			if (terms!=null) {
				termsEnum = terms.iterator();
				if (termsEnum!=null) {
					BytesRef bytesRef = termsEnum.next();
					
					while (bytesRef!=null) {
						String termString = bytesRef.utf8ToString();
						if (!stopwords.isKeyword(termString)) {
							CorpusTermMinimal corpusTermMinimal = corpusTermMinimalsDB.get(termString);
							if (!stopwords.isKeyword(termString)) {
								int[] positions = null;
								int[] offsets = null;
								int freq;
								if (isNeedsPositions || isNeedsOffsets) {
									docsAndPositionsEnum = termsEnum.docsAndPositions(allBits, docsAndPositionsEnum, DocsAndPositionsEnum.FLAG_OFFSETS);
									docsAndPositionsEnum.nextDoc();
									freq = docsAndPositionsEnum.freq();
									positions = new int[freq];
									offsets = new int[freq];
									for (int i=0; i<freq; i++) {
										positions[i] = docsAndPositionsEnum.nextPosition();
										offsets[i] = docsAndPositionsEnum.startOffset();
									}
								}
								else {
									freq = (int) termsEnum.totalTermFreq();
								}
								total+=freq;
								float zscore = stdDev != 0 ? ((float) freq - mean / stdDev) : Float.NaN;
								DocumentTerm documentTerm = new DocumentTerm(documentPosition, docId, termString, freq, totalTokensCount, zscore, positions, offsets, corpusTermMinimal);
								docQueue.offer(documentTerm);
							}
						}
						bytesRef = termsEnum.next();
					}
				}
			}
			int i = 0;
			for (DocumentTerm docTerm : docQueue.getOrderedList()) {
				queue.offer(docTerm);
				if (++i>=perDocLimit) {break;}
			}
		}
		corpusTermMinimalsDB.close();
		this.terms.addAll(queue.getOrderedList(start));
	}

	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		// don't allow non-consumptive access to all terms if we need positions or offsets
		if (parameters.getParameterBooleanValue("withPositions") || parameters.getParameterBooleanValue("withOffsets")) {
			CorpusAccess corpusAccess = corpusMapper.getCorpus().getValidatedCorpusAccess(parameters);
			if (corpusAccess==CorpusAccess.NONCONSUMPTIVE) {
				throw new CorpusAccessException("This is requesting data that's incompatible with the limited access of this corpus.");
			}
		}
		runAllTermsFromDocumentTermVectors(corpusMapper, stopwords);
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
			String freqsMode = parameters.getParameterValue("withDistributions");			
			boolean withRawDistributions = freqsMode != null && freqsMode.equals("raw");
			boolean withRelativeDistributions = freqsMode != null && !withRawDistributions && (freqsMode.equals("relative") || parameters.getParameterBooleanValue("withDistributions"));		
			int bins = parameters.getParameterIntValue("bins", 10);
			boolean withOffsets = parameters.getParameterBooleanValue("withOffsets");
			boolean withPositions = parameters.getParameterBooleanValue("withPositions");
			
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
				
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "zscore", Float.class);
				writer.setValue(String.valueOf(documentTerm.getZscore()));
				writer.endNode();
				
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "zscoreRatio", Float.class);
				writer.setValue(String.valueOf(documentTerm.getZscoreRatio()));
				writer.endNode();
				
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "tfidf", Float.class);
				writer.setValue(String.valueOf(documentTerm.getTfIdf()));
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
			        context.convertAnother(documentTerm.getRawDistributions(bins));
			        writer.endNode();
				}
				if (withRelativeDistributions) {
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "distributions", List.class);
			        context.convertAnother(documentTerm.getRelativeDistributions(bins));
			        writer.endNode();
				}
				
				if (withOffsets) {
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "offsets", List.class);
			        context.convertAnother(documentTerm.getOffsets());
			        writer.endNode();
				}
				
				if (withPositions) {
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "positions", List.class);
			        context.convertAnother(documentTerm.getPositions());
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

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.search.SpanQueryParser;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentNgram;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.FlexibleQueue;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public class DocumentNgrams extends AbstractTerms {
	
	@XStreamOmitField
	private int minLength;
	
	@XStreamOmitField
	private int minRawFreq;
	
	@XStreamOmitField
	private int maxLength;
	
	private List<DocumentNgram> ngrams = new ArrayList<DocumentNgram>();
	
	@XStreamOmitField
	private Comparator<DocumentNgram> comparator;
	
	private enum Filter {
		NONE, LENGTHFIRST, RAWFREQFIRST, POSITIONFIRST;
		private static Filter getForgivingly(FlexibleParameters parameters) {
			String filter = parameters.getParameterValue("overlapFilter", "").toUpperCase();
			String sortPrefix = "NONE"; // default
			if (filter.startsWith("LENGTH")) {return LENGTHFIRST;}
			else if (filter.startsWith("RAWFREQ")) {return RAWFREQFIRST;}
			else {return NONE;}
		}
	}


	public DocumentNgrams(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		minLength = parameters.getParameterIntValue("minLength", 2);
		maxLength = parameters.getParameterIntValue("maxLength", Integer.MAX_VALUE);
		minRawFreq = parameters.getParameterIntValue("minRawFreq", 2);
		DocumentNgram.Sort sort = DocumentNgram.Sort.getForgivingly(parameters);
		comparator = DocumentNgram.getComparator(sort);
	}
	
	@Override
	public int getVersion() {
		return super.getVersion()+1;
	}
	

	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		this.ngrams = getNgrams(corpusMapper, stopwords, queries);
	}
	

	List<DocumentNgram> getNgrams(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		SpanQueryParser spanQueryParser = new SpanQueryParser(corpusMapper.getLeafReader(), storage.getLuceneManager().getAnalyzer());
		Corpus corpus = corpusMapper.getCorpus();
		Map<String, SpanQuery> spanQueries = spanQueryParser.getSpanQueriesMap(queries, tokenType, isQueryCollapse);
		Map<Term, TermContext> termContexts = new HashMap<Term, TermContext>();
		int docIndexInCorpus; // this should always be changed on the first span
		Map<Integer, Map<String, List<int[]>>> docTermPositionsMap = new HashMap<Integer, Map<String, List<int[]>>>();
		
		for (Map.Entry<String, SpanQuery> spanQueryEntry : spanQueries.entrySet()) {
//			CorpusTermMinimal corpusTermMinimal = corpusTermMinimalsDB.get(queryString);
			Spans spans = corpusMapper.getFilteredSpans(spanQueryEntry.getValue());
			Map<Integer, List<int[]>> documentAndPositionsMap = new HashMap<Integer, List<int[]>>();
			int doc = spans.nextDoc();
			while(doc!=spans.NO_MORE_DOCS) {
				int pos = spans.nextStartPosition();
				docIndexInCorpus = corpusMapper.getDocumentPositionFromLuceneId(doc);
				documentAndPositionsMap.put(docIndexInCorpus, new ArrayList<int[]>());
				while(pos!=spans.NO_MORE_POSITIONS) {
					documentAndPositionsMap.get(docIndexInCorpus).add(new int[]{spans.startPosition(), spans.endPosition()});
					pos = spans.nextStartPosition();
				}
				doc = spans.nextDoc();
			}
			String queryString = spanQueryEntry.getKey();
			for (Map.Entry<Integer, List<int[]>> entry : documentAndPositionsMap.entrySet()) {
				doc = entry.getKey();
				if (docTermPositionsMap.containsKey(doc)==false) {
					docTermPositionsMap.put(doc, new HashMap<String, List<int[]>>());
				}
				docTermPositionsMap.get(doc).put(queryString, entry.getValue());
			}
			documentAndPositionsMap.clear();
		}
		
		int[] totalTokens = corpus.getLastTokenPositions(tokenType);
		StringBuilder realTermBuilder = new StringBuilder();
		String realTerm;
		List<DocumentNgram> allNgrams = new ArrayList<DocumentNgram>();
		OverlapFilter filter = getDocumentNgramsOverlapFilter(parameters);
		for (Map.Entry<Integer, Map<String, List<int[]>>> docEntry : docTermPositionsMap.entrySet()) {
			docIndexInCorpus = docEntry.getKey();
			SimplifiedTermInfo[] sparseSimplifiedTermInfoArray = getSparseSimplifiedTermInfoArray(corpusMapper, corpusMapper.getLuceneIdFromDocumentPosition(docIndexInCorpus), totalTokens[docIndexInCorpus]);
			Map<String, List<int[]>> realStringsMap = new HashMap<String, List<int[]>>();
			for (Map.Entry<String, List<int[]>> termEntry : docEntry.getValue().entrySet()) {
//				new Ngram(docIndexInCorpus, term, positions, length)
				for (int[] positions : termEntry.getValue()) {
					for (int i=positions[0]; i<positions[1]; i++) {
						realTermBuilder.append(sparseSimplifiedTermInfoArray[i].term).append(" ");
					}
					realTerm = realTermBuilder.toString().trim();
					realTermBuilder.setLength(0);
					if (realStringsMap.containsKey(realTerm) == false) {
						realStringsMap.put(realTerm, new ArrayList<int[]>());
					}
					realStringsMap.get(realTerm).add(new int[]{positions[0], positions[1]-1});
				}
			}
			List<DocumentNgram> ngrams = new ArrayList<DocumentNgram>();
			for (Map.Entry<String, List<int[]>> realTermMap : realStringsMap.entrySet()) {
				List<int[]> values = realTermMap.getValue();
				DocumentNgram ngram = new DocumentNgram(docIndexInCorpus, realTermMap.getKey(), values, values.get(0)[1]+1-values.get(0)[0]);
				ngrams.add(new DocumentNgram(docIndexInCorpus, realTermMap.getKey(), values, values.get(0)[1]+1-values.get(0)[0]));
			}
			
			// we need to go through our first list to see if any of them are long enough
			List<DocumentNgram> nextNgrams = getNextNgrams(ngrams, sparseSimplifiedTermInfoArray, docIndexInCorpus, 2);
			for (DocumentNgram ngram : ngrams) {
				if (ngram.getLength()>=minLength && ngram.getLength()<=maxLength) {
					nextNgrams.add(ngram);
				}
			}
			
			//ngrams = getFilteredNgrams(ngrams, totalTokens[docIndexInCorpus]);
			allNgrams.addAll(filter.getFilteredNgrams(nextNgrams, totalTokens[docIndexInCorpus]));
		}
		
		FlexibleQueue<DocumentNgram> queue = new FlexibleQueue<DocumentNgram>(comparator, start+limit);
		for (DocumentNgram ngram : allNgrams) {
			if (ngram.getLength()>=minLength && ngram.getLength()<=maxLength) {
				queue.offer(ngram);
			}
		}
		return queue.getOrderedList(start);
	}

	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		this.ngrams.addAll(getNgrams(corpusMapper, stopwords));
	}

	List<DocumentNgram> getNgrams(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		int[] totalTokens = corpus.getLastTokenPositions(tokenType);
		FlexibleQueue<DocumentNgram> queue = new FlexibleQueue<DocumentNgram>(comparator, start+limit);
		
		OverlapFilter filter = getDocumentNgramsOverlapFilter(parameters);
		DocIdSetIterator it = corpusMapper.getDocIdSet().iterator();
		while (it.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
			int luceneDoc = it.docID();
			int corpusDocumentIndex = corpusMapper.getDocumentPositionFromLuceneId(luceneDoc);
			int lastToken = totalTokens[corpusDocumentIndex];	
			
			// build single grams as seed for ngrams
			SimplifiedTermInfo[] sparseSimplifiedTermInfoArray = getSparseSimplifiedTermInfoArray(corpusMapper, luceneDoc, lastToken);
			
			Map<String, List<int[]>> stringPositionsMap = new HashMap<String, List<int[]>>();
			for (int i=0, len=sparseSimplifiedTermInfoArray.length; i<len; i++) {
				if (sparseSimplifiedTermInfoArray[i]!=null && sparseSimplifiedTermInfoArray[i].term.isEmpty()==false) {
					if (stringPositionsMap.containsKey(sparseSimplifiedTermInfoArray[i].term)==false) {
						List<int[]> l = new ArrayList<int[]>();
						l.add(new int[]{i,i});
						stringPositionsMap.put(sparseSimplifiedTermInfoArray[i].term, l);
					}
					else {
						stringPositionsMap.get(sparseSimplifiedTermInfoArray[i].term).add(new int[]{i,i});
					}
				}
			}
			
			List<DocumentNgram> ngrams = getNgramsFromStringPositions(stringPositionsMap, corpusDocumentIndex, 1);
			ngrams = getNextNgrams(ngrams, sparseSimplifiedTermInfoArray, corpusDocumentIndex, 2);
			
			ngrams = filter.getFilteredNgrams(ngrams, lastToken);
			
			for (DocumentNgram ngram : ngrams) {
				if (ngram.getLength()>=minLength && ngram.getLength()<=maxLength) {
					queue.offer(ngram);
				}
			}
		}
		
		return queue.getOrderedList(start);
		
		
	}
	
	private List<DocumentNgram> getNextNgrams(List<DocumentNgram> ngrams, SimplifiedTermInfo[] sparseSimplifiedTermInfoArray, int corpusDocumentIndex, int length) {
		Map<String, List<int[]>> stringPositionsMap = new HashMap<String, List<int[]>>();
		for (DocumentNgram ngram : ngrams) {
			for (int[] positions : ngram.getPositions()) {
				for (int i=positions[1]+1; i<sparseSimplifiedTermInfoArray.length;i++) {
					if (sparseSimplifiedTermInfoArray[i]!=null) {
						if (sparseSimplifiedTermInfoArray[i].term.isEmpty()) {break;} // non repeating word
						String term = ngram.getTerm()+" "+sparseSimplifiedTermInfoArray[i].term;
						int[] newint = new int[]{positions[0], i};
						if (stringPositionsMap.containsKey(term)==false) {
							List<int[]> list = new ArrayList<int[]>();
							list.add(newint);
							stringPositionsMap.put(term, list);
						}
						else {
							stringPositionsMap.get(term).add(newint);
						}
						break;
					}
				}
			}
		}
		List<DocumentNgram> newngrams = getNgramsFromStringPositions(stringPositionsMap, corpusDocumentIndex, length);
		if (newngrams.isEmpty()==false) {
			newngrams.addAll(getNextNgrams(newngrams, sparseSimplifiedTermInfoArray, corpusDocumentIndex, length+1));
		}
		return newngrams;
	}
	

	private List<DocumentNgram> getNgramsFromStringPositions(Map<String, List<int[]>> stringPositionsMap, int corpusDocumentIndex, int length) {
		List<DocumentNgram> ngrams = new ArrayList<DocumentNgram>();
		for (Map.Entry<String, List<int[]>> stringPositions : stringPositionsMap.entrySet()) {
			List<int[]> values = stringPositions.getValue();
			if (values.size()>=minRawFreq) {
				ngrams.add(new DocumentNgram(corpusDocumentIndex, stringPositions.getKey(), values, values.get(0)[1]+1-values.get(0)[0]));
			}
		}
		return ngrams;
	}

	/*
	private List<Gram> getFilteredGrams(List<Gram> grams, int lastToken) {
		
		// sort by length
		Collections.sort(grams, new Comparator<Gram>()  {
		    public int compare(Gram g1, Gram g2) {
		    	if (g1.getLength()==g2.getLength()) {
		    		return g1.getStart() > g2.getStart() ? 1 : g1.getStart() < g2.getStart() ? 1 : 0;
		    	}
		    	return g1.getLength() > g2.getLength() ? -1 : 1;
		    }
		});
		
		boolean[] occupied = new boolean[lastToken];
		for (int i=0; i<limit; i++) {
			System.out.println(grams.get(i));
		}
		return grams;
		
	}
	*/
	
	
	/*
	private List<Gram> getNextGrams(String[] termsArray, List<Gram> grams) {
		System.err.println(grams.get(0).getLength()+" "+grams.size());
		List<Gram> newgrams = new ArrayList<Gram>();
		Map<String, List<Gram>> newgramcandidates = new HashMap<String, List<Gram>>();
		for (Gram gram : grams) {
			int lastOffset = gram.getEnd();
			for (int i = lastOffset+1, len=termsArray.length; i<len; i++) {
				String term = termsArray[i];
				if (term!=null) { // keep looking if it's null
					if (term.isEmpty()) {break;} // bail if it's an empty string
					String string = gram.getTerm()+" "+term;
					Gram g = new Gram(gram.getCorpusDocumentIndex(), string, gram.getStart(), i, gram.getLength()+1);
					if (newgramcandidates.containsKey(string)==false) {newgramcandidates.put(string, new ArrayList<Gram>());}
					newgramcandidates.get(string).add(g);
					break;
				}
			}
		}
		for (Map.Entry<String, List<Gram>> newgramcandidate : newgramcandidates.entrySet()) {
			List<Gram> list = newgramcandidate.getValue();
			if (list.size()>=minRawFreq) { // only add if there are multiple instances
				newgrams.addAll(list);
			}
		}
		if (newgrams.isEmpty()==false) {
			newgrams.addAll(getNextGrams(termsArray, newgrams));
		}
		return newgrams;
	}
	*/

	private SimplifiedTermInfo[] getSparseSimplifiedTermInfoArray(CorpusMapper corpusMapper, int luceneDoc, int lastTokenOffset) throws IOException {
		
		Keywords stopwords = this.getStopwords(corpusMapper.getCorpus());
		Terms terms = corpusMapper.getLeafReader().getTermVector(luceneDoc, tokenType.name());
		TermsEnum termsEnum = terms.iterator();
		SimplifiedTermInfo[] simplifiedTermInfoArray = new SimplifiedTermInfo[lastTokenOffset+1];
		while(true) {
			BytesRef term = termsEnum.next();
			if (term!=null) {
				String termString = term.utf8ToString();
				//if (stopwords.isKeyword(termString)) {continue;} // treat as whitespace or punctuation
				DocsAndPositionsEnum docsAndPositionsEnum = termsEnum.docsAndPositions(null, null, DocsAndPositionsEnum.FLAG_OFFSETS);
				while(docsAndPositionsEnum.nextDoc() != DocsAndPositionsEnum.NO_MORE_DOCS) {
					int freq = docsAndPositionsEnum.freq();
					for (int i=0, len = freq; i<len; i++) {
						int pos = docsAndPositionsEnum.nextPosition();
						new SimplifiedTermInfo(termString, pos, 1, freq, docsAndPositionsEnum.startOffset(), docsAndPositionsEnum.endOffset());
						simplifiedTermInfoArray[pos] = freq>1 ? new SimplifiedTermInfo(termString, pos, 1, freq, docsAndPositionsEnum.startOffset(), docsAndPositionsEnum.endOffset())  : new SimplifiedTermInfo(""); // empty string if not repeating
					}
				}
			}
			else {break;}
		}
		return simplifiedTermInfoArray;
	}
	
	private OverlapFilter getDocumentNgramsOverlapFilter(FlexibleParameters parameters) {
		Filter filter = Filter.getForgivingly(parameters);
		switch (filter) {
			case LENGTHFIRST: return new LengthFirstOverlapFilter();
			case RAWFREQFIRST: return new RawFreqFirstOverlapFilter();
			default: return new NoOverlapFilter();
		}
	}


	private interface OverlapFilter {
		List<DocumentNgram> getFilteredNgrams(List<DocumentNgram> ngrams, int lastToken);
	}
	
	private abstract class FirstOverlapFilter implements OverlapFilter {
		private Comparator<DocumentNgram> comparator;
		private FirstOverlapFilter(Comparator<DocumentNgram> comparator) {
			this.comparator = comparator;
		}
		public List<DocumentNgram> getFilteredNgrams(List<DocumentNgram> ngrams, int lastToken) {
			Collections.sort(ngrams, comparator);
			List<DocumentNgram> filteredNgrams = new ArrayList<DocumentNgram>();
			boolean[] occupied = new boolean[lastToken];
			for (DocumentNgram ngram : ngrams) {
				boolean keep = true;
				for (int[] positions : ngram.getPositions()) {
					for (int i=positions[0]; i<positions[1]+1; i++) {
						if (i>=lastToken || occupied[i]) {
							keep=false;
							break;
						}
						else {occupied[i]=true;}
					}
				}
				if (keep) {
					filteredNgrams.add(ngram);
					if (filteredNgrams.size()>=limit) {break;}
				}
			}
			return filteredNgrams;		
		}
	}
	
	private class NoOverlapFilter implements OverlapFilter {
		public List<DocumentNgram> getFilteredNgrams(List<DocumentNgram> ngrams, int lastToken) {
			return ngrams;
		}
	}
	private class LengthFirstOverlapFilter extends FirstOverlapFilter {
		private LengthFirstOverlapFilter() {
			super(DocumentNgram.getComparator(DocumentNgram.Sort.LENGTHDESC));
		}
	}
	private class RawFreqFirstOverlapFilter extends FirstOverlapFilter {
		private RawFreqFirstOverlapFilter() {
			super(DocumentNgram.getComparator(DocumentNgram.Sort.RAWFREQDESC));
		}
	}

	private class SimplifiedTermInfo {
		private String term;
		private int pos;
		private int length; // number of tokens
		private int freq;
		private int startOffset;
		private int endOffset;
		private SimplifiedTermInfo(String term, int pos, int length, int freq, int startOffset, int endOffset) {
			this.term = term;
			this.pos = pos;
			this.length = length;
			this.freq = freq;
			this.startOffset = startOffset;
			this.endOffset = endOffset;
			
		}
		public SimplifiedTermInfo(String string) {
			this(string, 0, 0, 0, 0, 0);
		}
	}


	List<DocumentNgram> getNgrams() {
		return ngrams;
	}
}

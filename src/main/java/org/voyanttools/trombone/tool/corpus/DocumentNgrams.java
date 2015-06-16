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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.search.SpanQueryParser;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusTermMinimal;
import org.voyanttools.trombone.model.DocumentTerm;
import org.voyanttools.trombone.model.Gram;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.Ngram;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.FlexibleQueue;

/**
 * @author sgs
 *
 */
public class DocumentNgrams extends AbstractTerms {
	
	private int minLength;
	
	private int minRawFreq;
	
	private List<Ngram> ngrams = new ArrayList<Ngram>();

	public DocumentNgrams(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		minLength = parameters.getParameterIntValue("minLength", 1);
		minRawFreq = parameters.getParameterIntValue("minRawFreq", 2);
	}

	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		SpanQueryParser spanQueryParser = new SpanQueryParser(corpusMapper.getAtomicReader(), storage.getLuceneManager().getAnalyzer());
		Corpus corpus = corpusMapper.getCorpus();
		Map<String, SpanQuery> spanQueries = spanQueryParser.getSpanQueriesMap(queries, tokenType, isQueryCollapse);
		Map<Term, TermContext> termContexts = new HashMap<Term, TermContext>();
//		Map<Integer, List<Integer>> positionsMap = new HashMap<Integer, List<Integer>>();
//		int size = start+limit;
////		FlexibleQueue<DocumentTerm> queue = new FlexibleQueue<DocumentTerm>(comparator, size);
//		int[] totalTokenCounts = corpus.getTokensCounts(tokenType);
		int lastDoc = -1;
		int docIndexInCorpus = -1; // this should always be changed on the first span
		Bits docIdSet = corpusMapper.getDocIdOpenBitSetFromStoredDocumentIds(this.getCorpusStoredDocumentIdsFromParameters(corpus));
		Map<Integer, Map<String, List<int[]>>> docTermPositionsMap = new HashMap<Integer, Map<String, List<int[]>>>();
		for (Map.Entry<String, SpanQuery> spanQueryEntry : spanQueries.entrySet()) {
//			CorpusTermMinimal corpusTermMinimal = corpusTermMinimalsDB.get(queryString);
			Spans spans = spanQueryEntry.getValue().getSpans(corpusMapper.getAtomicReader().getContext(), docIdSet, termContexts);	
			Map<Integer, List<int[]>> documentAndPositionsMap = new HashMap<Integer, List<int[]>>();
			while(spans.next()) {
				int doc = spans.doc();
				if (doc != lastDoc) {
					docIndexInCorpus = corpusMapper.getDocumentPositionFromLuceneId(doc);
					documentAndPositionsMap.put(docIndexInCorpus, new ArrayList<int[]>());
					lastDoc = doc;
				}
				documentAndPositionsMap.get(docIndexInCorpus).add(new int[]{spans.start(), spans.end()});
			}
			String queryString = spanQueryEntry.getKey();
			for (Map.Entry<Integer, List<int[]>> entry : documentAndPositionsMap.entrySet()) {
				int doc = entry.getKey();
				if (docTermPositionsMap.containsKey(doc)==false) {
					docTermPositionsMap.put(doc, new HashMap<String, List<int[]>>());
				}
				docTermPositionsMap.get(doc).put(queryString, entry.getValue());
			}
		}
		
		int[] totalTokens = corpus.getLastTokenPositions(tokenType);
		StringBuilder realStringBuilder = new StringBuilder();
		String realString;
		for (Map.Entry<Integer, Map<String, List<int[]>>> docEntry : docTermPositionsMap.entrySet()) {
			docIndexInCorpus = docEntry.getKey();
			SimplifiedTermInfo[] sparseSimplifiedTermInfoArray = getSparseSimplifiedTermInfoArray(corpusMapper, corpusMapper.getLuceneIdFromDocumentPosition(docIndexInCorpus), totalTokens[docIndexInCorpus]);
			List<SimplifiedTermInfo> simplifiedTermInfosToKeepList = new ArrayList<SimplifiedTermInfo>();
			Map<String, List<int[]>> realStringsMap = new HashMap<String, List<int[]>>();
			for (Map.Entry<String, List<int[]>> termEntry : docEntry.getValue().entrySet()) {
//				new Ngram(docIndexInCorpus, term, positions, length)
				for (int[] positions : termEntry.getValue()) {
					for (int i=positions[0]; i<positions[1]; i++) {
						realStringBuilder.append(sparseSimplifiedTermInfoArray[i].term).append(" ");
					}
					realString = realStringBuilder.toString();
					realStringBuilder.setLength(0);
					if (realStringsMap.containsKey(realString) == false) {
						realStringsMap.put(realString, new ArrayList<int[]>());
					}
					realStringsMap.get(realString).add(new int[]{positions[0], positions[1]-1});
				}
			}
			List<Ngram> ngrams = new ArrayList<Ngram>();
			for (Map.Entry<String, List<int[]>> realStringMap : realStringsMap.entrySet()) {
				List<int[]> values = realStringMap.getValue();
				ngrams.add(new Ngram(docIndexInCorpus, realStringMap.getKey(), values, values.get(0)[1]+1-values.get(0)[0]));
			}
			ngrams = getNextNgrams(ngrams, sparseSimplifiedTermInfoArray, docIndexInCorpus, 2);			
			//ngrams = getFilteredNgrams(ngrams, totalTokens[docIndexInCorpus]);
			this.ngrams.addAll(ngrams);
		}
	}

	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		int[] totalTokens = corpus.getLastTokenPositions(tokenType);
		
		DocIdSetIterator it = corpusMapper.getDocIdBitSet().iterator();
		while (it.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
			int luceneDoc = it.docID();
			int corpusDocumentIndex = corpusMapper.getDocumentPositionFromLuceneId(luceneDoc);
			int lastToken = totalTokens[corpusDocumentIndex];	
			
			// build single grams as seed for ngrams
			SimplifiedTermInfo[] sparseSimplifiedTermInfoArray = getSparseSimplifiedTermInfoArray(corpusMapper, luceneDoc, lastToken);
			
			Map<String, List<int[]>> stringPositionsMap = new HashMap<String, List<int[]>>();
			List<Gram> grams = new ArrayList<Gram>();
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
			
			List<Ngram> ngrams = getNgramsFromStringPositions(stringPositionsMap, corpusDocumentIndex, 1);
			ngrams = getNextNgrams(ngrams, sparseSimplifiedTermInfoArray, corpusDocumentIndex, 2);
			
			ngrams = getFilteredNgrams(ngrams, lastToken);
			
			/*
			String document = corpus.getDocument(corpusDocumentIndex).getDocumentString();
//			String document = atomicReader.document(luceneDoc).get(tokenType.name());
			StringBuilder positionsBuilder = new StringBuilder();
			for (int i=0, ilen=ngrams.size(); i<ilen; i++) {
				Ngram ngram = ngrams.get(i);
				positionsBuilder.setLength(0); 
				String original = "";
				for (int j=0, jlen= ngram.getPositions().size(); j<jlen; j++) {
					int[] positions = ngram.getPositions().get(j);
					positionsBuilder.append(positions[0]).append(",").append(positions[1]);
					if (j+1<jlen) positionsBuilder.append(";");
					if (j==0) {
						original = document.substring(sparseSimplifiedTermInfoArray[positions[0]].startOffset, sparseSimplifiedTermInfoArray[positions[1]].endOffset).replaceAll("\\s+", " ");
					}
				}
			}
			*/
			/*
			System.out.println("{\n\tlastPosition: "+lastToken+",\n\tstrings: [");
			for (int i=0, ilen=ngrams.size(); i<ilen; i++) {
				Ngram ngram = ngrams.get(i);
				System.out.println("\t\t{\n\t\t\tstring: \""+ngram.term+"\",\n\t\t\tlength: "+ngram.length+",\n\t\t\tpositions: [");
				for (int j=0, jlen= ngram.positions.size(); j<jlen; j++) {
					int positions[] = ngram.positions.get(j);
					System.out.println("\t\t\t\t["+positions[0]+","+positions[1]+"]"+(j+1==jlen ? "" : ","));
				}
				System.out.println("\t\t\t]\n\t\t}"+(i+1==ilen ? "" : ","));
			}
			System.out.println("\t]\n}");
			*/
			
			this.ngrams.addAll(ngrams);
		}
		
		
	}
	
	private List<Ngram> getFilteredNgrams(List<Ngram> ngrams, int lastToken) {
		// sort by length
		Collections.sort(ngrams);
		
		List<Ngram> filteredNgrams = new ArrayList<Ngram>();
		boolean[] occupied = new boolean[lastToken];
		for (Ngram ngram : ngrams) {
			if (ngram.getLength()<minLength) {continue;}
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
	
	private List<Ngram> getNextNgrams(List<Ngram> ngrams, SimplifiedTermInfo[] sparseSimplifiedTermInfoArray, int corpusDocumentIndex, int length) {
		Map<String, List<int[]>> stringPositionsMap = new HashMap<String, List<int[]>>();
		for (Ngram ngram : ngrams) {
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
		List<Ngram> newngrams = getNgramsFromStringPositions(stringPositionsMap, corpusDocumentIndex, length);
		if (newngrams.isEmpty()==false) {
			newngrams.addAll(getNextNgrams(newngrams, sparseSimplifiedTermInfoArray, corpusDocumentIndex, length+1));
		}
		return newngrams;
	}
	

	private List<Ngram> getNgramsFromStringPositions(Map<String, List<int[]>> stringPositionsMap, int corpusDocumentIndex, int length) {
		List<Ngram> ngrams = new ArrayList<Ngram>();
		for (Map.Entry<String, List<int[]>> stringPositions : stringPositionsMap.entrySet()) {
			List<int[]> values = stringPositions.getValue();
			if (values.size()>=minRawFreq) {
				ngrams.add(new Ngram(corpusDocumentIndex, stringPositions.getKey(), values, length));
			}
		}
		return ngrams;
	}
	
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

	private SimplifiedTermInfo[] getSparseSimplifiedTermInfoArray(CorpusMapper corpusMapper, int luceneDoc, int lastTokenOffset) throws IOException {
		
		Keywords stopwords = this.getStopwords(corpusMapper.getCorpus());
		Terms terms = corpusMapper.getAtomicReader().getTermVector(luceneDoc, tokenType.name());
		TermsEnum termsEnum = terms.iterator(null);
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


	List<Ngram> getNgrams() {
		return ngrams;
	}
}

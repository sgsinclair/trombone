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
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.DocumentTermsQueue;
import org.voyanttools.trombone.util.FlexibleParameters;

import edu.stanford.nlp.util.StringUtils;

/**
 * @author sgs
 *
 */
public class DocumentNgrams extends AbstractTerms {
	
	private int minLength;

	public DocumentNgrams(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		minLength = parameters.getParameterIntValue("minLength", 1);
	}

	@Override
	protected void runQueries(Corpus corpus, String[] queries) throws IOException {
	}

	@Override
	protected void runAllTerms(Corpus corpus) throws IOException {
		int[] totalTokens = corpus.getLastTokenPositions(tokenType);
		AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getDirectoryReader());
		StoredToLuceneDocumentsMapper corpusMapper = getStoredToLuceneDocumentsMapper(corpus);
		DocIdSetIterator it = corpusMapper.getDocIdSetIterator();
		while (it.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
			int luceneDoc = it.docID();
			int corpusDocumentIndex = corpusMapper.getDocumentPositionFromLuceneDocumentIndex(luceneDoc);
			int lastToken = totalTokens[corpusDocumentIndex];	
			
			// build single grams as seed for ngrams
			SimplifiedTermInfo[] sparseSimplifiedTermInfoArray = getSparseSimplifiedTermInfoArray(corpus, atomicReader, luceneDoc, lastToken);
			
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
			
			// temporary thing
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File("/Users/sgs/Downloads/game.of.thrones.edited/game.of.thrones.edited-ngrams.xml")));
			
			String document = atomicReader.document(luceneDoc).get(tokenType.name());
			writer.write("<documentNgrams lastPosition='"+lastToken+"' count='"+ngrams.size()+"'>");
			StringBuilder positionsBuilder = new StringBuilder();
			for (int i=0, ilen=ngrams.size(); i<ilen; i++) {
				Ngram ngram = ngrams.get(i);
				positionsBuilder.setLength(0); 
				String original = "";
				for (int j=0, jlen= ngram.positions.size(); j<jlen; j++) {
					int[] positions = ngram.positions.get(j);
					positionsBuilder.append(positions[0]).append(",").append(positions[1]);
					if (j+1<jlen) positionsBuilder.append(";");;
					if (j==0) {
						original = document.substring(sparseSimplifiedTermInfoArray[positions[0]].startOffset, sparseSimplifiedTermInfoArray[positions[1]].endOffset).replaceAll("\\s+", " ");
					}
				}
				writer.write("\t<documentNgram docIndex='"+ngram.corpusDocumentIndex+"' length='"+ngram.length+"' positions='"+positionsBuilder+"'>"+StringEscapeUtils.escapeXml(original)+"</documentNgram>");
			}
			writer.write("</documentNgrams>");
			writer.close();
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
		}
		
	}
	
	private List<Ngram> getFilteredNgrams(List<Ngram> ngrams, int lastToken) {
		// sort by length
		Collections.sort(ngrams);
		
		List<Ngram> filteredNgrams = new ArrayList<Ngram>();
		boolean[] occupied = new boolean[lastToken];
		for (Ngram ngram : ngrams) {
			if (ngram.length<minLength) {continue;}
			boolean keep = true;
			for (int[] positions : ngram.positions) {
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
			for (int[] positions : ngram.positions) {
				for (int i=positions[1]+1; i<sparseSimplifiedTermInfoArray.length;i++) {
					if (sparseSimplifiedTermInfoArray[i]!=null) {
						if (sparseSimplifiedTermInfoArray[i].term.isEmpty()) {break;} // non repeating word
						String term = ngram.term+" "+sparseSimplifiedTermInfoArray[i].term;
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
			if (values.size()>1) {
				ngrams.add(new Ngram(corpusDocumentIndex, stringPositions.getKey(), values, length));
			}
		}
		return ngrams;
	}
	
	private class Ngram implements Comparable<Ngram> {
		private int corpusDocumentIndex;
		private String term;
		private int length;
		private List<int[]> positions;
		private Ngram(int corpusDocumentIndex, String term, List<int[]> positions, int length) {
			this.corpusDocumentIndex = corpusDocumentIndex;
			this.term = term;
			this.length = length;
			this.positions = positions;
		}
		public String toString() {
			return "("+corpusDocumentIndex+") "+term+": "+positions.size();
		}
		@Override
		public int compareTo(Ngram ngram) {
			if (length==ngram.length && positions.size()>0 && ngram.positions.size()>0) {
				// sort by first position if same length
				int a = positions.get(0)[0];
				int b = ngram.positions.get(0)[0];
				return a > b ? 1 : a < b ? -1 : 0;
			}
			return length > ngram.length ? -1 : length < ngram.length ? 1 : 0;
		}
	}
	
	private List<Gram> getFilteredGrams(List<Gram> grams, int lastToken) {
		
		// sort by length
		Collections.sort(grams, new Comparator<Gram>()  {
		    public int compare(Gram g1, Gram g2) {
		    	if (g1.length==g2.length) {
		    		return g1.start > g2.start ? 1 : g1.start < g2.start ? 1 : 0;
		    	}
		    	return g1.length > g2.length ? -1 : 1;
		    }
		});
		
		boolean[] occupied = new boolean[lastToken];
		for (int i=0; i<limit; i++) {
			System.out.println(grams.get(i));
		}
		return grams;
		
	}
	
	private List<Gram> getNextGrams(String[] termsArray, List<Gram> grams) {
		System.err.println(grams.get(0).length+" "+grams.size());
		List<Gram> newgrams = new ArrayList<Gram>();
		Map<String, List<Gram>> newgramcandidates = new HashMap<String, List<Gram>>();
		for (Gram gram : grams) {
			int lastOffset = gram.end;
			for (int i = lastOffset+1, len=termsArray.length; i<len; i++) {
				String term = termsArray[i];
				if (term!=null) { // keep looking if it's null
					if (term.isEmpty()) {break;} // bail if it's an empty string
					String string = gram.term+" "+term;
					Gram g = new Gram(gram.corpusDocumentIndex, string, gram.start, i, gram.length+1);
					if (newgramcandidates.containsKey(string)==false) {newgramcandidates.put(string, new ArrayList<Gram>());}
					newgramcandidates.get(string).add(g);
					break;
				}
			}
		}
		for (Map.Entry<String, List<Gram>> newgramcandidate : newgramcandidates.entrySet()) {
			List<Gram> list = newgramcandidate.getValue();
			if (list.size()>1) { // only add if there are multiple instances
				newgrams.addAll(list);
			}
		}
		if (newgrams.isEmpty()==false) {
			newgrams.addAll(getNextGrams(termsArray, newgrams));
		}
		return newgrams;
	}

	private SimplifiedTermInfo[] getSparseSimplifiedTermInfoArray(Corpus corpus, AtomicReader atomicReader, int luceneDoc, int lastTokenOffset) throws IOException {
		
		Keywords stopwords = this.getStopwords(corpus);
		Terms terms = atomicReader.getTermVector(luceneDoc, tokenType.name());
		TermsEnum termsEnum = terms.iterator(null);
		SimplifiedTermInfo[] simplifiedTermInfoArray = new SimplifiedTermInfo[lastTokenOffset+1];
		while(true) {
			BytesRef term = termsEnum.next();
			if (term!=null) {
				String termString = term.utf8ToString();
				if (stopwords.isKeyword(termString)) {continue;} // treat as whitespace or punctuation
				DocsAndPositionsEnum docsAndPositionsEnum = termsEnum.docsAndPositions(null, null, DocsAndPositionsEnum.FLAG_OFFSETS);
				int freq = docsAndPositionsEnum.freq();
				for (int i=0, len = freq; i<len; i++) {
					int pos = docsAndPositionsEnum.nextPosition();
					new SimplifiedTermInfo(termString, pos, freq, docsAndPositionsEnum.startOffset(), docsAndPositionsEnum.endOffset());
					simplifiedTermInfoArray[pos] = freq>1 ? new SimplifiedTermInfo(termString, pos, freq, docsAndPositionsEnum.startOffset(), docsAndPositionsEnum.endOffset())  : new SimplifiedTermInfo(""); // empty string if not repeating
				}
			}
			else {break;}
		}
		return simplifiedTermInfoArray;
	}

	private class Gram {
		private int corpusDocumentIndex;
		private String term;
		private int start;
		private int end;
		private int length;
		private Gram(int corpusDocumentIndex, String term, int start, int end, int length) {
			this.corpusDocumentIndex = corpusDocumentIndex;
			this.term = term;
			this.start = start;
			this.end = end;
			this.length = length;
		}
		public String toString() {
			return "("+corpusDocumentIndex+") "+term+" "+start+"-"+end+ " ("+length+")";
		}
	}
	private class SimplifiedTermInfo {
		private String term;
		private int pos;
		private int freq;
		private int startOffset;
		private int endOffset;
		private SimplifiedTermInfo(String term, int pos, int freq, int startOffset, int endOffset) {
			this.term = term;
			this.pos = pos;
			this.freq = freq;
			this.startOffset = startOffset;
			this.endOffset = endOffset;
			
		}
		public SimplifiedTermInfo(String string) {
			this(string, 0, 0, 0, 0);
		}
	}
}

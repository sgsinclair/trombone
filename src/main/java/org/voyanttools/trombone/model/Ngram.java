/**
 * 
 */
package org.voyanttools.trombone.model;

import java.util.List;

/**
 * @author sgs
 *
 */
public class Ngram implements Comparable<Ngram> {
	private int docIndex;
	private String term;
	private int length;
	private List<int[]> positions;
	public Ngram(int corpusDocumentIndex, String term, List<int[]> positions, int length) {
		this.docIndex = corpusDocumentIndex;
		this.term = term;
		this.length = length;
		this.positions = positions;
	}
	public int getCorpusDocumentIndex() {
		return docIndex;
	}
	public String getTerm() {
		return term;
	}
	public int getLength() {
		return length;
	}
	public List<int[]> getPositions() {
		return positions;
	}
	public String toString() {
		return "("+docIndex+") "+term+": "+positions.size()+" ("+length+")";
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

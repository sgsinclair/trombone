/**
 * 
 */
package org.voyanttools.trombone.model;

/**
 * @author sgs
 *
 */
public class Gram {
	private int corpusDocumentIndex;
	private String term;
	private int start;
	private int end;
	private int length;
	public Gram(int corpusDocumentIndex, String term, int start, int end, int length) {
		this.corpusDocumentIndex = corpusDocumentIndex;
		this.term = term;
		this.start = start;
		this.end = end;
		this.length = length;
	}
	public String toString() {
		return "("+corpusDocumentIndex+") "+term+" "+getStart()+"-"+end+ " ("+getLength()+")";
	}
	public int getLength() {
		return length;
	}
	public int getStart() {
		return start;
	}
	public int getEnd() {
		return end;
	}
	public String getTerm() {
		return term;
	}
	public int getCorpusDocumentIndex() {
		return corpusDocumentIndex;
	}
}

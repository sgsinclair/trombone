/**
 * 
 */
package org.voyanttools.trombone.model;

/**
 * @author sgs
 *
 */
public class DocumentToken implements Comparable<DocumentToken> {
	
	private String docId;
	
	private int docIndex;
	
	private String term;
	
	private TokenType tokenType;
	
	private int rawFreq;
	
	private int position;
	
	private int startOffset;
	
	private int endOffset;
	
	private String lemma;
	
	private String pos;
	
	/**
	 * 
	 */
	public DocumentToken(String docId, int docIndex, String term, TokenType tokenType, int position, int startOffset, int endOffset, int rawFreq) {
		this.docId = docId;
		this.docIndex = docIndex;
		this.term = term;
		this.tokenType = tokenType;
		this.rawFreq = rawFreq;
		this.position = position;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		lemma = null;
		pos = null;
	}

	@Override
	public int compareTo(DocumentToken dt) {
		return Integer.valueOf(position).compareTo(Integer.valueOf(dt.position));
	}

	public String getTerm() {
		return term;
	}
	
	public int getDocIndex() {
		return docIndex;
	}
	
	public TokenType getTokenType() {
		return tokenType;
	}
	
	public int getRawFreq() {
		return rawFreq;
	}
	
	public int getPosition() {
		return position;
	}
	
	public int getStartOffset() {
		return startOffset;
	}
	
	public int getEndOffset() {
		return startOffset;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(getTerm());
		if (lemma!=null || pos!=null) {
			sb.append(" (");
			if (lemma!=null) {sb.append(lemma);}
			if (pos!=null) {sb.append("/").append(pos);}
		}
		if (lemma!=null) {sb.append(" (").append(lemma).append(")");}
		sb.append(" ").append(position).append(":").append(startOffset).append("-").append(endOffset);
		return sb.toString();
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

	public String getLemma() {
		return lemma;
	}
	public String getPos() {
		return pos;
	}
}

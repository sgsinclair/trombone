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
	}

	@Override
	public int compareTo(DocumentToken dt) {
		return Integer.valueOf(position).compareTo(Integer.valueOf(dt.position));
	}

	public String getTerm() {
		return term;
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

}

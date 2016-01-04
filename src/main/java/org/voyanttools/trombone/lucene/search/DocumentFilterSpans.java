/**
 * 
 */
package org.voyanttools.trombone.lucene.search;

import java.io.IOException;

import org.apache.lucene.search.spans.SpanCollector;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.BitSet;

/**
 * @author sgs
 *
 */
public class DocumentFilterSpans extends Spans {

	private Spans in;
	private BitSet bitSet;

	/**
	 * @param in
	 * @throws IOException 
	 */
	public DocumentFilterSpans(Spans in, BitSet bitSet) throws IOException {
		this.in = in;
		this.bitSet = bitSet;
	}

	@Override
	public int nextStartPosition() throws IOException {
		return in.nextStartPosition();
	}

	@Override
	public int startPosition() {
		return in.startPosition();
	}

	@Override
	public int endPosition() {
		return in.endPosition();
	}

	@Override
	public int width() {
		return in.width();
	}

	@Override
	public void collect(SpanCollector collector) throws IOException {
		in.collect(collector);
	}

	@Override
	public int docID() {
		return in.docID();
	}

	@Override
	public int advance(int target) throws IOException {
		return in.advance(target);
	}

	@Override
	public long cost() {
		return in.cost();
	}
	
    @Override
    public int nextDoc() throws IOException {
    	if (in==null) return NO_MORE_DOCS;
    	int i = in.docID();
    	int j = i+1;
    	int k = bitSet.nextSetBit(j);
    	int l = advance(k);
    	return l < bitSet.length() && bitSet.get(l) ? l : NO_MORE_DOCS;
    }
	

}

package org.voyanttools.trombone.lucene.search;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.spans.SpanCollector;
import org.apache.lucene.search.spans.Spans;

public class DocumentFilterSpansWrapper extends Spans {

	private DocumentFilterSpans[] spans;
	private int dfs; // track the current DocumentFilterSpan
	
	public DocumentFilterSpansWrapper(DocumentFilterSpans[] spans) {
		this.spans = spans;
		this.dfs = -1;
	}
	
	@Override
	public int nextDoc() throws IOException {
		int nextDFS = this.nextDFS();
		if (nextDFS == DocIdSetIterator.NO_MORE_DOCS) {
			return nextDFS;
		}
		
		DocumentFilterSpans dfs = spans[nextDFS];
		
		int result = dfs.nextDoc();
		
		if (result == DocIdSetIterator.NO_MORE_DOCS || result == Spans.NO_MORE_POSITIONS) {
			return nextDoc();
		}
		
		return result;
	}
	
	@Override
	public int advance(int target) throws IOException {
		int i = nextDoc();
		while(i < target && i != DocIdSetIterator.NO_MORE_DOCS) {
			i = nextDoc();
		}
		return i;
	}
	
	@Override
	public int nextStartPosition() throws IOException {
		return spans[dfs].nextStartPosition();
	}

	@Override
	public int startPosition() {
		return spans[dfs].startPosition();
	}

	@Override
	public int endPosition() {
		return spans[dfs].endPosition();
	}

	@Override
	public int width() {
		return spans[dfs].width();
	}

	@Override
	public void collect(SpanCollector collector) throws IOException {
		spans[dfs].collect(collector);
	}

	@Override
	public float positionsCost() {
		return spans[dfs].positionsCost();
	}

	@Override
	public int docID() {
		return spans[dfs].docID();
	}

	@Override
	public long cost() {
		return spans[dfs].cost();
	}

	private int nextDFS() {
		dfs++;
		if (dfs >= spans.length) {
			return DocIdSetIterator.NO_MORE_DOCS;
		} else {
			return dfs;
		}
	}
}

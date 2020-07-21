package org.voyanttools.trombone.lucene.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.spans.FilterSpans;
import org.apache.lucene.search.spans.SpanCollector;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.BitSet;

/**
 * A wrapper for a list of Spans, that is treatable as a singular Spans.
 * @author Andrew
 *
 */
public class SpansList extends Spans {

	protected Spans in = null;

	private int currBit = 0;
	private int currIndex = -1;
	private final List<Spans> spans;

	private final Map<Integer, Integer> spansIndexToBitsMap;

	public SpansList() {
		spans = new ArrayList<Spans>();
		spansIndexToBitsMap = new HashMap<Integer, Integer>();
	}

	public void addSpans(int luceneId, Spans span) {
		int index = spans.size();
		spans.add(span);

		spansIndexToBitsMap.put(index, luceneId);
	}

	public boolean isEmpty() {
		return spans.isEmpty();
	}

	@Override
	public final int nextDoc() throws IOException {

		if (isEmpty())
			return DocIdSetIterator.NO_MORE_DOCS;

		in = nextSpans();
		if (in == null) {
			return DocIdSetIterator.NO_MORE_DOCS;
		}

		in.nextDoc();

		return currBit;
	}

	@Override
	public final int advance(int target) throws IOException {
		int i = nextDoc();
		while (i < target && i != Spans.NO_MORE_DOCS) {
			i = nextDoc();
		}
		return i;
	}

	@Override
	public final int docID() {
		return in.docID();
	}

	private Spans nextSpans() {
		currIndex++;
		if (currIndex >= spans.size()) {
			return null;
		}

		currBit = spansIndexToBitsMap.get(currIndex);

		return spans.get(currIndex);
	}

	@Override
	public final int nextStartPosition() throws IOException {
		return in.nextStartPosition();
	}

	@Override
	public final int startPosition() {
		return in.startPosition();
	}

	@Override
	public final int endPosition() {
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
	public final long cost() {
		return in.cost();
	}

	@Override
	public String toString() {
		return "CorpusFiltered(" + in.toString() + ")";
	}

	@Override
	public float positionsCost() {
		return in.positionsCost();
	}

}

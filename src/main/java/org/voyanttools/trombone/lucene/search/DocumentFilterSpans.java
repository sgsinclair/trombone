package org.voyanttools.trombone.lucene.search;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.spans.FilterSpans;
import org.apache.lucene.search.spans.SpanCollector;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.BitSet;


/**
 * A {@link Spans} implementation that filters with a {@link BitSet},
 * adapted from {@link FilterSpans}.
 */
public class DocumentFilterSpans extends Spans {
 
  /** The wrapped spans instance. */
  protected final Spans in;
  
  private BitSet bitSet;
  
  private int nextStartPosition = -1;
  
  private boolean atFirstStartPosition = true;
  
  /** Wrap the given {@link Spans}. */
  public DocumentFilterSpans(Spans in, BitSet bitSet) {
    this.in = in;
    this.bitSet = bitSet;
  }
    
  @Override
  public final int nextDoc() throws IOException {
	  
	  if (in==null) return DocumentFilterSpans.NO_MORE_DOCS;
	  
	  // this next section is probably like a two-phase iterator, but I find this easier to wrap head around
	  
	  // find the next doc in the spans
	  int nextDoc = in.nextDoc();
	  
	  // bail if no more docs
	  if (nextDoc==DocIdSetIterator.NO_MORE_DOCS) {return nextDoc;}
	  
	  // jump to the next valid doc
	  nextDoc = bitSet.nextSetBit(nextDoc);
	  
	  // bail if no more docs
	  if (nextDoc==DocIdSetIterator.NO_MORE_DOCS) {return nextDoc;}
	  
	  // recurse if the current doc is beyond the spans
	  if (nextDoc!=in.docID()) {
		  
		  // skip the inner span to next valid beyond this doc (so we don't read all spans)
		  nextDoc = in.advance(nextDoc);
		  
		  // bail if we have no more hits
		  if (nextDoc==DocIdSetIterator.NO_MORE_DOCS) {return nextDoc;}
		  
		  // start over if this document isn't valid
		  else if (!bitSet.get(nextDoc)) {return nextDoc();}

		  // we've advanced to a valid doc
	  }
	  
	  // check for first position (which means matching inner Spans) and cache first position
	  nextStartPosition = in.nextStartPosition();
	  if (nextStartPosition==Spans.NO_MORE_POSITIONS) {
		  // no matches in this document, search next in bitSet
		  return nextDoc();
	  } else {
		  atFirstStartPosition = true;
	  }
	  return nextDoc;
  }

  @Override
  public final int advance(int target) throws IOException {
	  int i = nextDoc();
	  while(i<target && i!=Spans.NO_MORE_DOCS) {
		  i = nextDoc();
	  }
	  return i;
  }

  @Override
  public final int docID() {
    return in.docID();
  }

  @Override
  public final int nextStartPosition() throws IOException {
	  nextStartPosition = atFirstStartPosition ? nextStartPosition : in.nextStartPosition();
	  atFirstStartPosition = false;
	  return nextStartPosition;
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

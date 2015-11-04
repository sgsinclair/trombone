/**
 * 
 */
package org.voyanttools.trombone.lucene.search;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Scorer.ChildScorer;
import org.apache.lucene.search.SimpleCollector;

/**
 * @author sgs
 *
 */
public class LuceneDocIdsCollector extends SimpleCollector {

	private Map<Integer, Integer> luceneDocIds = new HashMap<Integer,Integer>();
	private int base = 0;
	private Scorer scorer = null;
	private int rawFreq = 0;

	public void collect(int doc) throws IOException {
		int absoluteDoc = base+doc;
		if (isSeen(absoluteDoc)==false) {
			scorer.score();
			int freq = 0;
			// Scorer.freq() doesn't always return term frequency, contrary to expectations
			// this makes me a bit nervous, is there always just one level of children?
			for (ChildScorer childSorer : scorer.getChildren()) {
				freq += childSorer.child.freq();
			}
			rawFreq+=freq;
			luceneDocIds.put(absoluteDoc, freq);
		}
	}
	
	public int getRawFreq() {
		return rawFreq;
	}
	
	public int getInDocumentsCount() {
		return luceneDocIds.size();
	}
	
	protected boolean isSeen(int doc) {
		return luceneDocIds.containsKey(doc);
	}
	
	@Override
	public void doSetNextReader(LeafReaderContext context) {
		base = context.docBase;
	}
	
	@Override
	public void setScorer(Scorer scorer) {
		this.scorer = scorer;
	}

	public Set<Integer> getLuceneDocIds() {
		return luceneDocIds.keySet();
	}

	@Override
	public boolean needsScores() {
		return true; // can this be set to false while ensuring that setScorer is called?
	}
}

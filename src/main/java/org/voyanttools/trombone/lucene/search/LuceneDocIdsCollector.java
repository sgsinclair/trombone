/**
 * 
 */
package org.voyanttools.trombone.lucene.search;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Scorer.ChildScorer;

/**
 * @author sgs
 *
 */
public class LuceneDocIdsCollector extends Collector {

	private Map<Integer, Integer> luceneDocIds = new HashMap<Integer,Integer>();
	private int base = 0;
	private Scorer scorer = null;
	private int rawFreq = 0;

	public void collect(int doc) throws IOException {
		int absoluteDoc = base+doc;
		if (isSeen(absoluteDoc)==false) {
			scorer.score();
			int freq = scorer.freq();
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
	public void setNextReader(AtomicReaderContext context) {
		base = context.docBase;
	}
	
	@Override
	public void setScorer(Scorer scorer) {
		this.scorer = scorer;
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return true;
	}
	
	public Set<Integer> getLuceneDocIds() {
		return luceneDocIds.keySet();
	}
}

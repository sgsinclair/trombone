/**
 * 
 */
package org.voyanttools.trombone.lucene.search;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Scorable;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.util.BitSet;
import org.voyanttools.trombone.lucene.CorpusMapper;

/**
 * @author sgs
 *
 */
public class LuceneDocIdsCollector extends SimpleCollector {

	private Map<Integer, Integer> luceneDocIds = new HashMap<Integer,Integer>();
	private int base = 0;
	private Scorable scorer = null;
	private int rawFreq = 0;
	private BitSet bitSet;

	public LuceneDocIdsCollector(CorpusMapper corpusMapper) throws IOException  {
		bitSet = corpusMapper.getBitSet();
	}
	
	public void collect(int doc) throws IOException {
		int absoluteDoc = base+doc;
		// FIXME: determine if we're slowly iterating over all documents in the index and if we can use another doc id iterator
		if (bitSet.get(doc) && isSeen(absoluteDoc)==false) {
			scorer.score();
			int freq = (int) scorer.score();
			rawFreq += freq;
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
	public void setScorer(Scorable scorer) {
		this.scorer = scorer;
	}

	public Set<Integer> getLuceneDocIds() {
		return luceneDocIds.keySet();
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE;
	}
	
}

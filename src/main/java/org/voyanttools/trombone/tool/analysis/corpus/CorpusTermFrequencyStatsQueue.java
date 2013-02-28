package org.voyanttools.trombone.tool.analysis.corpus;

import org.apache.lucene.util.PriorityQueue;

public class CorpusTermFrequencyStatsQueue extends PriorityQueue<CorpusTermFrequencyStats> {
	
	CorpusTermFrequencyStatsSort sort;

	public CorpusTermFrequencyStatsQueue(int size, CorpusTermFrequencyStatsSort sort) {
		super(size);
		this.sort = sort;
	}

	@Override
	protected boolean lessThan(CorpusTermFrequencyStats a,
			CorpusTermFrequencyStats b) {
		int ai, bi;
		float af, bf;
		String ab, bb;
		switch(sort) {
			case rawFrequencyAsc:
				ai = a.getRawFrequency();
				bi = b.getRawFrequency();
				if (ai==bi) {
					ab = a.getNormalizedTerm();
					bb = b.getNormalizedTerm();
					return ab.compareTo(bb) > 0;
				}
				else {return ai>bi;}
			case termAsc:
				ab = a.getNormalizedTerm();
				bb = b.getNormalizedTerm();
				if (ab.equals(bb)) {
					ai = a.getRawFrequency();
					bi = b.getRawFrequency();
					return ai<bi;
				}
				else {
					return ab.compareTo(bb) > 0;
				}
			case termDesc:
				ab = a.getNormalizedTerm();
				bb = b.getNormalizedTerm();
				if (ab.equals(bb)) {
					ai = a.getRawFrequency();
					bi = b.getRawFrequency();
					return ai<bi;
				}
				else {
					return ab.compareTo(bb) < 0;
				}
			default: // rawFrequencyDesc
				ai = a.getRawFrequency();
				bi = b.getRawFrequency();
				if (ai==bi) {
					ab = a.getNormalizedTerm();
					bb = b.getNormalizedTerm();
					return ab.compareTo(bb) > 0;
				}
				else {return ai<bi;}
		}
	}

}

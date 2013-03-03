package org.voyanttools.trombone.tool.analysis.corpus;

import static org.junit.Assert.*;

import org.junit.Test;

public class CorpusTermFrequencyStatsTest {

	@Test
	public void test() {
		CorpusTermFrequencyStats d1 = new CorpusTermFrequencyStats("a", 1, null);
		CorpusTermFrequencyStats d2 = new CorpusTermFrequencyStats("z", 2, null);
		CorpusTermFrequencyStats d3 = new CorpusTermFrequencyStats("é", 3, null);
		CorpusTermFrequencyStats d4 = new CorpusTermFrequencyStats("a", 3, null);
		CorpusTermFrequencyStatsQueue queue;

		// descending raw frequency, then ascending ascending alphabet
		queue = new CorpusTermFrequencyStatsQueue(2, CorpusTermFrequencyStatsSort.rawFrequencyDesc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(queue.size(), 2);
		assertEquals(3, (int) queue.poll().getRawFrequency());
		assertEquals("a", queue.poll().getTerm());

		// descending raw frequency, then ascending ascending alphabet
		queue = new CorpusTermFrequencyStatsQueue(CorpusTermFrequencyStatsSort.rawFrequencyDesc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(queue.size(), 4);
		assertEquals(1, (int) queue.poll().getRawFrequency());
		assertEquals("z", queue.poll().getTerm());

		// descending raw frequency, then ascending ascending alphabet
		queue = new CorpusTermFrequencyStatsQueue(2, CorpusTermFrequencyStatsSort.rawFrequencyAsc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(2, (int) queue.poll().getRawFrequency());
		assertEquals("a", queue.poll().getTerm());

		// descending raw frequency, then ascending ascending alphabet
		queue = new CorpusTermFrequencyStatsQueue(CorpusTermFrequencyStatsSort.rawFrequencyAsc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(3, (int) queue.poll().getRawFrequency());
		assertEquals("a", queue.poll().getTerm());

		// ascending term alphabet, then descending term frequency
		queue = new CorpusTermFrequencyStatsQueue(2, CorpusTermFrequencyStatsSort.termAsc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(1, queue.poll().getRawFrequency());
		assertEquals("a", queue.poll().getTerm());

		// ascending term alphabet, then descending term frequency
		queue = new CorpusTermFrequencyStatsQueue(CorpusTermFrequencyStatsSort.termAsc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(2, queue.poll().getRawFrequency());
		assertEquals("é", queue.poll().getTerm());
		
		// descending term alphabet, then descending term frequency
		queue = new CorpusTermFrequencyStatsQueue(2, CorpusTermFrequencyStatsSort.termDesc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(3, queue.poll().getRawFrequency());
		assertEquals("z", queue.poll().getTerm());
		
		// descending term alphabet, then descending term frequency
		queue = new CorpusTermFrequencyStatsQueue(CorpusTermFrequencyStatsSort.termDesc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(1, queue.poll().getRawFrequency());
		assertEquals("a", queue.poll().getTerm());
		
	}

}

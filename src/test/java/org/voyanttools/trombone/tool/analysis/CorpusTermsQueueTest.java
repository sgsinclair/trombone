package org.voyanttools.trombone.tool.analysis;

import static org.junit.Assert.*;

import org.junit.Test;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.tool.analysis.CorpusTermsQueue;

public class CorpusTermsQueueTest {

	@Test
	public void test() {
		CorpusTerm d1 = new CorpusTerm("a", 1, null);
		CorpusTerm d2 = new CorpusTerm("z", 2, null);
		CorpusTerm d3 = new CorpusTerm("é", 3, null);
		CorpusTerm d4 = new CorpusTerm("a", 3, null);
		CorpusTermsQueue queue;

		// descending raw frequency, then ascending ascending alphabet
		queue = new CorpusTermsQueue(2, CorpusTerm.Sort.rawFrequencyDesc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(queue.size(), 2);
		assertEquals(3, (int) queue.poll().getRawFrequency());
		assertEquals("a", queue.poll().getTerm());

		// descending raw frequency, then ascending ascending alphabet
		queue = new CorpusTermsQueue(CorpusTerm.Sort.rawFrequencyDesc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(queue.size(), 4);
		assertEquals(1, (int) queue.poll().getRawFrequency());
		assertEquals("z", queue.poll().getTerm());

		// descending raw frequency, then ascending ascending alphabet
		queue = new CorpusTermsQueue(2, CorpusTerm.Sort.rawFrequencyAsc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(2, (int) queue.poll().getRawFrequency());
		assertEquals("a", queue.poll().getTerm());

		// descending raw frequency, then ascending ascending alphabet
		queue = new CorpusTermsQueue(CorpusTerm.Sort.rawFrequencyAsc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(3, (int) queue.poll().getRawFrequency());
		assertEquals("a", queue.poll().getTerm());

		// ascending term alphabet, then descending term frequency
		queue = new CorpusTermsQueue(2, CorpusTerm.Sort.termAsc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(1, queue.poll().getRawFrequency());
		assertEquals("a", queue.poll().getTerm());

		// ascending term alphabet, then descending term frequency
		queue = new CorpusTermsQueue(CorpusTerm.Sort.termAsc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(2, queue.poll().getRawFrequency());
		assertEquals("é", queue.poll().getTerm());
		
		// descending term alphabet, then descending term frequency
		queue = new CorpusTermsQueue(2, CorpusTerm.Sort.termDesc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(3, queue.poll().getRawFrequency());
		assertEquals("z", queue.poll().getTerm());
		
		// descending term alphabet, then descending term frequency
		queue = new CorpusTermsQueue(CorpusTerm.Sort.termDesc);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(1, queue.poll().getRawFrequency());
		assertEquals("a", queue.poll().getTerm());
		
	}

}

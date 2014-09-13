package org.voyanttools.trombone.tool.analysis;

import static org.junit.Assert.*;

import org.junit.Test;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.tool.analysis.CorpusTermsQueue;

public class CorpusTermsQueueTest {

	@Test
	public void test() {
		CorpusTerm d1 = new CorpusTerm("a", 1, 1, null, null);
		CorpusTerm d2 = new CorpusTerm("z", 2, 1, null, null);
		CorpusTerm d3 = new CorpusTerm("é", 3, 1, null, null);
		CorpusTerm d4 = new CorpusTerm("a", 3, 1, null, null);
		CorpusTermsQueue queue;

		// descending raw frequency, then ascending ascending alphabet
		queue = new CorpusTermsQueue(2, CorpusTerm.Sort.RAWFREQDESC);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(queue.size(), 2);
		assertEquals(3, (int) queue.poll().getRawFreq());
		assertEquals("a", queue.poll().getTerm());

		// descending raw frequency, then ascending ascending alphabet
		queue = new CorpusTermsQueue(CorpusTerm.Sort.RAWFREQDESC);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(queue.size(), 4);
		assertEquals(1, (int) queue.poll().getRawFreq());
		assertEquals("z", queue.poll().getTerm());

		// descending raw frequency, then ascending ascending alphabet
		queue = new CorpusTermsQueue(2, CorpusTerm.Sort.RAWFREQASC);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(2, (int) queue.poll().getRawFreq());
		assertEquals("a", queue.poll().getTerm());

		// descending raw frequency, then ascending ascending alphabet
		queue = new CorpusTermsQueue(CorpusTerm.Sort.RAWFREQASC);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(3, (int) queue.poll().getRawFreq());
		assertEquals("a", queue.poll().getTerm());

		// ascending term alphabet, then descending term frequency
		queue = new CorpusTermsQueue(2, CorpusTerm.Sort.TERMASC);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(1, queue.poll().getRawFreq());
		assertEquals("a", queue.poll().getTerm());

		// ascending term alphabet, then descending term frequency
		queue = new CorpusTermsQueue(CorpusTerm.Sort.TERMASC);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(2, queue.poll().getRawFreq());
		assertEquals("é", queue.poll().getTerm());
		
		// descending term alphabet, then descending term frequency
		queue = new CorpusTermsQueue(2, CorpusTerm.Sort.TERMDESC);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(3, queue.poll().getRawFreq());
		assertEquals("z", queue.poll().getTerm());
		
		// descending term alphabet, then descending term frequency
		queue = new CorpusTermsQueue(CorpusTerm.Sort.TERMDESC);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(1, queue.poll().getRawFreq());
		assertEquals("a", queue.poll().getTerm());
		
	}

}

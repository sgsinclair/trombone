package org.voyanttools.trombone.model;

import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.util.FlexibleQueue;

public class CorpusTermsQueueTest {

	@Test
	public void test() {
		CorpusTerm d1 = new CorpusTerm("a", 1, 1, 1, 1);
		CorpusTerm d2 = new CorpusTerm("z", 2, 1, 1, 1);
		CorpusTerm d3 = new CorpusTerm("é", 3, 1, 1, 1);
		CorpusTerm d4 = new CorpusTerm("a", 3, 1, 1, 1);
		FlexibleQueue<CorpusTerm> queue;
		List<CorpusTerm> list;

		/*
		// descending raw frequency, then ascending ascending alphabet
		queue = new FlexibleQueue<CorpusTerm>(CorpusTerm.getComparator(CorpusTerm.Sort.RAWFREQDESC), 2);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		list = queue.getOrderedList();
		assertEquals(list.size(), 2);
		assertEquals(3, list.get(list.size()-1).getRawFreq());
		assertEquals("a", list.get(list.size()-2).getTerm());

		// descending raw frequency, then ascending ascending alphabet
		queue = new FlexibleQueue<CorpusTerm>(CorpusTerm.getComparator(CorpusTerm.Sort.RAWFREQDESC));
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(queue.size(), 4);
		assertEquals(1, (int) queue.poll().getRawFreq());
		assertEquals("z", queue.poll().getTerm());

		// descending raw frequency, then ascending ascending alphabet
		queue = new FlexibleQueue<CorpusTerm>(CorpusTerm.getComparator(CorpusTerm.Sort.RAWFREQASC), 2);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(2, (int) queue.poll().getRawFreq());
		assertEquals("a", queue.poll().getTerm());

		// descending raw frequency, then ascending ascending alphabet
		queue = new FlexibleQueue<CorpusTerm>(CorpusTerm.getComparator(CorpusTerm.Sort.RAWFREQASC));
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(3, (int) queue.poll().getRawFreq());
		assertEquals("a", queue.poll().getTerm());

		// ascending term alphabet, then descending term frequency
		queue = new FlexibleQueue<CorpusTerm>(CorpusTerm.getComparator(CorpusTerm.Sort.TERMASC), 2);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(1, queue.poll().getRawFreq());
		assertEquals("a", queue.poll().getTerm());

		// ascending term alphabet, then descending term frequency
		queue = new FlexibleQueue<CorpusTerm>(CorpusTerm.getComparator(CorpusTerm.Sort.TERMASC));
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(2, queue.poll().getRawFreq());
		assertEquals("é", queue.poll().getTerm());
		
		// descending term alphabet, then descending term frequency
		queue = new FlexibleQueue<CorpusTerm>(CorpusTerm.getComparator(CorpusTerm.Sort.TERMDESC), 2);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(3, queue.poll().getRawFreq());
		assertEquals("z", queue.poll().getTerm());
		
		// descending term alphabet, then descending term frequency
		queue = new FlexibleQueue<CorpusTerm>(CorpusTerm.getComparator(CorpusTerm.Sort.TERMDESC));
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		assertEquals(1, queue.poll().getRawFreq());
		assertEquals("a", queue.poll().getTerm());
*/		
	}

}

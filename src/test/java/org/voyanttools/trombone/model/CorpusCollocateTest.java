/**
 * 
 */
package org.voyanttools.trombone.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * @author sgs
 *
 */
public class CorpusCollocateTest {

	@Test
	public void test() {
		
		List<CorpusCollocate> list = new ArrayList<CorpusCollocate>();
		list.add(new CorpusCollocate("a",10,"b",10));
		list.add(new CorpusCollocate("b",10,"b",10));
		list.add(new CorpusCollocate("c",10,"b",10));
		list.add(new CorpusCollocate("a",9,"b",10));
		list.add(new CorpusCollocate("b",10,"b",10));
		list.add(new CorpusCollocate("c",11,"b",10));
		list.add(new CorpusCollocate("a",9,"a",10));
		list.add(new CorpusCollocate("b",10,"b",10));
		list.add(new CorpusCollocate("c",11,"c",10));
		list.add(new CorpusCollocate("a",9,"b",9));
		list.add(new CorpusCollocate("b",10,"b",10));
		list.add(new CorpusCollocate("c",11,"b",11));
		list.add(new CorpusCollocate("c",11,"a",11));
		list.add(new CorpusCollocate("c",11,"c",11));
		list.add(new CorpusCollocate("c",11,"c",12));
		list.add(new CorpusCollocate("c",11,"c",8));
		list.add(new CorpusCollocate("b",10,"d",8));
		
		// default comparator
		Collections.sort(list);
		assertEquals(8, list.get(0).getContextTermRawFrequency());
		assertEquals("c", list.get(1).getContextTerm());
		assertEquals(10, list.get(list.size()-1).getContextTermRawFrequency());

		// same as above (default comparator)
		Collections.sort(list, CorpusCollocate.getComparator(CorpusCollocate.Sort.RAWFREQDESC));
		assertEquals(10, list.get(0).getContextTermRawFrequency());
		assertEquals("b", list.get(1).getContextTerm());
		assertEquals(8, list.get(list.size()-1).getContextTermRawFrequency());
		
		// same as above (default comparator)
		Collections.sort(list, CorpusCollocate.getComparator(CorpusCollocate.Sort.RAWFREQASC));
		assertEquals(8, list.get(0).getContextTermRawFrequency());
		assertEquals(list.get(list.size()-2).getContextTerm(), "b");
		assertEquals(10, list.get(list.size()-1).getContextTermRawFrequency());
		
		Collections.sort(list, CorpusCollocate.getComparator(CorpusCollocate.Sort.TERMASC));
		assertEquals(list.get(0).getContextTerm(), "b");
		assertEquals(list.get(0).getContextTermRawFrequency(), 10);
		assertEquals(list.get(list.size()-1).getContextTerm(), "b");
		assertEquals(list.get(list.size()-2).getContextTerm(), "c");
		assertEquals(12, list.get(list.size()-2).getContextTermRawFrequency());
		
		Collections.sort(list, CorpusCollocate.getComparator(CorpusCollocate.Sort.TERMDESC));
		assertEquals(list.get(0).getContextTerm(), "c");
		assertEquals("c", list.get(1).getContextTerm());
		assertEquals(10, list.get(1).getContextTermRawFrequency());
		assertEquals("a", list.get(list.size()-1).getContextTerm());
		assertEquals(10, list.get(list.size()-1).getContextTermRawFrequency());
	}

}

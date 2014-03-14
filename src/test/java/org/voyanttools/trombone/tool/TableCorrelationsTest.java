package org.voyanttools.trombone.tool;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.voyanttools.trombone.model.Table;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

public class TableCorrelationsTest {

	@Test
	public void test() throws IOException {
		Storage storage = new MemoryStorage();
		FlexibleParameters parameters = new FlexibleParameters();
		
		// 0	1	2
		// 0	1	3
		parameters.setParameter("input", "0	1	2\n10	11	12\n20	21	24");
		
		// no columns – defaults to first two columns
		TableCorrelations tableCorrelations = new TableCorrelations(storage, parameters);
		tableCorrelations.run();
		//assertEquals((float) tableCorrelations.getCorrelation(), 0f, 0f);
		
		parameters.addParameter("columns", new int[]{1,2});
		tableCorrelations = new TableCorrelations(storage, parameters);
		tableCorrelations.run();
//		assertEquals((float) tableCorrelations.getCorrelation(), 0f, 0f);

	}

	@Test(expected=IllegalArgumentException.class)
	public void testOneColumnTableException() throws IOException {
		Storage storage = new MemoryStorage();
		FlexibleParameters parameters = new FlexibleParameters();
		parameters.setParameter("input", "0\n1");
		TableCorrelations tableCorrelations = new TableCorrelations(storage, parameters);
		tableCorrelations.run();
	}
}

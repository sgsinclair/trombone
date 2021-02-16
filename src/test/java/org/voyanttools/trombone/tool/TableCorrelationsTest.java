package org.voyanttools.trombone.tool;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class TableCorrelationsTest {

	@Test
	public void test() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test(storage);
		}
	}
	
	public void test(Storage storage) throws IOException {
		FlexibleParameters parameters = new FlexibleParameters();
		parameters.addParameter("input", "a\tb\n20\t12\n19\t11\n16\t8\n10\t4\n2\t-1");
		parameters.addParameter("inputFormat", "tsv");
		parameters.addParameter("columnHeaders", "true");
		parameters.addParameter("implementation", "pearson");
		
		TableCorrelations tc = new TableCorrelations(storage, parameters);
		tc.run();
		
		assertEquals(0.9957, tc.getCorrelation(), 0.0001);
		
		parameters = new FlexibleParameters();
		parameters.addParameter("input", "a\tb\n20\t12\n19\t11\n16\t8\n10\t4");
		parameters.addParameter("inputFormat", "tsv");
		parameters.addParameter("columnHeaders", "true");
		parameters.addParameter("implementation", "spearman");
		
		tc = new TableCorrelations(storage, parameters);
		tc.run();
		
		assertEquals(1, tc.getCorrelation(), 0.0001);
		
		parameters = new FlexibleParameters();
		parameters.addParameter("input", "a\tb\n20\t12\n19\t11\n16\t8\n10\t4");
		parameters.addParameter("inputFormat", "tsv");
		parameters.addParameter("columnHeaders", "true");
		parameters.addParameter("implementation", "kendall");
		
		tc = new TableCorrelations(storage, parameters);
		tc.run();
		
		assertEquals(1, tc.getCorrelation(), 0.0001);
	}

}

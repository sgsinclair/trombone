/**
 * 
 */
package org.voyanttools.trombone.tool;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class TableManagerTest {
	
	//@Test(expected=IllegalArgumentException.class)
	public void testNoTable () throws IOException {
		Storage storage = new MemoryStorage();
		FlexibleParameters parameters = new FlexibleParameters();
		TableManager tableManager = new TableManager(storage, parameters);
		tableManager.run();
	}

	@Test
	public void test() throws IOException {
		Storage storage = new FileStorage();
		FlexibleParameters parameters = new FlexibleParameters();		
		TableManager tableManager;
		
		parameters = new FlexibleParameters();
		parameters.setParameter("table", "1");
		parameters.setParameter("verify", "true");
		tableManager = new TableManager(storage, parameters);
		tableManager.run();
		String generatedid = tableManager.getTableId();
		assertTrue(generatedid.isEmpty());
		
		
		/*
		// test storing new resources without id
		parameters = new FlexibleParameters();
		String testString = "0	1\n2	3";
		parameters.addParameter("input", testString);
		tableManager = new TableManager(storage, parameters);
		tableManager.run();
		String generatedid = tableManager.getTableId();
		assertFalse(testString.equals(generatedid));
		
		// test retrieving resource
		parameters = new FlexibleParameters();
		parameters.addParameter("table", generatedid);
		tableManager = new TableManager(storage, parameters);
		tableManager.run();
		assertEquals(testString, tableManager.getTable().toTsv());

		
		// test storing new resources with id
		parameters = new FlexibleParameters();
		parameters.addParameter("input", testString);
		String specifiedId = DigestUtils.md5Hex(testString);
		parameters.addParameter("table", specifiedId);
		tableManager = new TableManager(storage, parameters);
		tableManager.run();
		assertEquals(specifiedId, tableManager.getTableId());
		
		// test retrieving resource
		parameters = new FlexibleParameters();
		parameters.addParameter("table", specifiedId);
		tableManager = new TableManager(storage, parameters);
		tableManager.run();
		assertEquals(testString, tableManager.getTable().toTsv());
		*/
	}

}

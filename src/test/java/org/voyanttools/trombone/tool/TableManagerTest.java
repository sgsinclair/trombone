/**
 * 
 */
package org.voyanttools.trombone.tool;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class TableManagerTest {

	@Test
	public void test() throws IOException {
		Storage storage = new MemoryStorage();
		FlexibleParameters parameters = new FlexibleParameters();
		
		// test verifying resource that doesn't exist
		parameters.setParameter("verifyTableId", "doesNotExist");
		TableManager tableManager = new TableManager(storage, parameters);
		tableManager.run();
		assertEquals(tableManager.getTableId(), ""); // should be blank if doesn't exist
		
		// test storing new resources without id
		parameters = new FlexibleParameters();
		String testString = "0	1\n2	3";
		parameters.addParameter("storeTable", testString);
		tableManager = new TableManager(storage, parameters);
		tableManager.run();
		String generatedid = tableManager.getTableId();
		assertFalse(testString.equals(generatedid));
		
		// test retrieving resource
		parameters = new FlexibleParameters();
		parameters.addParameter("retrieveTableId", generatedid);
		tableManager = new TableManager(storage, parameters);
		tableManager.run();
		assertEquals(testString, tableManager.getTable().toTsv());

		
		// test storing new resources with id
		parameters = new FlexibleParameters();
		parameters.addParameter("storeTable", testString);
		String specifiedId = DigestUtils.md5Hex(testString);
		parameters.addParameter("storeTableId", specifiedId);
		tableManager = new TableManager(storage, parameters);
		tableManager.run();
		assertEquals(specifiedId, tableManager.getTableId());
		
		// test retrieving resource
		parameters = new FlexibleParameters();
		parameters.addParameter("retrieveTableId", specifiedId);
		tableManager = new TableManager(storage, parameters);
		tableManager.run();
		assertEquals(testString, tableManager.getTable().toTsv());
	}

}

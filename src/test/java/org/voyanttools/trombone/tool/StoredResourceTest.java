package org.voyanttools.trombone.tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.resource.StoredResource;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class StoredResourceTest {
	
	@Test
	public void test() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test(storage);
		}
	}


	public void test(Storage storage) throws IOException {

		FlexibleParameters parameters = new FlexibleParameters();
		
		// test verifying resource that doesn't exist
		parameters.setParameter("verifyResourceId", "doesNotExist");
		StoredResource storedResource = new StoredResource(storage, parameters);
		storedResource.run();
		assertEquals(storedResource.getResourceId(), ""); // should be blank if doesn't exist
		
		// test storing new resources without id
		parameters = new FlexibleParameters();
		String test = "this is a test";
		parameters.addParameter("storeResource", test);
		storedResource = new StoredResource(storage, parameters);
		storedResource.run();
		String generatedid = storedResource.getResourceId();
		assertFalse(test.equals(generatedid));
		
		// test retrieving resource
		parameters = new FlexibleParameters();
		parameters.addParameter("retrieveResourceId", generatedid);
		storedResource = new StoredResource(storage, parameters);
		storedResource.run();
		assertEquals(test, storedResource.getResource());

		
		// test storing new resources with id
		parameters = new FlexibleParameters();
		parameters.addParameter("storeResource", test);
		String specifiedId = DigestUtils.md5Hex(test);
		parameters.addParameter("resourceId", specifiedId);
		storedResource = new StoredResource(storage, parameters);
		storedResource.run();
		assertEquals(specifiedId, storedResource.getResourceId());
		
		// test retrieving resource
		parameters = new FlexibleParameters();
		parameters.addParameter("retrieveResourceId", specifiedId);
		storedResource = new StoredResource(storage, parameters);
		storedResource.run();
		assertEquals(test, storedResource.getResource());
		
		storage.destroy();
		
	}

}

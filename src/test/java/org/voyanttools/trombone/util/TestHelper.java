/**
 * 
 */
package org.voyanttools.trombone.util;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.voyanttools.trombone.storage.file.FileStorage;

/**
 * @author sgs
 *
 */
public class TestHelper {
	
	public static String RESOURCES_PATH = TestHelper.class.getResource("../texts").getFile();
	public static final String DEFAULT_TROMBOME_DIRECTORY = FileStorage.DEFAULT_TROMBOME_DIRECTORY+"_test";
	
	public static File getTemporaryTestStorageDirectory() throws IOException {
		File file = new File(FileStorage.DEFAULT_TROMBOME_DIRECTORY+"_test_"+UUID.randomUUID());
		System.out.println("Temporary storage created: "+file.toString());
		return file;
	}
	
	public static File getResource(String relativeToTexts) {
		return new File(RESOURCES_PATH+"/"+relativeToTexts);
	}

}

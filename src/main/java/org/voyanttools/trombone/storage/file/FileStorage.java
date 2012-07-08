/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import java.io.File;

import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.storage.Storage;

/**
 * @author sgs
 *
 */
public class FileStorage implements Storage {
	
	/**
	 * the default file-system location for storage
	 */
	public static final String DEFAULT_TROMBOME_DIRECTORY = System.getProperty("java.io.tmpdir") + File.separator + "trombone4_0";
	
	
	/**
	 * the actual base directory used for storage
	 */
	File storageLocation;
	
	/**
	 * the handler for InputSource operations
	 */
	private FileStoredDocumentSourceStorage documentSourceStorage = null;

	public FileStorage() {
		this(new File(DEFAULT_TROMBOME_DIRECTORY));
	}

	public FileStorage(File storageLocation) {
		this.storageLocation = storageLocation;
		if (storageLocation.exists()==false) {
			storageLocation.mkdirs();
		}
	}

	public StoredDocumentSourceStorage getStoredDocumentSourceStorage() {
		if (documentSourceStorage==null) {
			documentSourceStorage = new FileStoredDocumentSourceStorage(this.storageLocation);
		}
		return documentSourceStorage;
	}


}

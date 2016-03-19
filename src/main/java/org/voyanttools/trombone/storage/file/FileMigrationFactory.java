/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import java.io.File;

import org.voyanttools.trombone.storage.Migrator;

/**
 * @author sgs
 *
 */
public class FileMigrationFactory {

	public static Migrator getMigrator(FileStorage storage, String id) {
		File base = storage.storageLocation.getParentFile();
		if (new File(base, FileTrombone4_0Migrator.DEFAULT_TROMBOME_DIRECTORY_NAME).exists()) {return new FileTrombone4_0Migrator(storage, id);}
		if (new File(base, FileTrombone3_0Migrator.DEFAULT_TROMBOME_DIRECTORY_NAME).exists()) {return new FileTrombone3_0Migrator(storage, id);}
		return null;
	}
}

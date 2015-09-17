/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import org.voyanttools.trombone.storage.Migrator;

/**
 * @author sgs
 *
 */
public class FileMigrationFactory {

	public static Migrator getMigrator(FileStorage storage, String id) {
		if (FileTrombone3_0Migrator.isMigratable(storage, id)) {return new FileTrombone3_0Migrator(storage, id);}
		return null;
	}
}

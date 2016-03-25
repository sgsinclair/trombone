/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.lang.reflect.Constructor;

/**
 * @author sgs
 *
 */
public class FileMigrationFactory {
	
	@SuppressWarnings("unchecked")
	private static Class<? extends AbstractFileMigrator>[] migrators = new Class[]{FileTrombone4_1Migrator.class, FileTrombone4_0Migrator.class, FileTrombone3_0Migrator.class};

	public static FileMigrator getMigrator(FileStorage storage, String id) {
		for (Class<? extends AbstractFileMigrator> migratorClass : migrators) {
			Constructor<?> constructor;
			FileMigrator migrator;
			try {
				constructor = migratorClass.getDeclaredConstructor(FileStorage.class, String.class);
				migrator = (FileMigrator) constructor.newInstance(storage, id);
			} catch (Exception e) {
				throw new RuntimeException("Unable to instantiate migrator: "+migratorClass.getName(), e);
			}
			if (migrator.corpusExists()) {
				return migrator;
			}
		}
		return null;
	}

	public static File getStoredObjectFile(FileStorage storage, String id) {
		for (Class<? extends AbstractFileMigrator> migratorClass : migrators) {
			Constructor<?> constructor;
			FileMigrator migrator;
			try {
				constructor = migratorClass.getDeclaredConstructor(FileStorage.class, String.class);
				migrator = (FileMigrator) constructor.newInstance(storage, id);
			} catch (Exception e) {
				throw new RuntimeException("Unable to instantiate migrator: "+migratorClass.getName(), e);
			}
			File file = migrator.getStoredObjectFile();
			if (file!=null) {return file;}
		}
		return null;
	}
}

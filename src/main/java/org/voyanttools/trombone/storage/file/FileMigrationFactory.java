/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.lang.reflect.Constructor;

import org.voyanttools.trombone.storage.Migrator;

/**
 * @author sgs
 *
 */
public class FileMigrationFactory {
	
	@SuppressWarnings("unchecked")
	private static Class<? extends AbstractFileMigrator>[] migrators = new Class[]{FileTrombone4_1Migrator.class, FileTrombone4_0Migrator.class, FileTrombone3_0Migrator.class};

	public static Migrator getMigrator(FileStorage storage, String id) {
		for (Class<? extends AbstractFileMigrator> migratorClass : migrators) {
			Constructor<?> constructor;
			Migrator migrator;
			try {
				constructor = migratorClass.getDeclaredConstructor(FileStorage.class, String.class);
				migrator = (Migrator) constructor.newInstance(storage, id);
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
			Migrator migrator;
			try {
				constructor = migratorClass.getDeclaredConstructor(FileStorage.class, String.class);
				migrator = (Migrator) constructor.newInstance(storage, id);
			} catch (Exception e) {
				throw new RuntimeException("Unable to instantiate migrator: "+migratorClass.getName(), e);
			}
			File file = migrator.getStoredObjectFile();
			if (file!=null) {return file;}
		}
		return null;
	}
}

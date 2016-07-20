/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.util.Arrays;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

/**
 * @author sgs
 *
 */
public class FileMigrationFactory {
	
	@SuppressWarnings("unchecked")
	private static Class<? extends AbstractFileMigrator>[] migrators = new Class[]{FileTrombone4_2Migrator.class, FileTrombone4_1Migrator.class, FileTrombone4_0Migrator.class, FileTrombone3_0Migrator.class};

	public static FileMigrator getMigrator(FileStorage storage, String id) {
		
		// first try recovered storage
		for (File file : getRecoveredStorageDirectories(storage)) {
			FileMigrator migrator = new FileTromboneCurrentMigrator(file.getName(), storage, id);
			if (migrator.corpusExists()) {return migrator;}
		}
		
		// next try migrators
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
		
		// first try recovered storage
		for (File file : getRecoveredStorageDirectories(storage)) {
			FileMigrator migrator = new FileTromboneCurrentMigrator(file.getName(), storage, id);
			File f = migrator.getStoredObjectFile();
			if (f!=null && f.exists()) {return f;}
		}
		
		// next try migrators
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
	
	private static File[] getRecoveredStorageDirectories(FileStorage storage) {
		final String storageFilename = storage.storageLocation.getName();
		File storageParentFile = storage.storageLocation.getParentFile();
		File[] files = storageParentFile.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(storageFilename) && storageFilename.equals(name)==false;
			}
			
		});
		Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
		return files;
	}
}

package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.io.IOException;

import org.voyanttools.trombone.storage.Migrator;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;

public abstract class AbstractFileMigrator implements Migrator {
	
	protected static String DEFAULT_TROMBOME_DIRECTORY_NAME = null;
	
	protected FileStorage storage;
	
	protected String id;
	
	protected AbstractFileMigrator(FileStorage storage, String id) {
		this.storage = storage;
		this.id = id;
	}

	protected String getFromStoredId(String storedId, FlexibleParameters parameters) throws IOException {
		parameters.setParameter("nextCorpusCreatorStep", "extract"); // I *think* we can skip expand at the document level
		parameters.setParameter("storedId", storedId);
		RealCorpusCreator realCorpusCreator = new RealCorpusCreator(storage, parameters);
		realCorpusCreator.run();
		return realCorpusCreator.getStoredId();
	}

	protected static boolean isMigratable(FileStorage storage, String id) {
		return getMigrationSourceCorpusDirectory(storage, id).exists();
	}
	
	protected static File getMigrationSourceCorpusDirectory(FileStorage storage, String id) {
		return new File(getMigrationSourceDirectory(storage), id);
	}
	
	protected static File getMigrationSourceDirectory(FileStorage storage) {
		return new File(storage.storageLocation.getParentFile(), DEFAULT_TROMBOME_DIRECTORY_NAME);
	}

}

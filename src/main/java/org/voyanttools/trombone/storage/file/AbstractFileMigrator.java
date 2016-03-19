package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.io.IOException;

import org.voyanttools.trombone.storage.Migrator;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;

public abstract class AbstractFileMigrator implements Migrator {
	
	protected FileStorage storage;
	
	protected String id;
	
	protected AbstractFileMigrator(FileStorage storage, String id) {
		this.storage = storage;
		this.id = id;
	}

	public String getMigratedCorpusId() throws IOException {
		
		String storedId = transferDocuments();
		
		FlexibleParameters parameters = getParameters();
		
		return getNewCorpusId(storedId, parameters);
		
	}
		
	protected String transferDocuments() throws IOException {
		
		String[] ids = getDocumentIds();
		
		return getStoredDocumentsId(ids);
		
	}
	
	protected String getNewCorpusId(String storedId, FlexibleParameters parameters) throws IOException {
		parameters.setParameter("nextCorpusCreatorStep", "extract"); // I *think* we can skip expand at the document level
		parameters.setParameter("storedId", storedId);
		RealCorpusCreator realCorpusCreator = new RealCorpusCreator(storage, parameters);
		realCorpusCreator.run();
		return realCorpusCreator.getStoredId();
	}
	
	protected File getMigrationSourceDirectory() {
		return new File(storage.storageLocation.getParentFile(), getSourceTromboneDirectoryName());
	}
	
	protected abstract String[] getDocumentIds() throws IOException;

	protected abstract File getSourceTromboneCorpusDirectory();
	
	protected abstract String getStoredDocumentsId(String[] ids) throws IOException;
	
	protected abstract String getSourceTromboneDirectoryName();
	
	protected abstract FlexibleParameters getParameters();
	
}

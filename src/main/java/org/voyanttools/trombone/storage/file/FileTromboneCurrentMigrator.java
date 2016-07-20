package org.voyanttools.trombone.storage.file;

public class FileTromboneCurrentMigrator extends FileTrombone4_2Migrator {

	private String tromboneDirectoryName;
	public FileTromboneCurrentMigrator(String tromboneDirectoryName, FileStorage storage, String id) {
		super(storage, id);
		this.tromboneDirectoryName = tromboneDirectoryName;
	}

	@Override
	protected String getSourceTromboneDirectoryName() {
		assert(FileTromboneCurrentMigrator.class.isInstance(this));
		return tromboneDirectoryName;
	}

}

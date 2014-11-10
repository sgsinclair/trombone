package org.voyanttools.trombone.model;

import org.mapdb.DB;
import org.voyanttools.trombone.storage.Storage;

public abstract class AbstractDB {
	
	protected DB db;
	protected Storage storage;

	public AbstractDB(Storage storage, String dbId, boolean readOnly) {
		this.storage = storage;
		db = storage.getDB("mapbd-"+dbId, readOnly);
	}
	
	public void commit() {
		db.commit();
	}
	public void close() {
		storage.closeDB(db);
	}

}

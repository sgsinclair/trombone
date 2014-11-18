package org.voyanttools.trombone.model;

import org.mapdb.DB;
import org.voyanttools.trombone.storage.Storage;

public abstract class AbstractDB {
	
	protected DB db;
	protected Storage storage;
	private final static String PREFIX = "mapdb-";

	public AbstractDB(Storage storage, String dbId, boolean readOnly) {
		this.storage = storage;
		setDB(dbId, readOnly);
	}
	
	protected void setDB(String dbId, boolean readOnly) {
		db = storage.getDB(getName(dbId), readOnly);
	}
	
	public void commit() {
		db.commit();
	}
	public void close() {
		storage.closeDB(db);
	}
	
	private static String getName(String dbId) {
		return PREFIX+dbId;
	}

	protected static boolean exists(Storage storage, String name) {
		return storage.existsDB(getName(name));
	}


}

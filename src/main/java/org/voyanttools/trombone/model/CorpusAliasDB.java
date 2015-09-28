/**
 * 
 */
package org.voyanttools.trombone.model;

import java.util.Map;

import org.voyanttools.trombone.storage.Storage;

/**
 * @author sgs
 *
 */
public class CorpusAliasDB extends AbstractDB {

	private Map<String, String> map;
	
	/**
	 * @param storage
	 * @param dbId
	 * @param readOnly
	 */
	public CorpusAliasDB(Storage storage, boolean readOnly) {
		super(storage, "corpus-alias", readOnly);
		map = db.getHashMap("corpus-alias-map");
	}
	
	public void put(String alias, String corpusId) {
		map.put(alias, corpusId);
	}

	public String get(String alias) {
		return map.get(alias);
	}
	
	public static boolean exists(Storage storage) {
		return AbstractDB.exists(storage, "corpus-alias");
	}
}

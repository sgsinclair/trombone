/**
 * 
 */
package org.voyanttools.trombone.storage.file;

/**
 * Trombone 4.2 is similar to 4.1. 
 * 
 * @author Stéfan Sinclair
 */
public class FileTrombone4_2Migrator extends FileTrombone4_1Migrator {

	/**
	 * @param storage
	 * @param id
	 */
	public FileTrombone4_2Migrator(FileStorage storage, String id) {
		super(storage, id);
	}
	
	@Override
	protected String getSourceTromboneDirectoryName() {
		assert(FileTrombone4_2Migrator.class.isInstance(this));
		return "trombone4_2";		
	}

}

/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.io.IOException;

import org.voyanttools.trombone.storage.Storage.Location;

/**
 * @author sgs
 *
 */
public interface FileMigrator {

	public String getMigratedCorpusId() throws IOException;
	
	public boolean corpusExists();

	public File getStoredObjectFile(Location location);
}

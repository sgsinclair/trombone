/**
 * 
 */
package org.voyanttools.trombone.storage;

import java.io.File;
import java.io.IOException;

/**
 * @author sgs
 *
 */
public interface Migrator {

	public String getMigratedCorpusId() throws IOException;
	
	public boolean corpusExists();

	public File getStoredObjectFile();
}

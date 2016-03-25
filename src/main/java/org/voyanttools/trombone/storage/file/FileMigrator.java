/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.io.IOException;

/**
 * @author sgs
 *
 */
public interface FileMigrator {

	public String getMigratedCorpusId() throws IOException;
	
	public boolean corpusExists();

	public File getStoredObjectFile();
}

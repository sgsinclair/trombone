/**
 * 
 */
package org.voyanttools.trombone.storage;

import java.io.IOException;

/**
 * @author sgs
 *
 */
public interface Migrator {

	public String getMigratedCorpusId() throws IOException;
	
	public boolean exists();
}

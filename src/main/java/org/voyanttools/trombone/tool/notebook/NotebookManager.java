/**
 * 
 */
package org.voyanttools.trombone.tool.notebook;

import java.io.IOException;

import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class NotebookManager extends AbstractTool {

	public NotebookManager(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() throws IOException {
		throw new RuntimeException("Unable to locate requested notebook.");
	}

}

/**
 * 
 */
package org.voyanttools.trombone.tool.notebook;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.Storage.Location;
import org.voyanttools.trombone.storage.file.FileMigrationFactory;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("notebook")
public class NotebookManager extends AbstractTool {
	
	String id = null; // notebook source (ID, URL, etc.)
	
	String data = null; // notebook data as JSON
	
	public NotebookManager(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}
	public float getVersion() {
		return super.getVersion()+1f;
	}

	@Override
	public void run() throws IOException {
		String action = parameters.getParameterValue("action", "");
		
		// SAVE NOTEBOOK
		if (action.equals("save")) {
			String data = parameters.getParameterValue("data");
			if (data.trim().isEmpty()) {
				throw new IOException("Notebook contains no data.");
			}
			if (parameters.getParameterValue("id","").trim().isEmpty()==false) {
				id = parameters.getParameterValue("id");
			} else {
				while(true) {
					id = RandomStringUtils.randomAlphanumeric(6);
					if (storage.isStored(id, Location.notebook)==false) {
						break;
					}
				}
			}
			storage.storeString(data, id+".html", Location.notebook, true);
		}
		
		// CHECK IF NOTEBOOK EXISTS
		else if (action.equals("exists")) {
			id = parameters.getParameterValue("id");
			data = storage.isStored(id+".html", Location.notebook) ? "true" : "false";
		}
		
		// LOAD NOTEBOOK
		else if (action.equals("load")) {
			id = parameters.getParameterValue("id");
			if (id==null && parameters.getParameterValue("data", "").trim().isEmpty()==false) {
				data = parameters.getParameterValue("data"); // has been set by server
			}
			else if (storage.isStored(id+".html", Location.notebook)) {
				data = storage.retrieveString(id+".html", Location.notebook);
			} else if (storage.isStored(id, Location.notebook)) { // this is for version 2.4
				data = storage.retrieveString(id, Location.notebook);
			} else if (storage instanceof FileStorage) {
				File file = FileMigrationFactory.getStoredObjectFile((FileStorage) storage, id+".html", Location.notebook);
				if (file==null || !file.exists()) { // this is for version 2.4 and lower
					file = FileMigrationFactory.getStoredObjectFile((FileStorage) storage, id, Location.notebook);					
				}
				if (file!=null && file.exists()) {
					((FileStorage) storage).copyResource(file, id+".html", Storage.Location.notebook);
					data = storage.retrieveString(id+".html", Location.notebook);
				}
			}
			if (data==null) {
				throw new IOException("Unable to retrieve notebook: "+id);
			}
		}

	}
}

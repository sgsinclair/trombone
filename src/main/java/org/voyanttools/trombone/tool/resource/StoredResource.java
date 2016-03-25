package org.voyanttools.trombone.tool.resource;

import java.io.File;
import java.io.IOException;

import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileMigrationFactory;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("storedResource")
public class StoredResource extends AbstractTool {
	
	private String id = "";
	
	private String resource = "";
	
	public StoredResource(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}


	@Override
	public void run() throws IOException {
		if (this.parameters.containsKey("verifyResourceId")) {
			String id = this.parameters.getParameterValue("verifyResourceId");
			if (this.storage.hasStoredString(id)) {
				this.id = id;
			}
		}
		else if (this.parameters.containsKey("storeResource")) {
			if (this.parameters.containsKey("resourceId")) {
				this.id = this.parameters.getParameterValue("resourceId");
				this.storage.storeString(this.parameters.getParameterValue("storeResource"), this.id);
			}
			else {
				this.id = this.storage.storeString(this.parameters.getParameterValue("storeResource"));
			}
		}
		else if (this.parameters.containsKey("retrieveResourceId")) {
			String id = this.parameters.getParameterValue("retrieveResourceId");
			// if it doesn't exist, try to retrieve from previous storage
			if (!storage.isStored(id) && storage instanceof FileStorage) {
				File file = FileMigrationFactory.getStoredObjectFile((FileStorage) storage, id);
				if (file!=null && file.exists()) {
					((FileStorage) storage).copyResource(file, id);
				}
			}
			this.resource = this.storage.retrieveString(id);
			this.id = id;
		}
		
	}


	public String getResourceId() {
		return id;
	}


	public String getResource() {
		return resource;
	}

}

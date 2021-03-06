package org.voyanttools.trombone.tool.resource;

import java.io.File;
import java.io.IOException;

import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.Storage.Location;
import org.voyanttools.trombone.storage.file.FileMigrationFactory;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("storedResource")
public class StoredResource extends AbstractTool {
	
	protected String id = "";
	
	protected String resource = "";
	
	public StoredResource(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}


	@Override
	public void run() throws IOException {
		if (this.parameters.containsKey("verifyResourceId")) {
			String id = this.parameters.getParameterValue("verifyResourceId");
			if (this.storage.hasStoredString(id, Storage.Location.object)) {
				this.id = id;
			} else if (storage instanceof FileStorage) {
				File file = FileMigrationFactory.getStoredObjectFile((FileStorage) storage, id, Location.object);
				if (file!=null && file.exists()) {
					((FileStorage) storage).copyResource(file, id, Storage.Location.object);
					this.id = id;
				}
			}
		}
		else if (this.parameters.containsKey("storeResource")) {
			if (this.parameters.containsKey("resourceId")) {
				this.id = this.parameters.getParameterValue("resourceId");
				this.storage.storeString(this.parameters.getParameterValue("storeResource"), this.id, Storage.Location.object);
			}
			else {
				this.id = this.storage.storeString(this.parameters.getParameterValue("storeResource"), Storage.Location.object);
			}
		}
		else if (this.parameters.containsKey("retrieveResourceId")) {
			String id = this.parameters.getParameterValue("retrieveResourceId");
			// if it doesn't exist, try to retrieve from previous storage
			if (!storage.isStored(id, Storage.Location.object) && storage instanceof FileStorage) {
				File file = FileMigrationFactory.getStoredObjectFile((FileStorage) storage, id, Location.object);
				if (file!=null && file.exists()) {
					((FileStorage) storage).copyResource(file, id, Storage.Location.object);
				}
			}
			if (parameters.getParameterBooleanValue("failQuietly")) {
				try {
					this.resource = this.storage.retrieveString(id, Storage.Location.object);
				} catch (IOException e) {
					this.resource = "";
					this.id = "";
				}
			} else {
				this.resource = this.storage.retrieveString(id, Storage.Location.object);
			}
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

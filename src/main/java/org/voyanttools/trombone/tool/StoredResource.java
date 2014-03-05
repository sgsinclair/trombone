package org.voyanttools.trombone.tool;

import java.io.IOException;

import org.voyanttools.trombone.storage.Storage;
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
		if (this.parameters.containsKey("verifyResource")) {
			String id = this.parameters.getParameterValue("verifyResource");
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
		else if (this.parameters.containsKey("retrieveResource")) {
			String id = this.parameters.getParameterValue("retrieveResource");
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

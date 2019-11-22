/**
 * 
 */
package org.voyanttools.trombone.tool.notebook;

import java.io.File;
import java.io.IOException;

import javax.mail.MessagingException;

import org.apache.commons.lang3.RandomStringUtils;
import org.voyanttools.trombone.mail.Mailer;
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
	
	final static String ID_AND_CODE_TEMPLATE = "^[\\w-]{4,16}$"; // regex for matching notebook id and access code
	
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
			String accessCode = parameters.getParameterValue("accessCode");
			if (accessCode.isEmpty() == false && accessCode.matches(ID_AND_CODE_TEMPLATE) == false) {
				throw new IOException("Access code does not conform to template.");
			}
			
			String notebookData = parameters.getParameterValue("data","");
			if (notebookData.trim().isEmpty()) {
				throw new IOException("Notebook contains no data.");
			}
			
			if (parameters.getParameterValue("id","").trim().isEmpty()==false) {
				id = parameters.getParameterValue("id");
				if (id.matches(ID_AND_CODE_TEMPLATE) == false) {
					throw new IOException("Notebook ID does not conform to template.");
				}
			} else {
				while(true) {
					id = RandomStringUtils.randomAlphanumeric(6);
					if (storage.isStored(id+".html", Location.notebook)==false) {
						break;
					}
				}
			}

			String storedAccessCode;
			try {
				storedAccessCode = storage.retrieveString(id, Location.notebook);
			} catch (IOException e) {
				storedAccessCode = null;
			}
			
			if (storedAccessCode == null || storedAccessCode.isEmpty() || storedAccessCode.equals(accessCode)) {
				storage.storeString(notebookData, id+".html", Location.notebook, true);
				storage.storeString(accessCode, id, Location.notebook, true);
				data = "true";
			} else {
				data = "false"; // don't throw error here because we don't want to trigger a popup in Voyant
			}
			
			String email = parameters.getParameterValue("email", "");
			if (email.trim().isEmpty() == false) {
				String notebookUrl = "https://voyant-tools.org/spyral/"+id+"/";
				String body = "<html><head></head><body><h1>Voyant Notebooks</h1><p>Your notebook: "+notebookUrl+"</p><p>Your access code: "+accessCode+"</p></body></html>";
				try {
					Mailer.sendMail(email, "Voyant Notebooks", body);
				} catch (MessagingException e) {
					System.out.println(e.toString());
				}
			}
		}
		
		// CHECK IF NOTEBOOK EXISTS
		else if (action.equals("exists")) {
			id = parameters.getParameterValue("id","");
			if (id.trim().isEmpty() == false) {
				data = storage.isStored(id+".html", Location.notebook) ? "true" : "false";
			} else {
				throw new IOException("No notebook ID provided.");
			}
		}
		
		// CHECK IF ACCESS CODE EXISTS
		else if (action.equals("protected")) {
			id = parameters.getParameterValue("id","");
			if (id.trim().isEmpty() == false) {
				try {
					data = storage.retrieveString(id, Location.notebook).isEmpty() ? "false" : "true";
				} catch (IOException e) {
					data = "false";
				}
			} else {
				throw new IOException("No notebook ID provided.");
			}
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

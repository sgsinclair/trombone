/**
 * 
 */
package org.voyanttools.trombone.tool.notebook;

import java.io.IOException;

import javax.mail.MessagingException;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.voyanttools.trombone.mail.Mailer;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.git.RepositoryManager;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("notebook")
public class GitNotebookManager extends AbstractTool {
	
	final static String ID_AND_CODE_TEMPLATE = "^[\\w-]{4,16}$"; // regex for matching notebook id and access code
	
	static final String NOTEBOOK_REPO_NAME = "spyral";
	static final String ACCESS_CODE_REPO_NAME = "spyral";
	
	String id = null; // notebook source (ID, URL, etc.)
	
	String data = null; // notebook data as JSON
	
	public GitNotebookManager(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}
	public float getVersion() {
		return super.getVersion()+1f;
	}
	
	@Override
	public void run() throws IOException {
		String action = parameters.getParameterValue("action", "");
		
		RepositoryManager rm = initRM();
		
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
					try {
						if (doesNotebookFileExist(rm, id+".html") == false) {
							break;
						}
					} catch (GitAPIException e) {
						throw new IOException(e.toString());
					}
				}
			}

			String storedAccessCode;
			try {
				storedAccessCode = getAccessCodeFile(rm, id);
			} catch (IOException | GitAPIException e) {
				storedAccessCode = "";
			}
			
			if (storedAccessCode.isEmpty() || storedAccessCode.equals(accessCode)) {
				try {
					rm.addFile(NOTEBOOK_REPO_NAME, id+".html", notebookData);
					rm.addFile(ACCESS_CODE_REPO_NAME, id, accessCode);
				} catch (IOException | GitAPIException e) {
					throw new IOException(e.toString());
				}
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
				try {
					data = doesNotebookFileExist(rm, id+".html") ? "true" : "false";
				} catch (GitAPIException e) {
					throw new IOException(e.toString());
				}
			} else {
				throw new IOException("No notebook ID provided.");
			}
		}
		
		// CHECK IF ACCESS CODE EXISTS
		else if (action.equals("protected")) {
			id = parameters.getParameterValue("id","");
			if (id.trim().isEmpty() == false) {
				try {
					String accessCode = getAccessCodeFile(rm, id);
					if (accessCode.isEmpty()) {
						data = "false";
					} else {
						data = "true";
					}
				} catch (IOException | GitAPIException e) {
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
			} else {
				try {
					data = RepositoryManager.getRepositoryFile(rm.getRepository(NOTEBOOK_REPO_NAME), id+".html");
				} catch (GitAPIException e) {
					throw new IOException(e.toString());
				}
			}
			if (data==null) {
				throw new IOException("Unable to retrieve notebook: "+id);
			}
		}
	}

	private RepositoryManager initRM() throws IOException {
		try {
			RepositoryManager rm = new RepositoryManager();
			try {
				rm.getRepository(NOTEBOOK_REPO_NAME);
			} catch (RefNotFoundException e) {
				try (Git git = rm.setupRepository(NOTEBOOK_REPO_NAME)) {}
			}
			
			try {
				rm.getRepository(ACCESS_CODE_REPO_NAME);
			} catch (RefNotFoundException e) {
				try (Git git = rm.setupRepository(ACCESS_CODE_REPO_NAME)) {}
			}
			

			return rm;
		} catch (GitAPIException e) {
			throw new IOException(e.toString());
		}
	}
	
	private boolean doesNotebookFileExist(RepositoryManager rm, String filename) throws IOException, GitAPIException {
		return RepositoryManager.doesRepositoryFileExist(rm.getRepository(NOTEBOOK_REPO_NAME), filename);
	}
	
	private String getAccessCodeFile(RepositoryManager rm, String filename) throws IOException, GitAPIException {
		return RepositoryManager.getRepositoryFile(rm.getRepository(ACCESS_CODE_REPO_NAME), filename);
	}

}

/**
 * 
 */
package org.voyanttools.trombone.tool.notebook;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.mail.MessagingException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.notes.Note;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.voyanttools.trombone.mail.Mailer;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.Storage.Location;
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
	
	final static String NOTEBOOK_REPO_NAME = "spyral";
	final static String ACCESS_CODE_REPO_NAME = "spyral";
	
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
		
		RepositoryManager rm = initRM(); // check here if we need to init a memory storage version instead
		
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
			
			String notebookMetadata = parameters.getParameterValue("metadata", "");
			if (notebookMetadata.trim().isEmpty()) {
				throw new IOException("Notebook contains no metadata.");
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
					RevCommit commit = rm.addFile(NOTEBOOK_REPO_NAME, id+".html", notebookData);
					rm.addNoteToCommit(NOTEBOOK_REPO_NAME, commit, notebookMetadata);
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
				} catch (IOException | GitAPIException e) {
					// try old notebook location
					NotebookManager nm = new NotebookManager(storage, parameters);
					nm.run();
					data = nm.data;
					
					try {
						log("Migrating notebook '"+id+"'.");
						migrateNotebook(rm, id, data);
					} catch (GitAPIException e1) {
						log("Tried to migrate '"+id+"' notebook but failed.");
					}
				}
			}
			if (data==null) {
				throw new IOException("Unable to retrieve notebook: "+id);
			}
		}
		
		else if (action.equals("catalogue")) {
			try {
				Repository notebookRepo = rm.getRepository(NOTEBOOK_REPO_NAME);
				List<String> files = RepositoryManager.getRepositoryContents(notebookRepo);
				List<String> notebooks = files.stream().filter(f -> f.endsWith(".html")).collect(Collectors.toList());
				
				List<String> notes = new ArrayList<String>();
				
				for (String notebook : notebooks) {
//					System.out.println("---");
//					System.out.println(notebook);
					
					RevCommit rc = RepositoryManager.getMostRecentCommitForFile(notebookRepo, notebook);
//					System.out.println(rc.getName());
					
					try (Git git = new Git(notebookRepo)) {
						Note note = git.notesShow().setObjectId(rc).call();
						ObjectLoader loader = notebookRepo.open(note.getData());
						String noteContents = RepositoryManager.getStringFromObjectLoader(loader);
						notes.add(noteContents);
					} catch (IncorrectObjectTypeException | NullPointerException e) {
//						System.out.println("no note for "+notebook);
						String metadata = getMetadataFromNotebook(rm, notebook.replaceFirst(".html$", ""));
						if (metadata != null) {
							notes.add(metadata);
						}
					}
					
				}
				
				data = "["+String.join(",", notes)+"]";
			} catch (GitAPIException e) {
				throw new IOException(e.toString());
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
	
	private void migrateNotebook(RepositoryManager rm, String id, String data) throws IOException, GitAPIException {
		if (doesNotebookFileExist(rm, id+".html")) {
			return; // this should never happen, because if there was a collision, the git notebook would be loaded instead
		}
		
		String accessCode = storage.retrieveString(id, Location.notebook);
		
		RevCommit commit = rm.addFile(NOTEBOOK_REPO_NAME, id+".html", data);
		String notebookMetadata = getMetadataFromNotebook(rm, id);
		if (notebookMetadata != null) {
			rm.addNoteToCommit(NOTEBOOK_REPO_NAME, commit, notebookMetadata);
		}
		rm.addFile(ACCESS_CODE_REPO_NAME, id, accessCode);
	}
	
	private String getMetadataFromNotebook(RepositoryManager rm, String notebookId) {
		String notebookHtml;
		try {
			notebookHtml = RepositoryManager.getRepositoryFile(rm.getRepository(NOTEBOOK_REPO_NAME), notebookId+".html");
		} catch (IOException | GitAPIException e) {
			return null;
		}
		
		Document doc;
		try {
			doc = Jsoup.parse(notebookHtml);
		} catch (Exception e) {
			return null;
		}
		
		Elements metaEls = doc.select("head > meta");
		StringBuilder metadata = new StringBuilder("{\"id\":\""+notebookId+"\"");
		for (int i=0, len=metaEls.size(); i<len; i++) {
			Element meta = metaEls.get(i);
			if (meta.hasAttr("name") && meta.hasAttr("content")) {
				String field = meta.attr("name");
				String value = meta.attr("content");
				if (field.equals("title") || field.equals("description")) {
					value = value.replaceAll("<\\/?\\w+.*?>", ""); // remove possible tags
				}
				metadata.append(",\""+field+"\":\""+value+"\"");
			}
		}
		metadata.append("}");
		return metadata.toString();
	}

}

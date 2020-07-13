package org.voyanttools.trombone.storage.git;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.notes.Note;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;


public class RepositoryManager {

	public static final String DEFAULT_TROMBOME_DIRECTORY_NAME = "trombone5_2";
	
	public static final File DEFAULT_GIT_DIRECTORY = new File(System.getProperty("java.io.tmpdir"), DEFAULT_TROMBOME_DIRECTORY_NAME);
	
	public static final String DEFAULT_GIT_NAME = "spyral";
	public static final String DEFAULT_GIT_EMAIL = "spyral@voyant-tools.org";
	
	private File storageLocation;

	public RepositoryManager() throws IOException, GitAPIException {
		this(DEFAULT_GIT_DIRECTORY);
	}
	
	public RepositoryManager(File directory) throws IOException, GitAPIException {
		storageLocation = directory;
		System.out.println(directory.getAbsolutePath());
	}
	
	public Git setupRepository(String repoName) throws IOException, GitAPIException {
		File repoFile = new File(storageLocation, repoName);
		if (repoFile.exists() == false) {
			if (!repoFile.mkdirs()) {
				throw new IOException("Unable to create git directory: "+storageLocation);
			}
		}
		
		Git git = RepositoryManager.createRepository(repoFile);
		return git;
	}
	
	private static Git createRepository(File repoDir) throws GitAPIException, IOException {
		Git git = Git.init()
			.setDirectory(repoDir)
			.setBare(false)
			.call();
		
		StoredConfig config = git.getRepository().getConfig();
		config.setString("user", null, "name", DEFAULT_GIT_NAME);
		config.setString("user", null, "email", DEFAULT_GIT_EMAIL);
		config.save();
		
		git.commit()
			.setMessage("Created "+repoDir.getAbsolutePath())
			.call();
		
		return git;
	}
	
	public Repository getRepository(String repoName) throws IOException, GitAPIException {
		Repository repo = new FileRepositoryBuilder()
			.setGitDir(new File(storageLocation, repoName+File.separator+".git"))
			.build();
		
		if (repo.exactRef("HEAD") == null) {
			throw new RefNotFoundException("Repository does not exist: "+repoName);
		}
		
		return repo;
	}
	
	
	public RevCommit addFile(String repoName, String filename, String content) throws IOException, GitAPIException {
		File file = new File(storageLocation, repoName+File.separator+filename);
		FileUtils.writeStringToFile(file, content, "UTF-8");
		
		try (Git git = new Git(getRepository(repoName))) {
			git.add().addFilepattern(filename).call();
			RevCommit commit = git.commit().setMessage("Added file: "+filename).call();
			return commit;
		}
	}
	
	public Note addNoteToCommit(String repoName, RevCommit commit, String noteContent) throws IOException, GitAPIException {
		try (Git git = new Git(getRepository(repoName))) {
			Note note = git.notesAdd().setMessage(noteContent).setObjectId(commit).call();
			return note;
		}
	}
	
	
	private static RevTree getTree(Repository repository) throws IOException {
        ObjectId lastCommitId = repository.resolve(Constants.HEAD);
        return getTree(repository, lastCommitId);
    }
	
	/**
	 * Get the tree for traversing the repo at a specific commit point.
	 * @param repository
	 * @param commitId
	 * @return
	 * @throws IOException
	 */
	private static RevTree getTree(Repository repository, ObjectId commitId) throws IOException {
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(commitId);
            RevTree tree = commit.getTree();
            return tree;
        }
	}
	
	/**
	 * Get a list of the top level files in a repo (ignoring any directories).
	 * @param repository
	 * @return
	 * @throws IOException
	 */
	public static List<String> getRepositoryContents(Repository repository) throws IOException {
		RevTree tree = getTree(repository);
		try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(false);
        
            ArrayList<String> contents = new ArrayList<String>();
            while(treeWalk.next()) {
            	contents.add(treeWalk.getNameString());
            }
            
            return contents;
		}
	}
	
	public static List<String> getRepositoryContents(Repository repository, String filter) throws IOException {
		RevTree tree = getTree(repository);
		try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(false);
            treeWalk.setFilter(PathFilter.create(filter));
        
            ArrayList<String> contents = new ArrayList<String>();
            while(treeWalk.next()) {
            	contents.add(treeWalk.getNameString());
            }
            
            return contents;
		}
	}
	
	public static boolean doesRepositoryFileExist(Repository repository, String filename) throws IOException {
		RevTree tree = getTree(repository);
		try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(false);
            treeWalk.setFilter(PathFilter.create(filename));
            if (!treeWalk.next()) {
                return false;
            } else {
            	return true;
            }
		}
	}
	
	public static String getRepositoryFile(Repository repository, String filename) throws IOException {
		RevTree tree = getTree(repository);
		try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(false);
            treeWalk.setFilter(PathFilter.create(filename));
            if (!treeWalk.next()) {
                throw new IOException("Could not find file: "+filename);
            }
            
            ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
            String contents = RepositoryManager.getStringFromObjectLoader(loader);
            
            return contents;
		}
	}
	
	public static RevCommit getMostRecentCommitForFile(Repository repository, String filename) throws IOException, GitAPIException {
		try (Git git = new Git(repository)) {
			Iterable<RevCommit> logs = git.log()
		            .addPath(filename)
		            .setMaxCount(1)
		            .call();
			
			RevCommit rc = logs.iterator().next();
			return rc;
		}
	}
	
	public static String getStringFromObjectLoader(ObjectLoader loader) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        loader.copyTo(baos);
        return baos.toString(Charset.forName("UTF-8"));
	}
	
	public static Set<String> getUntrackedFiles(Repository repository) throws GitAPIException {
		try(Git git = new Git(repository)) {
			Status status = git.status().call();
			Set<String> untracked = status.getUntracked();
			return untracked;
		}
	}
}

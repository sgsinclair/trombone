package org.voyanttools.trombone.storage.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.voyanttools.trombone.storage.DirectoryFactory;

public class SingleDirectoryFactory implements DirectoryFactory {

	private Path path;
	
	public SingleDirectoryFactory(Path path) {
		this.path = path;
	}

	@Override
	public Directory getDirectory(String corpus) throws IOException {
		if (Files.exists(path)==false) {
			Files.createDirectories(path);
		}
		return new NIOFSDirectory(path);
	}

}

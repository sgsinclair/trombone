package org.voyanttools.trombone.storage.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.voyanttools.trombone.storage.DirectoryFactory;

public class PerCorpusDirectoryFactory implements DirectoryFactory {

	private Path basePath;
	public PerCorpusDirectoryFactory(Path basePath) {
		this.basePath = basePath;
	}

	@Override
	public Directory getDirectory(String corpus) throws IOException {
		Path path = Paths.get(basePath.toString(), corpus);
		if (Files.exists(path)==false) {
			Files.createDirectories(path);
		}
		return new NIOFSDirectory(path);
	}

}

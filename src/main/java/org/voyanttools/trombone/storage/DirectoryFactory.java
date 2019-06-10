package org.voyanttools.trombone.storage;

import java.io.IOException;

import org.apache.lucene.store.Directory;

public interface DirectoryFactory {
	Directory getDirectory(String corpus) throws IOException;
}

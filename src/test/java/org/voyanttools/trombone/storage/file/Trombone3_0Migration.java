/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Migrator;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.TestHelper;

/**
 * @author sgs
 *
 */
public class Trombone3_0Migration {

	@Test
	public void test() throws IOException, ZipException {
		File base = new File(System.getProperty("java.io.tmpdir"), "_test_"+UUID.randomUUID());
		File oldStorageDirectory = new File(base, FileTrombone3_0Migrator.DEFAULT_TROMBOME_DIRECTORY_NAME);
		oldStorageDirectory.mkdir();
		File file = TestHelper.getResource("migration/trombone3_0.zip");
		String oldCorpusId = "old";
		File oldCorpus = new File(oldStorageDirectory, oldCorpusId);
		unzip(file, oldCorpus);
		
		
		FileStorage storage = new FileStorage(new File(base, FileStorage.DEFAULT_TROMBOME_DIRECTORY_NAME));
		Migrator migrator = FileMigrationFactory.getMigrator(storage, oldCorpusId);
		String id = migrator.getMigratedCorpusId();
		
		Corpus corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(13, corpus.size());
		for (IndexedDocument doc : corpus) {
			Assert.assertFalse(doc.getMetadata().getTitle().equals("rawbytes"));
		}
		
		storage.destroy();
		FileUtils.deleteDirectory(base);
		
	}
	
	private void unzip(File file, File destination) throws ZipException {
        ZipFile zipFile = new ZipFile(file);
        zipFile.extractAll(destination.getPath());
	}

}

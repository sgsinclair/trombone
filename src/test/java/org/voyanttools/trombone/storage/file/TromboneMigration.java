/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Migrator;
import org.voyanttools.trombone.util.TestHelper;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * @author sgs
 *
 */
public class TromboneMigration {

	@Test
	public void testTrombone3_0() throws IOException, ZipException {
		
		
		File base = new File(System.getProperty("java.io.tmpdir"), "_test_"+UUID.randomUUID());
		FileStorage storage = new FileStorage(new File(base, FileStorage.DEFAULT_TROMBOME_DIRECTORY_NAME));
		
		File oldStorageDirectory = new File(base, FileTrombone3_0Migrator.DEFAULT_TROMBOME_DIRECTORY_NAME);
		oldStorageDirectory.mkdir();
		File file = TestHelper.getResource("migration/trombone3_0.zip");
		new ZipFile(file).extractAll(oldStorageDirectory.getPath());

		Migrator migrator;
		String id;
		Corpus corpus;
		
		// test the bundle of formats
		migrator = FileMigrationFactory.getMigrator(storage, "one");
		id = migrator.getMigratedCorpusId();
		corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(13, corpus.size());
		for (IndexedDocument doc : corpus) {
			Assert.assertFalse(doc.getMetadata().getTitle().equals("rawbytes"));
		}
		
		// test handling of title and author metadata
		migrator = FileMigrationFactory.getMigrator(storage, "two");
		id = migrator.getMigratedCorpusId();
		corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(2, corpus.size());
		assertEquals("", corpus.getDocument(0).getMetadata().getAuthor());
		assertEquals("Defined", corpus.getDocument(1).getMetadata().getAuthor());
		// test transfer of stoplist
		List<String> stopwords = storage.retrieveStrings("1458405208292sw");
		assertEquals(1, stopwords.size());
		assertEquals("test", stopwords.get(0));
		
		storage.destroy();
		FileUtils.deleteDirectory(base);
		
	}
}

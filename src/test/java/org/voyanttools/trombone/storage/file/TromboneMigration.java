/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.Storage.Location;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;
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
		
		// create a dummy migrator to get the proper destination directory for unzipping
		AbstractFileMigrator dummyMigrator = new FileTrombone3_0Migrator(storage, "");
		Assert.assertNull(null, dummyMigrator.getMigratedCorpusId());

		File oldStorageDirectory = dummyMigrator.getSourceTromboneDirectory();
		oldStorageDirectory.mkdir();
		File file = TestHelper.getResource("migration/trombone3_0.zip");
		new ZipFile(file).extractAll(oldStorageDirectory.getPath());

		FileMigrator migrator;
		String id;
		Corpus corpus;
		
		// test the bundle of formats
		migrator = FileMigrationFactory.getMigrator(storage, "one");
		assertTrue(migrator instanceof FileTrombone3_0Migrator);
		id = migrator.getMigratedCorpusId();
		corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(13, corpus.size());
		for (IndexedDocument doc : corpus) {
			Assert.assertFalse(doc.getMetadata().getTitle().equals("rawbytes"));
		}
		
		// test handling of title and author metadata
		migrator = FileMigrationFactory.getMigrator(storage, "two");
		assertTrue(migrator instanceof FileTrombone3_0Migrator);
		id = migrator.getMigratedCorpusId();
		corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(2, corpus.size());
		assertEquals("", corpus.getDocument(0).getMetadata().getAuthor());
		assertEquals("Defined", corpus.getDocument(1).getMetadata().getAuthor());
		// test transfer of stoplist
		List<String> stopwords = storage.retrieveStrings("1458405208292sw", Storage.Location.object);
		assertEquals(1, stopwords.size());
		assertEquals("test", stopwords.get(0));
		
		storage.destroy();
		FileUtils.deleteDirectory(base);
		
	}
	
	@Test
	public void testTrombone4_0() throws IOException, ZipException {
		
		File base = new File(System.getProperty("java.io.tmpdir"), "_test_"+UUID.randomUUID());
		FileStorage storage = new FileStorage(new File(base, FileStorage.DEFAULT_TROMBOME_DIRECTORY_NAME));
		
		// create a dummy migrator to get the proper destination directory for unzipping
		AbstractFileMigrator dummyMigrator = new FileTrombone4_0Migrator(storage, "");
		Assert.assertNull(null, dummyMigrator.getMigratedCorpusId());
		
		// unzip trombone 4.0 contents
		File oldStorageDirectory = dummyMigrator.getSourceTromboneDirectory();
		oldStorageDirectory.mkdir();
		File file = TestHelper.getResource("migration/trombone4_0.zip");
		new ZipFile(file).extractAll(oldStorageDirectory.getPath());

		FileMigrator migrator;
		String id;
		Corpus corpus;
		
		// test the bundle of formats
		migrator = FileMigrationFactory.getMigrator(storage, "1cb657d4f807a824536059c9ade0d907");
		assertTrue(migrator instanceof FileTrombone4_0Migrator);
		id = migrator.getMigratedCorpusId();
		corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(15, corpus.size());
		for (IndexedDocument doc : corpus) {
			Assert.assertFalse(doc.getMetadata().getTitle().equals("rawbytes"));
		}
		
		// test handling of title and author metadata
		migrator = FileMigrationFactory.getMigrator(storage, "824b82f75e5053a0f52a0a3db2654d15");
		assertTrue(migrator instanceof FileTrombone4_0Migrator);
		id = migrator.getMigratedCorpusId();
		corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(1, corpus.size());
		assertEquals("Il était une fois.", corpus.getDocument(0).getMetadata().getAuthor());
		assertEquals("un texte intéressant et un test. ⚠️", corpus.getDocument(0).getMetadata().getTitle());
		
		storage.destroy();
		FileUtils.deleteDirectory(base);
		
	}

	@Test
	public void testTrombone4_1() throws IOException, ZipException {
		
		File base = new File(System.getProperty("java.io.tmpdir"), "_test_"+UUID.randomUUID());
		FileStorage storage = new FileStorage(new File(base, FileStorage.DEFAULT_TROMBOME_DIRECTORY_NAME));
		
		// create a dummy migrator to get the proper destination directory for unzipping
		FileTrombone4_1Migrator dummyMigrator = new FileTrombone4_1Migrator(storage, "");
		Assert.assertNull(null, dummyMigrator.getMigratedCorpusId());
		
		// unzip trombone 4.1 contents
		File oldStorageDirectory = dummyMigrator.getSourceTromboneDirectory();
		oldStorageDirectory.mkdir();
		File file = TestHelper.getResource("migration/trombone4_1.zip");
		new ZipFile(file).extractAll(oldStorageDirectory.getPath());

		FileMigrator migrator;
		String id;
		Corpus corpus;
		String corpusIdToMigrate;
		
		// test the bundle of formats
		corpusIdToMigrate = "d0be1ce35c9941b21af22260a47938e2";
		migrator = FileMigrationFactory.getMigrator(storage, corpusIdToMigrate);
		assertTrue(migrator instanceof FileTrombone4_1Migrator);
		id = migrator.getMigratedCorpusId();
		assertTrue(storage.getCorpusStorage().corpusExists(corpusIdToMigrate));
		assertTrue(storage.getCorpusStorage().corpusExists(id));
		corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(15, corpus.size());
		for (IndexedDocument doc : corpus) {
			Assert.assertFalse(doc.getMetadata().getTitle().equals("rawbytes"));
		}
		
		// test handling of title and author metadata
		corpusIdToMigrate = "e0a54420a5555aa00dacd1ccf0a2ba0e";
		migrator = FileMigrationFactory.getMigrator(storage, corpusIdToMigrate);
		assertTrue(migrator instanceof FileTrombone4_1Migrator);
		id = migrator.getMigratedCorpusId();
		assertTrue(storage.getCorpusStorage().corpusExists(corpusIdToMigrate));
		assertTrue(storage.getCorpusStorage().corpusExists(id));
		corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(1, corpus.size());
		assertEquals("Il était une fois.", corpus.getDocument(0).getMetadata().getTitle());
		assertEquals("un texte intéressant et un test. ⚠️", corpus.getDocument(0).getMetadata().getAuthor());

		// those should be using stored top-level original sources, now try if one of them has disappeared (only use corpus creation parameters)
		corpusIdToMigrate = "d0be1ce35c9941b21af22260a47938e2";
		migrator = FileMigrationFactory.getMigrator(storage, corpusIdToMigrate);
		assertTrue(migrator instanceof FileTrombone4_1Migrator);
		// remove the top-level source zip directory
		File deleteDir = new File(dummyMigrator.getSourceTromboneDocumentsDirectory(), "d807e3732cc09d24783201aed49d5742");
		assertTrue(deleteDir.exists());
		FileUtils.deleteDirectory(deleteDir);
		assertFalse(deleteDir.exists());
		// we have to modify the parameters to point to an existing file, not the one that was used when the zip was created
		FlexibleParameters corpusCreationParameters = ((FileTrombone4_1Migrator) migrator).getCorpusCreationParameters();
		File newUploadFile = TestHelper.getResource("archive/chars.zip");
		assertTrue(newUploadFile.exists());
		corpusCreationParameters.setParameter("upload", newUploadFile.getAbsolutePath());
		File newCorpusParametersFile =  new File(((FileTrombone4_1Migrator) migrator).getSourceTromboneCorpusDirectory(), "parameters.xml");
		assertTrue(newCorpusParametersFile.exists());
		corpusCreationParameters.saveFlexibleParameters(newCorpusParametersFile);
		// now proceed
		id = migrator.getMigratedCorpusId();
		assertTrue(storage.getCorpusStorage().corpusExists(id));
		assertTrue(storage.getCorpusStorage().corpusExists(corpusIdToMigrate));
		corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(15, corpus.size());
		for (IndexedDocument doc : corpus) {
			Assert.assertFalse(doc.getMetadata().getTitle().equals("rawbytes"));
		}
		
		// now try if the corpus creation parameters don't work out (so simple migration with very limited metadata
		newUploadFile = new File(UUID.randomUUID().toString());
		assertFalse(newUploadFile.exists());
		corpusCreationParameters.setParameter("upload", UUID.randomUUID().toString());
		newCorpusParametersFile =  new File(((FileTrombone4_1Migrator) migrator).getSourceTromboneCorpusDirectory(), "parameters.xml");
		corpusCreationParameters.saveFlexibleParameters(newCorpusParametersFile);
		// now proceed
		id = migrator.getMigratedCorpusId();
		assertTrue(storage.getCorpusStorage().corpusExists(id));
		assertTrue(storage.getCorpusStorage().corpusExists(corpusIdToMigrate));
		corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(15, corpus.size());
		for (IndexedDocument doc : corpus) {
			Assert.assertFalse(doc.getMetadata().getTitle().equals("rawbytes"));
		}
		
		// test migration of resources
		id = "7f96fa278a1cc64fc298ab808bcc2682";
		assertFalse(storage.isStored(id, Storage.Location.object));
		file = FileMigrationFactory.getStoredObjectFile(storage, id, Location.object);
		assertTrue(file.exists());
		assertTrue(storage.copyResource(file, id, Storage.Location.object));
		assertTrue(storage.isStored(id, Storage.Location.object));
		
		// test migration of non-existent resource
		id = "z";
		assertFalse(storage.isStored(id, Storage.Location.object));
		file = FileMigrationFactory.getStoredObjectFile(storage, id, Location.object);
		assertNull(file);
		
		// test migration of recovered directory
		File recoveryFile = new File(base, oldStorageDirectory.getName()+".2");
		FileUtils.copyDirectory(oldStorageDirectory, new File(base, oldStorageDirectory.getName()+".1"));
		FileUtils.copyDirectory(oldStorageDirectory, recoveryFile);
		recoveryFile.setLastModified(recoveryFile.lastModified()+1); // make sure it's the most recent
		
		
		
		storage.destroy();
		FileUtils.deleteDirectory(base);
		
	}

	@Test
	public void testTrombone4_2() throws IOException, ZipException {
		
		File base = new File(System.getProperty("java.io.tmpdir"), "_test_"+UUID.randomUUID());
		FileStorage storage = new FileStorage(new File(base, FileStorage.DEFAULT_TROMBOME_DIRECTORY_NAME));
		
		// create a dummy migrator to get the proper destination directory for unzipping
		FileTrombone4_2Migrator dummyMigrator = new FileTrombone4_2Migrator(storage, "");
		Assert.assertNull(null, dummyMigrator.getMigratedCorpusId());
		
		// unzip trombone 4.2 contents
		File oldStorageDirectory = dummyMigrator.getSourceTromboneDirectory();
		oldStorageDirectory.mkdir();
		File file = TestHelper.getResource("migration/trombone4_2.zip");
		new ZipFile(file).extractAll(oldStorageDirectory.getPath());

		FileMigrator migrator;
		String id;
		Corpus corpus;
		String corpusIdToMigrate;
		
		// test the bundle of formats
		corpusIdToMigrate = "45ce972416fa9278974a42830668d7ff";
		migrator = FileMigrationFactory.getMigrator(storage, corpusIdToMigrate);
		assertTrue(migrator instanceof FileTrombone4_2Migrator);
		id = migrator.getMigratedCorpusId();
		assertTrue(storage.getCorpusStorage().corpusExists(corpusIdToMigrate));
		assertTrue(storage.getCorpusStorage().corpusExists(id));
		corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(15, corpus.size());
		for (IndexedDocument doc : corpus) {
			Assert.assertFalse(doc.getMetadata().getTitle().equals("rawbytes"));
		}
		
		// test handling of title and author metadata
		corpusIdToMigrate = "677eea3feacb2852718c1880602ddc8f";
		migrator = FileMigrationFactory.getMigrator(storage, corpusIdToMigrate);
		assertTrue(migrator instanceof FileTrombone4_2Migrator);
		id = migrator.getMigratedCorpusId();
		assertTrue(storage.getCorpusStorage().corpusExists(corpusIdToMigrate));
		assertTrue(storage.getCorpusStorage().corpusExists(id));
		corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(1, corpus.size());
		assertEquals("Il était une fois.", corpus.getDocument(0).getMetadata().getTitle());
		assertEquals("un texte intéressant et un test. ⚠️", corpus.getDocument(0).getMetadata().getAuthor());

		// those should be using stored top-level original sources, now try if one of them has disappeared (only use corpus creation parameters)
		corpusIdToMigrate = "45ce972416fa9278974a42830668d7ff";
		migrator = FileMigrationFactory.getMigrator(storage, corpusIdToMigrate);
		assertTrue(migrator instanceof FileTrombone4_2Migrator);
		// remove the top-level source zip directory
		File deleteDir = new File(dummyMigrator.getSourceTromboneDocumentsDirectory(), "f279e4ef454f03de4e9a31b4c6032fef");
		assertTrue(deleteDir.exists());
		FileUtils.deleteDirectory(deleteDir);
		assertFalse(deleteDir.exists());
		// we have to modify the parameters to point to an existing file, not the one that was used when the zip was created
		FlexibleParameters corpusCreationParameters = ((FileTrombone4_2Migrator) migrator).getCorpusCreationParameters();
		File newUploadFile = TestHelper.getResource("archive/chars.zip");
		assertTrue(newUploadFile.exists());
		corpusCreationParameters.setParameter("upload", newUploadFile.getAbsolutePath());
		File newCorpusParametersFile =  new File(((FileTrombone4_2Migrator) migrator).getSourceTromboneCorpusDirectory(), "parameters.xml");
		assertTrue(newCorpusParametersFile.exists());
		corpusCreationParameters.saveFlexibleParameters(newCorpusParametersFile);
		// now proceed
		id = migrator.getMigratedCorpusId();
		assertTrue(storage.getCorpusStorage().corpusExists(id));
		assertTrue(storage.getCorpusStorage().corpusExists(corpusIdToMigrate));
		corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(15, corpus.size());
		for (IndexedDocument doc : corpus) {
			Assert.assertFalse(doc.getMetadata().getTitle().equals("rawbytes"));
		}
		
		// now try if the corpus creation parameters don't work out (so simple migration with very limited metadata
		newUploadFile = new File(UUID.randomUUID().toString());
		assertFalse(newUploadFile.exists());
		corpusCreationParameters.setParameter("upload", UUID.randomUUID().toString());
		newCorpusParametersFile =  new File(((FileTrombone4_2Migrator) migrator).getSourceTromboneCorpusDirectory(), "parameters.xml");
		corpusCreationParameters.saveFlexibleParameters(newCorpusParametersFile);
		// now proceed
		id = migrator.getMigratedCorpusId();
		assertTrue(storage.getCorpusStorage().corpusExists(id));
		assertTrue(storage.getCorpusStorage().corpusExists(corpusIdToMigrate));
		corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(15, corpus.size());
		for (IndexedDocument doc : corpus) {
			Assert.assertFalse(doc.getMetadata().getTitle().equals("rawbytes"));
		}
		
		// test migration of resources
		id = "0366879fcdc310ae2511e58ebb4ae64b";
		assertFalse(storage.isStored(id, Storage.Location.object));
		file = FileMigrationFactory.getStoredObjectFile(storage, id, Location.object);
		assertTrue(file.exists());
		assertTrue(storage.copyResource(file, id, Storage.Location.object));
		assertTrue(storage.isStored(id, Storage.Location.object));
		
		// test migration of non-existent resource
		id = "z";
		assertFalse(storage.isStored(id, Storage.Location.object));
		file = FileMigrationFactory.getStoredObjectFile(storage, id, Location.object);
		assertNull(file);
		
		storage.destroy();
		FileUtils.deleteDirectory(base);
		
	}
	
	@Test
	public void testTromboneCurrent() throws IOException, ZipException {
		
		File base = new File(System.getProperty("java.io.tmpdir"), "_test_"+UUID.randomUUID());
		File current = new File(base, FileStorage.DEFAULT_TROMBOME_DIRECTORY_NAME);
		File recovery = new File(base, FileStorage.DEFAULT_TROMBOME_DIRECTORY_NAME+".1");
		FileStorage storage = new FileStorage(current);
		
		RealCorpusCreator creator;
		FlexibleParameters parameters;
		
		parameters = new FlexibleParameters();
		parameters.addParameter("string",  "dark and stormy night in document one");
		parameters.addParameter("tool", "StepEnabledIndexedCorpusCreator");
		parameters.addParameter("noCache", 1);

		creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		String corpusIdToMigrate = creator.getStoredId();
		
		storage.getLuceneManager().getIndexWriter().commit();
		storage.getLuceneManager().getIndexWriter().close();
		FileUtils.moveDirectory(current, recovery);
		assertTrue(recovery.exists());
		assertFalse(current.exists());
		
		storage = new FileStorage(current);
		FileMigrator migrator = FileMigrationFactory.getMigrator(storage, corpusIdToMigrate);
		assertTrue(migrator instanceof FileTromboneCurrentMigrator);
		assertEquals(((FileTromboneCurrentMigrator) migrator).getSourceTromboneDirectory().getPath(), recovery.getPath());
		String id = migrator.getMigratedCorpusId();
		assertTrue(storage.getCorpusStorage().corpusExists(corpusIdToMigrate));
		assertTrue(storage.getCorpusStorage().corpusExists(id));
		Corpus corpus = storage.getCorpusStorage().getCorpus(id);
		assertEquals(1, corpus.size());
		Assert.assertTrue(corpus.getDocument(0).getMetadata().getTitle().equals("dark and stormy night in document one"));

		
		storage.destroy();
		FileUtils.deleteDirectory(base);


	}
}

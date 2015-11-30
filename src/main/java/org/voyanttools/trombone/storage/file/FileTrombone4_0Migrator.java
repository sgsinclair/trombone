/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Migrator;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class FileTrombone4_0Migrator implements Migrator {
	
	private static String DEFAULT_TROMBOME_DIRECTORY_NAME = "trombone4_0";
	
	private FileStorage storage;
	
	private String id;

	/**
	 * 
	 */
	public FileTrombone4_0Migrator() {
		// TODO Auto-generated constructor stub
	}

	public FileTrombone4_0Migrator(FileStorage storage, String id) {
		this.storage = storage;
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.storage.Migrator#getMigratedCorpusId()
	 */
	@Override
	public String getMigratedCorpusId() throws IOException {
		File corpusMetadataFile = new File(getMigrationSourceCorpusDirectory(storage, id), "metadata.xml");
		FlexibleParameters params = FlexibleParameters.loadFlexibleParameters(corpusMetadataFile);
		String[] ids = params.getParameterValues("documentIds");

		File oldStoredDocumentSourcesDirectory = new File(getMigrationSourceDirectory(storage), "stored_document_sources");
		// this part somewhat reproduces {@link DocumentStorer}, but custom setting metadata along the way
		// we use the rawbytes instead of the tokens to simplify and to allow the new code to do better extraction
		StoredDocumentSourceStorage storedDocumentStorage = storage.getStoredDocumentSourceStorage();
		List<String> storedIds = new ArrayList<String>();
		for (String id : ids) {
			File documentDirectory = new File(oldStoredDocumentSourcesDirectory, id);
			InputSource inputSource = new FileInputSource(new File(documentDirectory, "raw_bytes.gz"));
			DocumentMetadata documentMetadata = inputSource.getMetadata();
			documentMetadata.setSource(Source.STREAM); // claim that this is a stream since it shouldn't be recoverable
			documentMetadata.setLocation("Trombone 3.0 Migration");
			documentMetadata.setTitle(""); // we don't want the default "rawbytes" title for a file
			StoredDocumentSource storedDocumentSource = storedDocumentStorage.getStoredDocumentSource(inputSource);
			storedIds.add(storedDocumentSource.getId());
		}
		String storedId = storage.storeStrings(storedIds);
		
		// now we can continue on with the corpus building
		FlexibleParameters parameters = new FlexibleParameters();
		parameters.setParameter("nextCorpusCreatorStep", "extract"); // I *think* we can skip expand at the document level
		parameters.setParameter("storedId", storedId);
		RealCorpusCreator realCorpusCreator = new RealCorpusCreator(storage, parameters);
		realCorpusCreator.run();
		
		return realCorpusCreator.getStoredId();
	}
	
	static boolean isMigratable(FileStorage storage, String id) {
		File sourceCorpusDirectory = getMigrationSourceCorpusDirectory(storage, id);
		return sourceCorpusDirectory.exists();
	}
	
	private static File getMigrationSourceCorpusDirectory(FileStorage storage, String id) {
		return new File(new File(getMigrationSourceDirectory(storage), "corpora"), id);
	}
	
	private static File getMigrationSourceDirectory(FileStorage storage) {
		return new File(storage.storageLocation.getParentFile(), DEFAULT_TROMBOME_DIRECTORY_NAME);
	}


}

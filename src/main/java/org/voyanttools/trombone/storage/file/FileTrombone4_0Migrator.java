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
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class FileTrombone4_0Migrator extends FileTrombone3_0Migrator {
	
	protected static String DEFAULT_TROMBOME_DIRECTORY_NAME = "trombone4_0";
	
	public FileTrombone4_0Migrator(FileStorage storage, String id) {
		super(storage, id);
	}

	@Override
	protected String[] getDocumentIds() throws IOException {
		File corpusMetadataFile = new File(getSourceTromboneCorpusDirectory(), "metadata.xml");
		FlexibleParameters params = FlexibleParameters.loadFlexibleParameters(corpusMetadataFile);
		return params.getParameterValues("documentIds");
	}
		
	protected String getStoredId(String[] ids) throws IOException {
		File oldStoredDocumentSourcesDirectory = new File(this.getMigrationSourceDirectory(), "stored_document_sources");
		// this part somewhat reproduces {@link DocumentStorer}, but custom setting metadata along the way
		// we use the rawbytes instead of the tokens to simplify and to allow the new code to do better extraction
		List<String> storedIds = new ArrayList<String>();
		for (String id : ids) {
			File documentDirectory = new File(oldStoredDocumentSourcesDirectory, id);
			StoredDocumentSource storedDocumentSource = getStoredDocumentSource(documentDirectory);
			storedIds.add(storedDocumentSource.getId());
		}
		return storage.storeStrings(storedIds);
		
	}
	
	protected StoredDocumentSource getStoredDocumentSource(File directory) throws IOException {
		File documentDirectory = new File(directory, id);
		InputSource inputSource = new FileInputSource(new File(documentDirectory, "raw_bytes.gz"));
		DocumentMetadata documentMetadata = inputSource.getMetadata();
		documentMetadata.setSource(Source.STREAM); // claim that this is a stream since it shouldn't be recoverable
		documentMetadata.setLocation("Trombone 4.0 Migration");
		documentMetadata.setTitle(""); // we don't want the default "rawbytes" title for a file
		return storage.getStoredDocumentSourceStorage().getStoredDocumentSource(inputSource);
	}
	
	protected FlexibleParameters getParameters() {
		return new FlexibleParameters();
	}
	
}

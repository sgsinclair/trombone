/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Migrator;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.XStream;

/**
 * @author sgs
 *
 */
public class FileTrombone3_0Migrator implements Migrator {

	// this is package level for unit testing
	static String DEFAULT_TROMBOME_DIRECTORY_NAME = "trombone3_0";
	
	private FileStorage storage;
	private String id;
	
	/**
	 * 
	 */
	public FileTrombone3_0Migrator(FileStorage storage, String id) {
		this.storage = storage;
		this.id = id;
	}

	@Override
	public String getMigratedCorpusId() throws IOException {
		File oldCorpusDirectory = getMigrationSourceCorpusDirectory(storage, id);
		FlexibleParameters corpusMetadata = getOldFlexibleParameters(new File(oldCorpusDirectory, "corpus-metadata.xml"));
		String[] ids = corpusMetadata.getParameterValues("documentIds");

		// this part somewhat reproduces {@link DocumentStorer}, but custom setting metadata along the way
		StoredDocumentSourceStorage storedDocumentStorage = storage.getStoredDocumentSourceStorage();
		List<String> storedIds = new ArrayList<String>();
		for (String id : ids) {
			File documentDirectory = new File(oldCorpusDirectory, id);
			InputSource inputSource = new FileInputSource(new File(documentDirectory, "rawbytes"));
			DocumentMetadata documentMetadata = inputSource.getMetadata();
			documentMetadata.setSource(Source.STREAM); // claim that this is a stream
			documentMetadata.setLocation("Trombone 3.0 Migration");
			documentMetadata.setTitle("");
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
		return getMigrationSourceCorpusDirectory(storage, id).exists();
	}
	
	private static File getMigrationSourceCorpusDirectory(FileStorage storage, String id) {
		return new File(getMigrationSourceDirectory(storage), id);
	}
	
	private static File getMigrationSourceDirectory(FileStorage storage) {
		return new File(storage.storageLocation.getParentFile(), DEFAULT_TROMBOME_DIRECTORY_NAME);
	}
	
	private FlexibleParameters getOldFlexibleParameters(File file) {
		final XStream xstream = new XStream();
		ca.hermeneuti.trombone.util.FlexibleParameters oldFlexibleParameters = (ca.hermeneuti.trombone.util.FlexibleParameters) xstream.fromXML(file);
		FlexibleParameters params = new FlexibleParameters();
		for (String key : oldFlexibleParameters.getKeys()) {
			params.setParameter(key, oldFlexibleParameters.getParameterValues(key));
		}
		return params;
	}

}

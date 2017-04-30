/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.DocumentMetadata.ParentType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.Storage.Location;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.XStream;

import ca.hermeneuti.trombone.results.filtering.stoplist.StopList;

/**
 * This is the migration utility for Trombone 3.0.
 * @author Stéfan Sinclair
 */
@SuppressWarnings("deprecation")
class FileTrombone3_0Migrator extends AbstractFileMigrator {

	/**
	 * 
	 */
	FileTrombone3_0Migrator(FileStorage storage, String id) {
		super(storage, id);
	}

	@Override
	protected String transferDocuments() throws IOException {
		
		// we can do this even for subsequent migrators since stoplist directory won't exist
		File oldCorpusDirectory = getSourceTromboneCorpusDirectory();
		File stoplistDirectory = new File(oldCorpusDirectory, "stoplist");
		if (stoplistDirectory.exists() && stoplistDirectory.isDirectory()) {
			for (File stoplistFile : stoplistDirectory.listFiles()) {
				if (stoplistFile.isFile()) {
					if (!storage.isStored(stoplistFile.getName(), Storage.Location.object)) { // ensure we don't overwrite
						InputStream fis = null;
						try {
							fis = new FileInputStream(stoplistFile);
							ObjectInputStream ois = new ObjectInputStream(fis);
							StopList stoplist;
							try {
								stoplist = (StopList) ois.readObject();
							} catch (ClassNotFoundException e) {
								throw new IOException("Unable to read stoplist file: "+stoplistFile.getAbsolutePath(), e);
							}
							storage.storeStrings(stoplist.getKeywords(), stoplistFile.getName(), Storage.Location.object);
						}
						finally {
							if (fis!=null) {
								fis.close();
							}
						}
					}
				}
			}
		}

		return super.transferDocuments();
	}
	
	@Override
	protected String[] getDocumentIds() throws IOException {
		// read in the list of documents for the older corpus metadata file
		FlexibleParameters corpusMetadata = getOldFlexibleParameters(new File(getSourceTromboneCorpusDirectory(), "corpus-metadata.xml"));
		return corpusMetadata.getParameterValues("documentIds");
	}

	@Override
	protected String getStoredDocumentsId(String[] ids) throws IOException {
		File oldCorpusDirectory = getSourceTromboneDocumentsDirectory();
		List<String> storedIds = new ArrayList<String>();
		for (String id : ids) {
			File documentDirectory = new File(oldCorpusDirectory, id);
			StoredDocumentSource storedDocumentSource = getStoredDocumentSource(documentDirectory);
			storedIds.add(storedDocumentSource.getId());
		}
		return storage.storeStrings(storedIds, Storage.Location.object);
	}
	
	protected StoredDocumentSource getStoredDocumentSource(File documentDirectory) throws IOException {
		InputSource inputSource = getInputSource(documentDirectory);
		return storage.getStoredDocumentSourceStorage().getStoredDocumentSource(inputSource);
		
	}
	
	protected InputSource getInputSource(File documentDirectory) throws IOException {
		InputSource inputSource =  new FileInputSource(new File(documentDirectory, "rawbytes"));
		updateInputSource(documentDirectory, inputSource);
		return inputSource;
	}
	
	protected void updateInputSource(File documentDirectory, InputSource inputSource) throws IOException {
		DocumentMetadata newMetadata = inputSource.getMetadata();
		DocumentMetadata sourceMetadata = getSourceDocumentMetadata(documentDirectory);
		DocumentMetadata parentSourceMetadata = sourceMetadata.asParent("", ParentType.MIGRATION);
		parentSourceMetadata.setExtra("migratedFrom", getSourceTromboneDirectoryName());
		FlexibleParameters parentSourceParams = parentSourceMetadata.getFlexibleParameters();
		for (String key : parentSourceParams.getKeys()) {
			newMetadata.setExtras(key, parentSourceParams.getParameterValues(key));
		}
		newMetadata.setSource(Source.STREAM); // claim that this is a stream since it shouldn't be recoverable
		newMetadata.setLocation(inputSource.getMetadata().getLocation());
		// try to recover author and title, especially if they were extracted with XPath queries that we no longer have access to
		newMetadata.setTitle(sourceMetadata.getTitle().trim().isEmpty() ? "" : sourceMetadata.getTitle());
		newMetadata.setAuthor(sourceMetadata.getAuthor().trim().isEmpty() ? "" : sourceMetadata.getAuthor()); // don't use default of "rawbytes"
	}
	
	protected DocumentMetadata getSourceDocumentMetadata(File documentDirectory) throws IOException {
		File file = new File(documentDirectory, "document-metadata.xml");
		FlexibleParameters params = getOldFlexibleParameters(file);
		return new DocumentMetadata(params);
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

	@Override
	protected File getSourceTromboneCorpusDirectory() {
		return new File(getSourceTromboneDirectory(), id);
	}

	protected File getSourceTromboneDocumentsDirectory() {
		return getSourceTromboneCorpusDirectory();
	}
	
	@Override
	protected FlexibleParameters getCorpusCreationParameters() throws IOException {
		FlexibleParameters parameters = new FlexibleParameters();
		parameters.setParameter("migrated_source_version", getSourceTromboneDirectoryName());
		return parameters;
	}

	@Override
	protected String getSourceTromboneDirectoryName() {
		assert(FileTrombone3_0Migrator.class.isInstance(this));
		return "trombone3_0";
	}

	@Override
	public File getStoredObjectFile() {
		// this doesn't happen in Trombone 3, so this is always null
		return null;
	}
	
}

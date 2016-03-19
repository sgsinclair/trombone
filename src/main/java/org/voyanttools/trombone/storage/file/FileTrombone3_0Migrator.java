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
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.XStream;

import ca.hermeneuti.trombone.results.filtering.stoplist.StopList;

/**
 * This is the migration utility for Trombone 3.0.
 * @author Stéfan Sinclair
 */
public class FileTrombone3_0Migrator extends AbstractFileMigrator {

	// this is package level for unit testing
	static String DEFAULT_TROMBOME_DIRECTORY_NAME = "trombone3_0";
	
	/**
	 * 
	 */
	public FileTrombone3_0Migrator(FileStorage storage, String id) {
		super(storage, id);
	}

	@Override
	protected String transferDocuments() throws IOException {
		super.transferDocuments();
		File oldCorpusDirectory = getSourceTromboneCorpusDirectory();
		File stoplistDirectory = new File(oldCorpusDirectory, "stoplist");
		if (stoplistDirectory.exists() && stoplistDirectory.isDirectory()) {
			for (File stoplistFile : stoplistDirectory.listFiles()) {
				if (stoplistFile.isFile()) {
					if (!storage.isStored(stoplistFile.getName())) { // ensure we don't overwrite
						InputStream fis = new FileInputStream(stoplistFile);
						ObjectInputStream ois = new ObjectInputStream(fis);
						StopList stoplist;
						try {
							stoplist = (StopList) ois.readObject();
						} catch (ClassNotFoundException e) {
							throw new IOException("Unable to read stoplist file: "+stoplistFile.getAbsolutePath(), e);
						}
						storage.storeStrings(stoplist.getKeywords(), stoplistFile.getName());
					}
				}
			}
		}

		// look for and transfer stopwords
		
		String[] ids = getDocumentIds();
		
		return getStoredDocumentsId(ids);
		
	}
	
	@Override
	protected String[] getDocumentIds() throws IOException {
		// read in the list of documents for the older corpus metadata file
		File oldCorpusDirectory = getSourceTromboneCorpusDirectory();
		FlexibleParameters corpusMetadata = getOldFlexibleParameters(new File(oldCorpusDirectory, "corpus-metadata.xml"));
		return corpusMetadata.getParameterValues("documentIds");
	}

	@Override
	protected String getStoredDocumentsId(String[] ids) throws IOException {
		File oldCorpusDirectory = getSourceTromboneCorpusDirectory();
		StoredDocumentSourceStorage storedDocumentStorage = storage.getStoredDocumentSourceStorage();
		List<String> storedIds = new ArrayList<String>();
		for (String id : ids) {
			File documentDirectory = new File(oldCorpusDirectory, id);
			InputSource inputSource = new FileInputSource(new File(documentDirectory, "rawbytes"));
			
			// we want to copy old metadata information as "parent"
			FlexibleParameters oldDocParams = getOldFlexibleParameters(new File(documentDirectory, "document-metadata.xml"));
			DocumentMetadata oldDocMetadata = new DocumentMetadata(oldDocParams);
			DocumentMetadata oldDocParentMetadata = oldDocMetadata.asParent(inputSource.getUniqueId(), ParentType.MIGRATION);
			oldDocParams = oldDocParentMetadata.getFlexibleParameters();
			FlexibleParameters newDocumentParams = inputSource.getMetadata().getFlexibleParameters();
			
			// now copy in the new values
			for (String key : oldDocParams.getKeys()) {
				if (key.equals("id")==false) {
					newDocumentParams.setParameter(key.replace("parent_", "parent_extra."), oldDocParams.getParameterValues(key));
				}
			}
			
			// create a new metadata and set certain additional fields
			DocumentMetadata documentMetadata = new DocumentMetadata(newDocumentParams);
			documentMetadata.setSource(Source.STREAM); // claim that this is a stream since it shouldn't be recoverable
			documentMetadata.setLocation(inputSource.getMetadata().getLocation());
			// try to recover author and title, especially if they were extracted with XPath queries that we no longer have access to
			documentMetadata.setTitle(oldDocMetadata.getTitle().trim().isEmpty() ? "" : oldDocMetadata.getTitle());
			documentMetadata.setAuthor(oldDocMetadata.getAuthor().trim().isEmpty() ? "" : oldDocMetadata.getAuthor()); // don't use default of "rawbytes"
			documentMetadata.setExtra("migration", getSourceTromboneDirectoryName());
			
			StoredDocumentSource storedDocumentSource = storedDocumentStorage.getStoredDocumentSource(inputSource);
			storedIds.add(storedDocumentSource.getId());
		}
		return storage.storeStrings(storedIds);
	}

	@Override
	protected File getSourceTromboneCorpusDirectory() {
		return new File(this.getMigrationSourceDirectory(), id);
	}
	
	@Override
	protected String getSourceTromboneDirectoryName() {
		return DEFAULT_TROMBOME_DIRECTORY_NAME;
	}

	@Override
	protected FlexibleParameters getParameters() {
		return new FlexibleParameters();
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

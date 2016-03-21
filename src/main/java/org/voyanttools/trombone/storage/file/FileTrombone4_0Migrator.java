/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * Trombone 4.0 is completely different from Trombone 3.0, especially in that it
 * uses Lucene for storage (though the Lucene index isn't actually used during migration).
 * 
 * @author Stéfan Sinclair
 */
class FileTrombone4_0Migrator extends FileTrombone3_0Migrator {
	
	
	FileTrombone4_0Migrator(FileStorage storage, String id) {
		super(storage, id);
	}
	
	@Override
	protected String transferDocuments() throws IOException {
		
		String[] ids = getDocumentIds();
		
		return getStoredDocumentsId(ids);
		
	}


	@Override
	protected String[] getDocumentIds() throws IOException {
		File file = new File(getSourceTromboneCorpusDirectory(), "metadata.xml");
		FlexibleParameters params = getFromPropertiesFile(file);
		return params.getParameterValue("documentIds").split(",");
	}
	
	@Override
	protected File getSourceTromboneCorpusDirectory() {
		return new File(new File(getSourceTromboneDirectory(), "corpora"), id);
	}
	
	@Override
	protected File getSourceTromboneDocumentsDirectory() {
		return new File(getSourceTromboneDirectory(), "stored_document_sources");
	}
	
	@Override
	protected InputSource getInputSource(File documentDirectory) throws IOException {
		InputSource inputSource =  new FileInputSource(new File(documentDirectory, "raw_bytes.gz"));
		updateInputSource(documentDirectory, inputSource);
		return inputSource;
	}

	@Override
	protected DocumentMetadata getSourceDocumentMetadata(File documentDirectory) throws IOException {
		File file = new File(documentDirectory, "metadata.xml");
		FlexibleParameters parameters = getFromPropertiesFile(file);
		return new DocumentMetadata(parameters);
	}
	
	@Override
	protected String getSourceTromboneDirectoryName() {
		assert(FileTrombone4_0Migrator.class.isInstance(this));
		return "trombone4_0";
	}
		
	@Override
	public File getStoredObjectFile() {
		Path path = Paths.get(this.getSourceTromboneDirectory().getAbsolutePath(), "object-storage", id);
		return Files.exists(path) ? path.toFile() : null;
	}
	
	private FlexibleParameters getFromPropertiesFile(File file) throws IOException {
		Properties properties = new Properties();
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			properties.loadFromXML(is);
		}
		finally {
			if (is!=null) {
				is.close();
			}
		}
		return new FlexibleParameters(properties);
	}
}

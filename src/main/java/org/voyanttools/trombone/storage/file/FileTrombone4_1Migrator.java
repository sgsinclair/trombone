/**
 * 
 */
package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.InputSourcesBuilder;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.tool.corpus.CorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * Trombone 4.1 is similar to 4.0, with the notable exception that metadata is stored
 * using {@link FlexibleParameters} instead of {@link Properties}, especially to allow multiple
 * values. The other important distinction is that initial query parameters are stored with the
 * corpus, so if the original parent source(s) can be found, we'll use that instead.
 * 
 * @author Stéfan Sinclair
 */
public class FileTrombone4_1Migrator extends FileTrombone4_0Migrator {
	
	/**
	 * @param storage
	 * @param id
	 */
	public FileTrombone4_1Migrator(FileStorage storage, String id) {
		super(storage, id);
	}

	@Override
	public String getMigratedCorpusId() throws IOException {
		
		if (!corpusExists()) {return null;}
		
		String newid;
		
		
		// try from parameters
		newid = getMigratedCorpusIdFromParameters();
		if (newid!=null) {return newid;}

		// try from document parents
		// this should be tried first but there was a bug in Trombone 4.2 and lower where extraction wasn't setting the parent properly
		newid = getMigratedCorpusIdFromDocumentParents();
		if (newid!=null) {return newid;}

		// if that didn't work, try the old way
		return super.getMigratedCorpusId();
	}
	
	protected String getMigratedCorpusIdFromDocumentParents() throws IOException {
		String[] ids = getDocumentIds();
		Set<String> idsSet = new LinkedHashSet<String>();
		for (String id : ids) {
			String parentId = getParentId(id);
			if (parentId==null) {return null;} // couldn't find a parent
			
			// make sure we copy the old source to the new one if need be
			File target = ((FileStoredDocumentSourceStorage) storage.getStoredDocumentSourceStorage()).getDocumentSourceDirectory(parentId);
			if (target.exists()==false) {
				File source = new File(getSourceTromboneDocumentsDirectory(), parentId);
				FileUtils.copyDirectory(source, target, new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						// we don't want the expanded sources since we don't have them in the new trombone directory
				        return !pathname.getName().equals("multiple_expanded_stored_document_source_ids.txt");
					}
				});
			}
			idsSet.add(parentId);
		}
		if (idsSet.size()>0) {	
			// pick up corpus creation from expansion, with the original parameters
			String newid = storage.storeStrings(idsSet);
			FlexibleParameters parameters = this.getCorpusCreationParameters();
			parameters.setParameter("nextCorpusCreatorStep", "expand");
			parameters.setParameter("storedId", newid);
			RealCorpusCreator creator = new RealCorpusCreator(storage, parameters);
			creator.run();
			newid = creator.getStoredId();
			storage.getCorpusStorage().addAlias(this.id, newid);
			return newid;
		}
		return null;
	}
	
	protected String getParentId(String id) throws IOException {
		File file = new File(getSourceTromboneDocumentsDirectory(), id);
		DocumentMetadata metadata = getSourceDocumentMetadata(file);
		String parentId = metadata.getParentId();
		if (parentId.isEmpty()) {return id;} // no more parents, this should be the top-level
		if (new File(getSourceTromboneDocumentsDirectory(), parentId).exists()) {return getParentId(parentId);}
		return null; // parent is defined but can't be found, so we bail
	}

	protected String getMigratedCorpusIdFromParameters() throws IOException {
		
		FlexibleParameters corpusCreationParams = getCorpusCreationParameters();
		
		// create input sources (nothing should be stored at this point)
		InputSourcesBuilder inputSourcesBulider = new InputSourcesBuilder(corpusCreationParams);
		List<InputSource> inputSources = inputSourcesBulider.getInputSources();
		
		// something didn't work, parameters may not have contained real input (like when we have corpus creation step)
		if (inputSources.isEmpty()) {return null;}
		
		// go through and double check that everything is still available
		int count = 0;
		for (InputSource inputSource : inputSources) {
			// we can check if this is a file input source and bail if it's not there
			if (inputSource instanceof FileInputSource && ((FileInputSource) inputSource).getFile().exists()==false) {break;}
			InputStream is = null;
			try {
				is = inputSource.getInputStream();
				if (is.available()>0) {
					count++;
				} else {
					System.err.println("Nothing available from "+inputSource);
				}
			}
			finally {
				if (is!=null) {
					is.close();
				}
			}
		}
		
		// load from params if we everything seems available
		if (count==inputSources.size()) {
			CorpusCreator corpusCreator = new CorpusCreator(storage, corpusCreationParams);
			corpusCreator.run();
			String newid = corpusCreator.getStoredId();
			storage.getCorpusStorage().addAlias(this.id, newid);
			return newid;
		}
		
		// not able to return a string so return null
		return null;
		
	}

	
	@Override
	protected String[] getDocumentIds() throws IOException {
		File file = new File(getSourceTromboneCorpusDirectory(), "metadata.xml");
		FlexibleParameters params = FlexibleParameters.loadFlexibleParameters(file);
		return params.getParameterValues("documentIds");
	}

	@Override
	protected DocumentMetadata getSourceDocumentMetadata(File documentDirectory) throws IOException {
		File file = new File(documentDirectory, "metadata.xml");
		FlexibleParameters parameters = FlexibleParameters.loadFlexibleParameters(file);
		return new DocumentMetadata(parameters);
	}
	
	@Override
	protected String getSourceTromboneDirectoryName() {
		assert(FileTrombone4_1Migrator.class.isInstance(this));
		return "trombone4_1";
	}
	
	@Override
	protected FlexibleParameters getCorpusCreationParameters() throws IOException {
		File file = new File(getSourceTromboneCorpusDirectory(), "parameters.xml");
		FlexibleParameters parameters = FlexibleParameters.loadFlexibleParameters(file);
		parameters.setParameter("migrated_source_version", getSourceTromboneDirectoryName());
		return parameters;
	}


}

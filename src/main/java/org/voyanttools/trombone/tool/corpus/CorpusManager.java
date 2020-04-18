/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.voyanttools.trombone.input.source.InputSourcesBuilder;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusAccess;
import org.voyanttools.trombone.model.CorpusAccessException;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileMigrator;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
@XStreamAlias("corpus")
public class CorpusManager extends AbstractTool {
	
	private String id = "";
	
	private String access = null;
	
	@XStreamOmitField
	private Corpus corpus = null;

	public CorpusManager(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.utils.RunnableTool#run()
	 */
	@Override
	public void run() throws IOException {
		
		// try to load a corpus
		if (parameters.containsKey("corpus")) {
			String corpusId = parameters.getParameterValue("corpus");
			
			// lookup for an alias
			/*
			// we don't use aliases like this anymore, they're just symbolic links now
			if (corpusId.length()<32 && CorpusAliasDB.exists(storage)) { // MD5 should be 32 characters
				CorpusAliasDB corpusAliasDB = new CorpusAliasDB(storage, true);
				String id = corpusAliasDB.get(corpusId);
				if (id!=null && id.isEmpty()==false) {
					corpusId = id;
				}
				corpusAliasDB.close();
			}
			*/
			
			// check if corpus exists and return it if so
			if (storage.getCorpusStorage().corpusExists(corpusId)) {
				this.id = corpusId;
				checkActions();
				return;
			}
			
			// check of a previous format exists and return it if so
			FileMigrator migrator = storage.getMigrator(corpusId);
			if (migrator!=null) {
				this.id = migrator.getMigratedCorpusId();
				checkActions();
				return;
			}
			
			// if we've gotten here and we still can't find a corpus, then we hope there are parameters to recreate it
			if (InputSourcesBuilder.hasParameterSources(parameters)) {
				RealCorpusCreator realCorpusCreator = new RealCorpusCreator(storage, parameters);
				realCorpusCreator.run();
				this.id = realCorpusCreator.getStoredId();
				// alias for next time
				storage.getCorpusStorage().addAlias(corpusId, this.id);
				checkActions();
				return;
			}
			
			if (parameters.getParameterBooleanValue("failQuietly")) {
				return;
			}

			// otherwise we have a problem Houston
			else {
				throw new IllegalArgumentException("A corpus was specified but does not exist, could not be migrated and could not be recreated: "+corpusId);
			}
			
		}

		RealCorpusCreator realCorpusCreator = new RealCorpusCreator(storage, parameters);
		realCorpusCreator.run();
		this.id = realCorpusCreator.getStoredId();
		checkActions();

	}
	
	public static Corpus getCorpus(Storage storage, FlexibleParameters parameters) throws IOException {
		CorpusManager corpusManager = new CorpusManager(storage, parameters);
		corpusManager.run();
		return corpusManager.getCorpus();
	}
	
	String getId() {
		return id;
	}
	
	private void checkActions() throws IOException {
		
		corpus = getCorpus();
		if (parameters.getParameterBooleanValue("getAccess")) {
			access = corpus.getValidatedCorpusAccess(parameters).name();
		}
		else if (parameters.getParameterBooleanValue("removeDocuments") || parameters.getParameterBooleanValue("keepDocuments") || parameters.getParameterBooleanValue("reorderDocuments") || parameters.getParameterBooleanValue("addDocuments")) {
			
			// make sure we have permissions to do this
			CorpusAccess corpusAccess = corpus.getValidatedCorpusAccess(parameters);
			if (corpusAccess==CorpusAccess.NONCONSUMPTIVE) {
				throw new CorpusAccessException("You aren't permitted to edit this corpus.");
			}

			List<String> ids = new ArrayList<String>();
			
			// add IDs
			for (String docId : parameters.getParameterValues("docId")) {
				if (docId.isEmpty()==false) {
					ids.add(docId);
				}
			}
			
			// add indices
			// check first if we have real values
			String[] docIndices = parameters.getParameterValues("docIndex");
			if (docIndices.length>0 && docIndices[0].isEmpty()==false) {
				for (int docIndex : parameters.getParameterIntValues("docIndex")) {
					if (docIndex<corpus.size()) {
						ids.add(corpus.getDocument(docIndex).getId());
					}
				}
			}
			
			List<String> keepers = new ArrayList<String>();
			
			if (parameters.getParameterBooleanValue("removeDocuments")) {
				Set<String> set = new HashSet<String>(ids);
				for (String id : corpus.getDocumentIds()) {
					if (set.contains(id)==false) {
						keepers.add(id);
					}
				}
			}
			
			if (parameters.getParameterBooleanValue("keepDocuments")) {
				Set<String> set = new HashSet<String>(ids);
				for (String id : ids) {
					if (set.contains(id)) {
						keepers.add(id);
					}
				}
			}
			
			if (parameters.getParameterBooleanValue("reorderDocuments")) {
				Set<String> set = new HashSet<String>(corpus.getDocumentIds());
				for (String id : ids) {
					if (set.contains(id)) {
						keepers.add(id);
					}
				}
			}
			
			if (parameters.getParameterBooleanValue("addDocuments")) {
				keepers.addAll(corpus.getDocumentIds()); // add existing
				
				// prepare other documents
				RealCorpusCreator realCorpusCreator = new RealCorpusCreator(storage, parameters);
				realCorpusCreator.run(4); // make sure to create corpus
				realCorpusCreator.getStoredId();
				String id = realCorpusCreator.getStoredId();
				List<String> documentIds = storage.retrieveStrings(id, Storage.Location.object);
				keepers.addAll(documentIds);
			}
			
			String corpusId = storage.storeStrings(keepers, Storage.Location.object);
			FlexibleParameters params = new FlexibleParameters(new String[]{"storedId="+corpusId,"nextCorpusCreatorStep=index"}); // re-index in case we have per-corpus index
			RealCorpusCreator realCorpusCreator = new RealCorpusCreator(storage, params);
			realCorpusCreator.run(); // make sure to create corpus
			this.id = realCorpusCreator.getStoredId();
			this.corpus = null; // reset the corpus
		}

		if (parameters.containsKey("addAlias") && parameters.getParameterValue("addAlias").isEmpty()==false) {
			
			// make sure we have permissions to do this
			CorpusAccess corpusAccess = corpus.getValidatedCorpusAccess(parameters);
			if (corpusAccess==CorpusAccess.NONCONSUMPTIVE) {
				throw new CorpusAccessException("You aren't permitted to set an alias to this corpus.");
			}
			
			String alias = parameters.getParameterValue("addAlias").trim();
			if (storage.getCorpusStorage().corpusExists(alias)) {
				if (storage.getCorpusStorage().getCorpus(alias).equals(storage.getCorpusStorage().getCorpus(this.id))==false) {
					throw new IllegalArgumentException("Attempt to create an alias for an alias that's already used: "+alias);
				}
				
			} else {
				storage.getCorpusStorage().addAlias(alias, this.id);
			}
		}

	}

	Corpus getCorpus() throws IOException {
		if (this.corpus==null && this.id!=null) {
			this.corpus = this.storage.getCorpusStorage().getCorpus(this.id);
		}
		return this.corpus;
	}

}

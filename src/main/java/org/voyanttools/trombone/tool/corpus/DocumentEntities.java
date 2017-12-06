/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.DocumentEntity;
import org.voyanttools.trombone.model.EntityType;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.nlp.NlpAnnotator;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("documentEntities")
public class DocumentEntities extends AbstractCorpusTool {
	
	private List<DocumentEntity> entities = new ArrayList<DocumentEntity>();

	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentEntities(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}
	
	public float getVersion() {
		return super.getVersion()+1;
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.corpus.AbstractCorpusTool#run(org.voyanttools.trombone.lucene.CorpusMapper)
	 */
	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		boolean verbose = parameters.getParameterBooleanValue("verbose");
		List<String> ids = getCorpusStoredDocumentIdsFromParameters(corpusMapper.getCorpus());
		for (String docId : ids) {
			// only check for "withDistributions" though this will actually shift to this class
			// TODO: offset to token mapping should happen here instead of in the annotator
			String id = "cached-document-entities-"+String.valueOf(this.getVersion())+DigestUtils.md5Hex(docId+parameters.getParameterValue("withDistributions","")+parameters.getParameterValue("entityType",""));
			List<DocumentEntity> entitiesList = new ArrayList<DocumentEntity>();
			if (storage.isStored(id, Storage.Location.object)) {
				try {
					entitiesList = (List<DocumentEntity>) storage.retrieve(id, Storage.Location.object);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
			else {
				IndexedDocument indexedDocument = corpusMapper.getCorpus().getDocument(docId);
				String lang = indexedDocument.getMetadata().getLanguageCode();
				Set<EntityType> types =  new HashSet<EntityType>();
				if (parameters.containsKey("entityType")) {
					for (String entityType : parameters.getParameterValues("entityType")) {
						EntityType type = EntityType.getForgivingly(entityType);
						if (type!=EntityType.unknnown) {
							types.add(type);
						}
					}
				}
					 
				
				if (lang.equals("en")) {
					NlpAnnotator nlpAnnotator = storage.getNlpAnnotatorFactory().getNlpAnnotator(lang);
					// get all types that are recognized (though not ordinals and numbers)
					entitiesList = nlpAnnotator.getEntities(corpusMapper, indexedDocument, types, parameters);
					storage.store(entitiesList, id, Storage.Location.object);
				}
			}
			
			// build a simple list of types to keep
			Set<EntityType> types = new HashSet<EntityType>();
			for (String type : parameters.getParameterValues("type")) {
				for (String t : type.split(",\\s*")) {
					EntityType et = EntityType.getForgivingly(t);
					if (et!=EntityType.unknnown) {
						types.add(et);
					}
				}
			}

			for (DocumentEntity entity : entitiesList) {
				if (types.isEmpty() || types.contains(entity.getType())) {
					entities.add(entity);
				}
			}
			
			if (verbose) {
				System.out.println("document: "+docId+" has "+entitiesList.size()+" entities ("+entities.size()+" total)");
			}
		}
	}
	
	List<DocumentEntity> getDocumentEntities() {
		return entities;
	}

}

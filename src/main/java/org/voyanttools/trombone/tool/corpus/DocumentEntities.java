/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.DocumentEntity;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.EntityType;
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
	
	public int getVersion() {
		return super.getVersion();
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.corpus.AbstractCorpusTool#run(org.voyanttools.trombone.lucene.CorpusMapper)
	 */
	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
				
		for (IndexedDocument indexedDocument : corpusMapper.getCorpus()) {
			// only check for "withDistributions" though this will actually shift to this class
			// TODO: offset to token mapping should happen here instead of in the annotator
			String id = "cached-document-entities-"+String.valueOf(this.getVersion())+DigestUtils.md5Hex(indexedDocument.getId()+parameters.getParameterValue("withDistributions",""));
			List<DocumentEntity> entitiesList = new ArrayList<DocumentEntity>();
			if (storage.isStored(id)) {
				try {
					entitiesList = (List<DocumentEntity>) storage.retrieve(id);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
			else {
				String lang = indexedDocument.getMetadata().getLanguageCode();
				if (lang.equals("en")) {
					NlpAnnotator nlpAnnotator = storage.getNlpAnnotator(lang);
					// get all types that are recognized (though not ordinals and numbers)
					entitiesList = nlpAnnotator.getEntities(corpusMapper, indexedDocument, new HashSet<EntityType>(), parameters);
					storage.store(entitiesList, id);
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
		}
	}
	
	List<DocumentEntity> getDocumentEntities() {
		return entities;
	}

}

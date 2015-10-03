/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.corpus.AbstractCorpusTool#run(org.voyanttools.trombone.lucene.CorpusMapper)
	 */
	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		
		// build a simple list of types
		Collection<EntityType> types = new HashSet<EntityType>();
		for (String type : parameters.getParameterValues("type")) {
			for (String t : type.split(",\\s*")) {
				EntityType et = EntityType.getForgivingly(t);
				if (et!=EntityType.unknnown) {
					types.add(et);
				}
			}
		}
		
		for (IndexedDocument indexedDocument : corpusMapper.getCorpus()) {
			String id = "document-entities-"+String.valueOf(this.getVersion())+DigestUtils.md5Hex(indexedDocument.getId()+parameters.toString());
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
					entitiesList = nlpAnnotator.getEntities(corpusMapper, indexedDocument, types, parameters);
					storage.store(entitiesList, id);
				}
			}
			entities.addAll(entitiesList);
		}
	}
	
	List<DocumentEntity> getDocumentEntities() {
		return entities;
	}

}

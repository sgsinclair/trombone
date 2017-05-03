/**
 * 
 */
package org.voyanttools.trombone.nlp;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.DocumentEntity;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.EntityType;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public interface NlpAnnotator {

	List<DocumentEntity> getEntities(CorpusMapper corpusMapper,
			IndexedDocument indexedDocument, Collection<EntityType> types, FlexibleParameters parameters)
			throws IOException;
	
}

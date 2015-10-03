/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.CorpusEntity;
import org.voyanttools.trombone.model.DocumentEntity;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("corpusEntities")
public class CorpusEntities extends AbstractCorpusTool {
	
	private List<CorpusEntity> corpusEntities = new ArrayList<CorpusEntity>();

	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusEntities(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.corpus.AbstractCorpusTool#run(org.voyanttools.trombone.lucene.CorpusMapper)
	 */
	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		DocumentEntities documentEntities = new DocumentEntities(storage, parameters);
		documentEntities.run(corpusMapper);
		corpusEntities = getCorpusEntities(documentEntities.getDocumentEntities());
	}
	
	List<CorpusEntity> getCorpusEntities(List<DocumentEntity> documentEntities) {
		
		// build map based on terms and types
		Map<String, List<DocumentEntity>> map = new HashMap<String, List<DocumentEntity>>();
		for (DocumentEntity docEntity : documentEntities) {
			String key = docEntity.getTerm()+" -- "+docEntity.getType().name();
			if (!map.containsKey(key)) {map.put(key, new ArrayList<DocumentEntity>());}
			map.get(key).add(docEntity);
		}
		
		List<CorpusEntity> entities = new ArrayList<CorpusEntity>();
		for (List<DocumentEntity> docEntities : map.values()) {
			int rawFreq = 0;
			for (DocumentEntity entity : docEntities) {
				rawFreq += entity.getRawFreq();
			}
			DocumentEntity docEntity = docEntities.get(0);
			entities.add(new CorpusEntity(docEntity.getTerm(), docEntity.getType(), rawFreq, null));
		}
		
		return entities;
	}

}

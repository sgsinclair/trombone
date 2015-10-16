/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.CorpusEntity;
import org.voyanttools.trombone.model.DocumentEntity;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.FlexibleQueue;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
@XStreamAlias("corpusEntities")
public class CorpusEntities extends AbstractTerms {
	
	private List<CorpusEntity> corpusEntities = new ArrayList<CorpusEntity>();

	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusEntities(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}
	
	public int getVersion() {
		return super.getVersion()+1;
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
		
		CorpusEntity.Sort sort = CorpusEntity.Sort.getForgivingly(parameters);
		Comparator<CorpusEntity> compartor = CorpusEntity.getComparator(sort);
		FlexibleQueue<CorpusEntity> entities = new FlexibleQueue<CorpusEntity>(compartor, start+limit);
		for (List<DocumentEntity> docEntities : map.values()) {
			int rawFreq = 0;
			int inDocumentsCount = 0;
			for (DocumentEntity entity : docEntities) {
				rawFreq += entity.getRawFreq();
				if (entity.getRawFreq()>0) {inDocumentsCount++;}
			}
			DocumentEntity docEntity = docEntities.get(0);
			entities.offer(new CorpusEntity(docEntity.getTerm(), docEntity.getType(), rawFreq, inDocumentsCount, null));
		}
		
		return entities.getOrderedList(start);
	}

	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords,
			String[] queries) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

}

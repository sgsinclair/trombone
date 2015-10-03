/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
@XStreamAlias("entityCollocationsGraph")
public class EntityCollocationsGraph extends AbstractCorpusTool {
	
	private List<Edge> edges = new ArrayList<Edge>();
	private List<CorpusEntity> nodes = new ArrayList<CorpusEntity>();

	/**
	 * @param storage
	 * @param parameters
	 */
	public EntityCollocationsGraph(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.corpus.AbstractCorpusTool#run(org.voyanttools.trombone.lucene.CorpusMapper)
	 */
	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		DocumentEntities documentEntitiesTool = new DocumentEntities(storage, parameters);
		documentEntitiesTool.run(corpusMapper);
		List<DocumentEntity> documentEntities = documentEntitiesTool.getDocumentEntities();
		
		
		// organize by document and track counts
		Map<Integer, List<DocumentEntity>> entitesByDocumentMap = new HashMap<Integer, List<DocumentEntity>>();
		for (DocumentEntity entity : documentEntities) {
			int docIndex = entity.getDocIndex();
			if (!entitesByDocumentMap.containsKey(docIndex)) {
				entitesByDocumentMap.put(docIndex, new ArrayList<DocumentEntity>());
			}
			entitesByDocumentMap.get(docIndex).add(entity);
		}
		
		// create intra-document links
		Map<String, List<DocumentEntity[]>> termEntitiesMap = new HashMap<String, List<DocumentEntity[]>>();
		for (List<DocumentEntity> docEntities : entitesByDocumentMap.values()) {
			Collections.sort(docEntities);
			for (DocumentEntity outer : docEntities) {
				for (DocumentEntity inner : docEntities) {
					if (inner.compareTo(outer)<0) {
						String key = outer.getTerm()+" -- "+inner.getTerm();
						if (!termEntitiesMap.containsKey(key)) {
							termEntitiesMap.put(key, new ArrayList<DocumentEntity[]>());
						}
						termEntitiesMap.get(key).add(new DocumentEntity[]{outer,inner});
					}
					else {
						break;
					}
				}
			}
		}
		
		// create inter-document links
		CorpusEntities corpusEntitiesTool = new CorpusEntities(storage, parameters);
		nodes = corpusEntitiesTool.getCorpusEntities(documentEntities);
		Map<String, Integer> corpusEntitiesMap = new HashMap<String, Integer>();
		String separator = " -- ";
		int counter = 0;
		for (CorpusEntity corpusEntity : nodes) {
			corpusEntitiesMap.put(corpusEntity.getTerm()+separator+corpusEntity.getType().name(), counter++);
		}
		for (List<DocumentEntity[]> termEntitesList : termEntitiesMap.values()) {
			DocumentEntity[] docEntities = termEntitesList.get(0);
			String key1 = docEntities[0].getTerm()+separator+docEntities[0].getType().name();
			String key2 = docEntities[1].getTerm()+separator+docEntities[1].getType().name();
			if (corpusEntitiesMap.containsKey(key1) && corpusEntitiesMap.containsKey(key2)) {
				edges.add(new Edge(new int[]{corpusEntitiesMap.get(key1), corpusEntitiesMap.get(key2)}, termEntitesList.size()));
			}
		}
	}
	
	private static class Edge {
		private int[] nodes;
		private int count;
		private Edge(int[] nodes, int count) {
			this.nodes = nodes;
			this.count = count;
		}
	}

}

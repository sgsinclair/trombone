/**
 * 
 */
package org.voyanttools.trombone.nlp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.DocumentEntity;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.model.EntityType;
import org.voyanttools.trombone.util.FlexibleParameters;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
//import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NormalizedNamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * @author sgs
 *
 */
public class StanfordNlpAnnotator implements NlpAnnotator {
	StanfordCoreNLP pipeline;

	/**
	 * 
	 */
	StanfordNlpAnnotator(String languageCode) {
	    Properties props = new Properties();
	    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, entitymentions");
	    // props.setProperty("ner.verbose", "true");
	    if (languageCode.equals("fr")) {
	    	props.setProperty("props", "StanfordCoreNLP-french.properties");
	    }
	    pipeline = new StanfordCoreNLP(props);
	}

	@Override
	public List<DocumentEntity> getEntities(CorpusMapper corpusMapper, IndexedDocument indexedDocument, Collection<EntityType> types, FlexibleParameters parameters) throws IOException {
		
		List<CoreMap> entitiesMap = getEntities(indexedDocument.getDocumentString(), types);
		
		// organize by term-name so that we can group together
		Map<String, List<CoreMap>> stringEntitiesMap = new HashMap<String, List<CoreMap>>();
		for (CoreMap entity : entitiesMap) {
			String term = entity.get(TextAnnotation.class);
			EntityType type = EntityType.getForgivingly(entity.get(NamedEntityTagAnnotation.class));
			if (types.isEmpty()==false && types.contains(type)==false) {continue;}
			String key = term+" --" +type.name(); 
			if (!stringEntitiesMap.containsKey(key)) {
				stringEntitiesMap.put(key, new ArrayList<CoreMap>());
			}
			stringEntitiesMap.get(key).add(entity);
		}
		
		Map<Integer, Integer> offsetToTokenPositionMap = parameters.getParameterBooleanValue("withDistributions") ? getOffsetsToPositionsMap(corpusMapper, indexedDocument, entitiesMap) : null;
		
		List<DocumentEntity> entities = new ArrayList<DocumentEntity>();
		int corpusDocumentIndex = corpusMapper.getCorpus().getDocumentPosition(indexedDocument.getId());
		for (Map.Entry<String, List<CoreMap>> stringEntitiesMapEntry : stringEntitiesMap.entrySet()) {
			List<CoreMap> coreMaps = stringEntitiesMapEntry.getValue();
			List<Integer> positions = new ArrayList<Integer>();
			if (offsetToTokenPositionMap!=null) {
				for (CoreMap entity : coreMaps) {
					int startOffset = entity.get(CharacterOffsetBeginAnnotation.class);
					Integer position = offsetToTokenPositionMap.get(startOffset);
					if (position!=null) {positions.add(position);}
				}
			}
			CoreMap entity = coreMaps.get(0);
			String term = entity.get(TextAnnotation.class);
			String normalized = entity.get(NormalizedNamedEntityTagAnnotation.class);
			EntityType type = EntityType.getForgivingly(entity.get(NamedEntityTagAnnotation.class));
			DocumentEntity e = new DocumentEntity(corpusDocumentIndex, term, normalized, type, positions.isEmpty() ? coreMaps.size() : positions.size(),  positions.isEmpty() ? null : ArrayUtils.toPrimitive(positions.toArray(new Integer[0])), null);
			entities.add(e);
		}
		
		return entities;
	}
	
	private Map<Integer, Integer> getOffsetsToPositionsMap(CorpusMapper corpusMapper, IndexedDocument indexedDocument, List<CoreMap> entitiesMap) throws IOException {
		// go through and collect offsets to keep
		Set<Integer> offsets = new HashSet<Integer>();
		for (CoreMap entity : entitiesMap) {
			offsets.add(entity.get(CharacterOffsetBeginAnnotation.class));
		}
		
		// go through vector to collect tokens of interest
		Map<Integer, Integer> offsetToTokenPositionMap = new HashMap<Integer, Integer>();
		int luceneDoc = corpusMapper.getLuceneIdFromDocumentId(indexedDocument.getId());
		// TODO: check that we can assume that offsets align regardless of TokenType
		Terms terms = corpusMapper.getIndexReader().getTermVector(luceneDoc, TokenType.lexical.name()); 
		TermsEnum termsEnum = terms.iterator();
		while(true) {
			BytesRef term = termsEnum.next();
			if (term!=null) {
				PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.OFFSETS);
				if (postingsEnum!=null) {
					postingsEnum.nextDoc();
					for (int i=0, len = postingsEnum.freq(); i<len; i++) {
						int pos = postingsEnum.nextPosition();
						int offset = postingsEnum.startOffset();
						if (offsets.contains(offset)) {
							offsetToTokenPositionMap.put(offset, pos);
						}
					}
				}
			}
			else {break;}
		}
		return offsetToTokenPositionMap;
		
	}

	private List<CoreMap> getEntities(String text, Collection<EntityType> types) {
		
		List<CoreMap> sentences = getSentences(text);
		List<CoreMap> entities = new ArrayList<CoreMap>();
	    for(CoreMap sentence: sentences) {
	    	for (CoreMap entity : sentence.get(MentionsAnnotation.class)) {
	    		EntityType type = EntityType.getForgivingly(entity.get(NamedEntityTagAnnotation.class));
	    		if (type!=EntityType.unknnown && (types.isEmpty() || types.contains(type))) {
	    			entities.add(entity);
	    		}
	    	}
	    }
	    return entities;
	}
	
	public Annotation getAnnotated(String text) {
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		return document;
	}
	
	private List<CoreMap> getSentences(String text) {
		Annotation annotation = getAnnotated(text);
		return annotation.get(SentencesAnnotation.class);
	}
	
//	public static void main(String[] args) {
//		StanfordNlpAnnotator annotator = new StanfordNlpAnnotator("en");
//		annotator.getEntities("October 1, 2015. This is a test from yesterday in London, UK.");
//	}

}

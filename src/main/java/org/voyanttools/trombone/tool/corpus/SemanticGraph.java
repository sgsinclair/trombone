package org.voyanttools.trombone.tool.corpus;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.DocumentToken;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.FlexibleQueue;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.PointerType;
import net.sf.extjwnl.data.PointerUtils;
import net.sf.extjwnl.data.list.PointerTargetNodeList;
import net.sf.extjwnl.data.list.PointerTargetTree;
import net.sf.extjwnl.data.relationship.AsymmetricRelationship;
import net.sf.extjwnl.data.relationship.Relationship;
import net.sf.extjwnl.data.relationship.RelationshipFinder;
import net.sf.extjwnl.data.relationship.RelationshipList;
import net.sf.extjwnl.dictionary.Dictionary;

@XStreamAlias("semanticGraph")
@XStreamConverter(SemanticGraph.SemanticGraphConverter.class)
public class SemanticGraph extends AbstractCorpusTool {
	
	@XStreamOmitField
    private Dictionary dictionary;
	
	@XStreamOmitField
	private Keywords whiteList;
	
	private List<RelatedWords> wordsList = new ArrayList<RelatedWords>();
	
	public SemanticGraph(Storage storage, FlexibleParameters parameters) throws JWNLException {
		super(storage, parameters);
        dictionary = Dictionary.getDefaultResourceInstance();
		whiteList = new Keywords();
		if (parameters.getParameterValue("whiteList", "").isEmpty()==false) {
			try {
				whiteList.load(storage, parameters.getParameterValues("whiteList"));
			} catch (IOException e) {
				throw new IllegalArgumentException("Unable to load whitelist: "+StringUtils.join(parameters.getParameterValues("whiteList"), ","));
			}
		}
    }
	
	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		Set<IndexWord> forms = new HashSet<IndexWord>();
		Keywords stopwords = getStopwords(corpusMapper.getCorpus());
		if (parameters.containsKey("posLemmas")) {
			for (String posLemma : parameters.getParameterValues("posLemmas")) {
				for (String pl : posLemma.split(",")) {
					if (pl.contains("/")) {
						String[] parts = pl.trim().split("/");
						if (whiteList.isEmpty()==false && whiteList.isKeyword(parts[0])==false) {continue;}
						if (stopwords.isKeyword(parts[0])) {continue;}
						try {
							
							POS pos = POS.getPOSForLabel(parts[1]);
							IndexWord indexWord = dictionary.getIndexWord(pos, parts[0]);
							if (indexWord!=null) {
								forms.add(indexWord);
							}
						} catch (JWNLException e) {
							System.err.println(e);
						}
					}
				}
			}
		} else {
			List<String> ids = getCorpusStoredDocumentIdsFromParameters(corpusMapper.getCorpus());
			FlexibleParameters params = new FlexibleParameters();
			params.setParameter("withPosLemmas", "true");
			for (String id : ids) {
				params.setParameter("docId", id);
				params.setParameter("limit", 0);
				DocumentTokens documentTokens = new DocumentTokens(storage, params);
				documentTokens.run(corpusMapper);
				for (DocumentToken documentToken : documentTokens.getDocumentTokens()) {
					String lemma = documentToken.getLemma();
					if (whiteList.isEmpty()==false && whiteList.isKeyword(lemma)==false) {continue;}
					if (stopwords.isKeyword(lemma)) {continue;}

					String pos = documentToken.getPos();
					if (lemma!=null && pos!=null) {
						POS poss = POS.getPOSForLabel(pos.toLowerCase());
						if (poss!=null && (poss==POS.ADJECTIVE || poss==POS.VERB || poss==POS.NOUN)) {
							try {
								IndexWord word = dictionary.getIndexWord(poss, lemma);
								if (word!=null) {
									forms.add(word);
								}
							}  catch (JWNLException e) {
								e.printStackTrace();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		
		if (forms.size()>1) {
			int start = parameters.getParameterIntValue("start", 0);
			int limit = parameters.getParameterIntValue("limit", Integer.MAX_VALUE);
			FlexibleQueue<RelatedWords> queue = new FlexibleQueue<RelatedWords>(new Comparator<RelatedWords>() {
				@Override
				public int compare(RelatedWords o1, RelatedWords o2) {
					return Integer.compare(o1.relationship.getDepth(), o2.relationship.getDepth());
				}
			}, start+limit);

			int i = 0;
			for (IndexWord outerWord : forms) {
				if (outerWord.getLemma()==null) {continue;}
				int j = 0;
				for (IndexWord innerWord : forms) {
					if (innerWord.getLemma()==null) {continue;}
					if (j>i) {
						RelationshipList list;
						try {
							list = RelationshipFinder.findRelationships(outerWord.getSenses().get(0), innerWord.getSenses().get(0), PointerType.HYPERNYM);
						} catch (CloneNotSupportedException e) {
							throw new RuntimeException("Unable to build relationships list", e);
						} catch (JWNLException e) {
							throw new RuntimeException("Unable to build relationships list", e);
						}
						if (list.size()==0) {continue;}
						AsymmetricRelationship relationship = (AsymmetricRelationship) list.get(0);
						if (relationship==null) {
							System.out.println(relationship);
						}
						queue.offer(new RelatedWords(innerWord, outerWord, relationship));
					}
					j++;
				}
				i++;
			}
			wordsList.addAll(queue.getOrderedList());
		}
		
	}


    public static void main(String[] args) throws FileNotFoundException, JWNLException, CloneNotSupportedException {
//    	new SemanticGraph().go();
    }

    public void go() throws JWNLException, CloneNotSupportedException {
    	IndexWord ACCOMPLISH = dictionary.getIndexWord(POS.VERB, "accomplish");
    	IndexWord DOG = dictionary.getIndexWord(POS.NOUN, "dog");
    	IndexWord CAT = dictionary.lookupIndexWord(POS.NOUN, "cat");
    	IndexWord FUNNY = dictionary.lookupIndexWord(POS.ADJECTIVE, "funny");
    	IndexWord DROLL = dictionary.lookupIndexWord(POS.ADJECTIVE, "droll");
    	String MORPH_PHRASE = "running-away";
        demonstrateMorphologicalAnalysis(MORPH_PHRASE);
        demonstrateListOperation(ACCOMPLISH);
        demonstrateTreeOperation(DOG);
        demonstrateAsymmetricRelationshipOperation(DOG, CAT);
        demonstrateSymmetricRelationshipOperation(FUNNY, DROLL);
    }

    private void demonstrateMorphologicalAnalysis(String phrase) throws JWNLException {
        // "running-away" is kind of a hard case because it involves
        // two words that are joined by a hyphen, and one of the words
        // is not stemmed. So we have to both remove the hyphen and stem
        // "running" before we get to an entry that is in WordNet
        System.out.println("Base form for \"" + phrase + "\": " +
                dictionary.lookupIndexWord(POS.VERB, phrase));
    }

    private void demonstrateListOperation(IndexWord word) throws JWNLException {
        // Get all of the hypernyms (parents) of the first sense of <var>word</var>
        PointerTargetNodeList hypernyms = PointerUtils.getDirectHypernyms(word.getSenses().get(0));
        System.out.println("Direct hypernyms of \"" + word.getLemma() + "\":");
        hypernyms.print();
    }

    private void demonstrateTreeOperation(IndexWord word) throws JWNLException {
        // Get all the hyponyms (children) of the first sense of <var>word</var>
        PointerTargetTree hyponyms = PointerUtils.getHyponymTree(word.getSenses().get(0));
        System.out.println("Hyponyms of \"" + word.getLemma() + "\":");
        hyponyms.print();
    }

    private void demonstrateAsymmetricRelationshipOperation(IndexWord start, IndexWord end) throws JWNLException, CloneNotSupportedException {
        // Try to find a relationship between the first sense of <var>start</var> and the first sense of <var>end</var>
        RelationshipList list = RelationshipFinder.findRelationships(start.getSenses().get(0), end.getSenses().get(0), PointerType.HYPERNYM);
        System.out.println("Hypernym relationship between \"" + start.getLemma() + "\" and \"" + end.getLemma() + "\":");
        for (Object aList : list) {
            ((Relationship) aList).getNodeList().print();
        }
        System.out.println("Common Parent Index: " + ((AsymmetricRelationship) list.get(0)).getCommonParentIndex());
        System.out.println("Depth: " + list.get(0).getDepth());
    }

    private void demonstrateSymmetricRelationshipOperation(IndexWord start, IndexWord end) throws JWNLException, CloneNotSupportedException {
        // find all synonyms that <var>start</var> and <var>end</var> have in common
        RelationshipList list = RelationshipFinder.findRelationships(start.getSenses().get(0), end.getSenses().get(0), PointerType.SIMILAR_TO);
        System.out.println("Synonym relationship between \"" + start.getLemma() + "\" and \"" + end.getLemma() + "\":");
        for (Object aList : list) {
            ((Relationship) aList).getNodeList().print();
        }
        System.out.println("Depth: " + list.get(0).getDepth());
    }
    
	public static class SemanticGraphConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return SemanticGraph.class.isAssignableFrom(type);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			SemanticGraph semanticGraph = (SemanticGraph) source;
			
			// first calculate frequencies
	        Map<String, AtomicInteger> edgeFreqs = new HashMap<String, AtomicInteger>();
	        Map<String, AtomicInteger> nodeFreqs = new HashMap<String, AtomicInteger>();
			for (RelatedWords relatedWords : semanticGraph.wordsList) {
					String src = relatedWords.firstWord.getLemma();
					String target = relatedWords.secondWord.getLemma();
					for (String s : new String[]{src,target}) {
						if (nodeFreqs.containsKey(s)==false) {
							nodeFreqs.put(s, new AtomicInteger(1));
						} else {
							nodeFreqs.get(s).incrementAndGet();
						}
					}
					String key = src +" - "+ target;
					if (edgeFreqs.containsKey(key)==false) {
						edgeFreqs.put(key, new AtomicInteger(1));
					}
					edgeFreqs.get(key).incrementAndGet();
			}
			
//	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "nodes", List.class);
//			for (Map.Entry<String, AtomicInteger> entry : nodeFreqs.entrySet()) {
//				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "node", String.class);
//		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "name", String.class);
//		        writer.setValue(entry.getKey());
//		        writer.endNode();
//		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
//		        writer.setValue(String.valueOf(entry.getValue().get()));
//		        writer.endNode();
//		        writer.endNode();
//			}
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "edges", List.class);
			for (Map.Entry<String, AtomicInteger> entry : edgeFreqs.entrySet()) {
				String[] parts = entry.getKey().split(" - ");

				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "edge", String.class);
				
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "source", String.class);
		        writer.setValue(parts[0]);
		        writer.endNode();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "target", String.class);
		        writer.setValue(parts[1]);
		        writer.endNode();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
		        writer.setValue(String.valueOf(entry.getValue().get()));
		        writer.endNode();
		        writer.endNode();
			}
			writer.endNode();
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader arg0,
				UnmarshallingContext arg1) {
			return null;
		}
		
	}
	
	private class RelatedWords {
		private IndexWord firstWord;
		private IndexWord secondWord;
		private Relationship relationship;
		private RelatedWords(IndexWord firstWord, IndexWord secondWord, Relationship relationship) {
			this.firstWord = firstWord;
			this.secondWord = secondWord;
			this.relationship = relationship;
		}
	}

}

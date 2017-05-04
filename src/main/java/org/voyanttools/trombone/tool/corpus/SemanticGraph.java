package org.voyanttools.trombone.tool.corpus;

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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

public class SemanticGraph extends AbstractCorpusTool {
	
    private Dictionary dictionary;

	
	public SemanticGraph(Storage storage, FlexibleParameters parameters) throws JWNLException {
		super(storage, parameters);
        dictionary = Dictionary.getDefaultResourceInstance();
    }
	
	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		Set<IndexWord> forms = new HashSet<IndexWord>();
		if (parameters.containsKey("posLemmas")) {
			for (String posLemma : parameters.getParameterValues("posLemmas")) {
				for (String pl : posLemma.split(",")) {
					if (pl.contains("/")) {
						String[] parts = pl.trim().split("/");
						try {
							
							IndexWord indexWord = dictionary.getIndexWord(POS.getPOSForLabel(parts[0]), parts[1]);
							forms.add(indexWord);
						} catch (JWNLException e) {
							System.err.println(e);
						}
					}
				}
			}
		}
		
		
		// TODO Auto-generated method stub
		
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
}

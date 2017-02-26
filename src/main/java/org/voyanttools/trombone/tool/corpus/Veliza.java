/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.Stripper;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import net.chayden.ElizaApp;
import net.chayden.ElizaMain;

/**
 * @author sgs
 *
 */
@XStreamAlias("veliza")
public class Veliza extends AbstractCorpusTool {
	
	String response = null;
	String sentence = null;
	String[] previous = null;
	int docIndex = -1;

	/**
	 * @param storage
	 * @param parameters
	 */
	public Veliza(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		
		// load Eliza
        ElizaMain eliza = new ElizaMain();
        String script = ElizaApp.class.getResource("script").getFile();
        int res = eliza.readScript(true, script);
        if (res!=0) {
			throw new RuntimeException("Unable to load Veliza's data file.");
        }
        Set<String> nones = new HashSet<String>(); // tracks when we don't really get an answer
        nones.add("I'm not sure I understand you fully.");
        nones.add("Please go on.");
        nones.add("What does that suggest to you ?");
        nones.add("Do you feel strongly about discussing such things ?");
        nones.add("That is interesting.  Please continue.");
        nones.add("Tell me more about that.");
        nones.add("Does talking about this bother you ?");
        
        // see if we need to read from corpus
		if (parameters.getParameterBooleanValue("fromCorpus")) {
			
			Corpus corpus = corpusMapper.getCorpus();
			
			// first randomly select a document in the corpus
			docIndex = (int) Math.floor(Math.random() * corpus.size());

			// try to load stored document and if not generate sentences
			String id = corpusMapper.getCorpus().getId()+"-docIndex-"+String.valueOf(docIndex)+"-veliza-sentences-"+String.valueOf(this.getVersion());
			List<String> sentences;
			if (!storage.isStored(id)) {
				sentences = this.storeSentences(corpus.getDocument(docIndex), id);
			} else {
				sentences = storage.retrieveStrings(id);
			}
			
			// now try to find a sentence that produces a reply for the specified time
			long giveUp = Calendar.getInstance().getTimeInMillis()+1000;
			while (true) {
				int random = (int) Math.floor(Math.random() * sentences.size());
				sentence = sentences.get(random);
	        	response = eliza.processInput(sentence);
	        	System.err.println(random+" "+response+" "+sentence);
	        	if (nones.contains(response)==false || Calendar.getInstance().getTimeInMillis()>giveUp) {
	        		break;
	        	}
			}
			
		}
		
		// otherwise we'll hope to have a sentence parameter
		else {
			
			sentence = parameters.getParameterValue("sentence", "").trim();
			if (sentence.length()==0) {
				throw new RuntimeException("Veliza needs an input sentence to function.");
			}
			
        	// load previous answers to use Eliza's existing memory mechanism
        	previous = parameters.getParameterValues("previous");
        	for (String prev : previous) {
        		if (!prev.isEmpty()) {eliza.mem.save(prev);}
        	}
        	
        	// grab answer
        	response = eliza.processInput(sentence);
        }
		
    	// add to previous
    	if (!response.equals("I'm not sure I understand you fully.") && !ArrayUtils.contains(previous, response)) {
    		previous = ArrayUtils.add(previous, 0, response);
    	}
	}
	
	private List<String> storeSentences(IndexedDocument document, String storedId) throws IOException {
		List<String> sentences = new ArrayList<String>();
		Pattern sentencePattern = Pattern.compile("\\p{L}.*?[.!?](\\s|$)");
		Stripper stripper = new Stripper(Stripper.TYPE.ALL); // only used for text output
		String string = document.getDocumentString();
		string = stripper.strip(string).trim().replace("&amp;", "&");
		string = string.replaceAll("\\s+", " "); // all whitepace becomes a single space
		Matcher matcher = sentencePattern.matcher(string);
		while (matcher.find()) {
			sentences.add(matcher.group(0).trim());
		}
		if (sentences.isEmpty()==false) {
			storage.storeStrings(sentences, storedId);
		}
		return sentences;
	}

}

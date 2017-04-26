/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.cxf.helpers.IOUtils;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.UriInputSource;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.tool.resource.StoredResource;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.Stripper;
import org.voyanttools.trombone.util.TextUtils;

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
	String script = null;
	String id = null;
	int docIndex = -1;

	/**
	 * @param storage
	 * @param parameters
	 */
	public Veliza(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}
	
	@Override
	public float getVersion() {
		return super.getVersion()+1;
	}

	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		
		String scriptParam = parameters.getParameterValue("script", "").trim();
		
		// load the default script (and possibly remove it at the end if not requested)
		if (scriptParam.isEmpty()) {
			InputStream is = null;
			try {
				is = ElizaApp.class.getResource("script").openStream();
				script = IOUtils.readStringFromStream(is);
			} catch (IOException e) {
				throw new IOException("Unable to read default Veliza script file.", e);
			} finally {
				if (is!=null) {
					is.close();
				}
			}
		} else {
			if (scriptParam.startsWith("http")) {
				StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
				URI uri;
				try {
					uri = new URI(scriptParam);
				} catch (URISyntaxException e) {
					throw new IOException("Bad URI provided for script: "+scriptParam);
				}
				InputSource inputSource = new UriInputSource(uri);
				StoredDocumentSource storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
				InputStream inputStream = null;
				try {
					inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
					script = IOUtils.readStringFromStream(inputStream);
				}
				finally {
					if (inputStream!=null) {
						inputStream.close();
					}
				}
			} else if (Pattern.compile("\\s").matcher(scriptParam).find()) {
				// input has spaces so treat it as the script itself but store it for another time
				script = scriptParam;
				FlexibleParameters p = new FlexibleParameters();
				p.setParameter("storeResource", scriptParam);
				StoredResource storedResource = new StoredResource(storage, p);
				storedResource.run();
				id = storedResource.getResourceId();
				script = storedResource.getResource();
				
			} else {
				// treat it as resourceId
				StoredResource storedResource = new StoredResource(storage, new FlexibleParameters(new String[]{"retrieveResourceId="+scriptParam}));
				storedResource.run();
				script = storedResource.getResource();
			}
		}
		
		if (parameters.containsKey("fromCorpus") || parameters.containsKey("sentence")) {

			// load Eliza
	        ElizaMain eliza = new ElizaMain();
	        for (String s : script.split("(\r\n|\r|\n)")) {
	        	System.out.println(" - "+s);
	        	eliza.collect(s);
	        }
	        script = null;

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
	}
	
	private List<String> storeSentences(IndexedDocument document, String storedId) throws IOException {
		String string = document.getDocumentString();
		List<String> sentences = TextUtils.getSentences(string, Locale.ENGLISH);
		if (sentences.isEmpty()==false) {
			storage.storeStrings(sentences, storedId);
		}
		return sentences;
	}
	
	List<String> getSentences(String string) {
		Pattern abbreviations = Pattern.compile("\\b(Mrs?|Dr|Rev|Mr|Ms|st)\\.$", Pattern.CASE_INSENSITIVE);
		List<String> sentences = new ArrayList<String>();
		Stripper stripper = new Stripper(Stripper.TYPE.ALL); // only used for text output
		string = stripper.strip(string).trim().replace("&amp;", "&");
		string = string.replaceAll("\\s+", " "); // all whitepace becomes a single space
		BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(Locale.US);
		sentenceIterator.setText(string);
	     int start = sentenceIterator.first();
	     StringBuffer sb = new StringBuffer();
	     for (int end = sentenceIterator.next();
	          end != sentenceIterator.DONE;
	          start = end, end = sentenceIterator.next()) {
	    	 sb.append(string.substring(start,end).trim());
	    	 String sentence = sb.toString();
	    	 if (abbreviations.matcher(sentence).find()==false) {
	    		 if (sentence.contains(" ")) {
		    		 sentences.add(sentence);
	    		 }
	    		 sb.setLength(0); // reset buffer
	    	 } else {
	    		 sb.append(" ");
	    	 }
	     }
	     return sentences;
	}
}

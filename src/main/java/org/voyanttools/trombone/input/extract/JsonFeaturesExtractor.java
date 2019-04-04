/**
 * 
 */
package org.voyanttools.trombone.input.extract;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class JsonFeaturesExtractor implements Extractor {
	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	private FlexibleParameters parameters;

	/**
	 * 
	 */
	public JsonFeaturesExtractor(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.input.extract.Extractor#getExtractableInputSource(org.voyanttools.trombone.model.StoredDocumentSource)
	 */
	@Override
	public InputSource getExtractableInputSource(StoredDocumentSource storedDocumentSource) throws IOException {
		StringBuilder id = new StringBuilder(storedDocumentSource.getId()).append("jsonfeatures-extracted");
		// add any other parameters
		return new ExtractableJsonFeaturesInputSource(DigestUtils.md5Hex(id.toString()), storedDocumentSource);
	}

	private class ExtractableJsonFeaturesInputSource implements InputSource {
		
		private String id;
		private StoredDocumentSource storedDocumentSource;
		private DocumentMetadata metadata;
		private boolean isProcessed = false;
		
		ExtractableJsonFeaturesInputSource(String id, StoredDocumentSource storedDocumentSource) {
			this.id = id;
			this.storedDocumentSource = storedDocumentSource;
			this.metadata = storedDocumentSource.getMetadata();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			
			// load line
			InputStream is = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
			String jsonString = IOUtils.toString(is, StandardCharsets.UTF_8);
			
			// parse doc
			JSONParser parser = new JSONParser();
			Object obj;
			try {
				obj = parser.parse(jsonString);
			} catch (ParseException e) {
				throw new IOException("Unable to parse JSON features "+storedDocumentSource.getId()+" ("+storedDocumentSource.getMetadata().getLocation()+")");
			}
			
			JSONObject jsonObject = (JSONObject) obj;
	        List<String> words = new ArrayList<String>();

	        
	        JSONObject features = (JSONObject) jsonObject.get("features");
	        if (features!=null) {
	        		        
		        JSONArray pages = (JSONArray) features.get("pages");
		        Iterator<JSONObject> iterator = pages.iterator();
		        int j=0;
		        while (iterator.hasNext()) {
		        	JSONObject page = iterator.next();
		        	JSONObject body = (JSONObject) page.get("body");
		        	if (body==null) {continue;}
		        	JSONObject tokenPosCount = (JSONObject) body.get("tokenPosCount");
		        	Set<String> terms = tokenPosCount.keySet();
		        	for (String term : terms) {
		        		if (!Character.isLetter(term.charAt(0))) {continue;} // skip if not starting with alphabetic
		        		JSONObject posCounts = (JSONObject) tokenPosCount.get(term);
		        		for (String pos : (Set<String>) posCounts.keySet()) {
		        			int count = ((Long) posCounts.get(pos)).intValue();
		        			for (int i=0; i<count; i++) {
		        				words.add(term);
		        			}
		        		}
		        	}
		        }
	        }
	        
			JSONObject metadataObj = (JSONObject) jsonObject.get("metadata");
			
			if (metadataObj!=null) {
				
				String title = (String) metadataObj.get("name");
				if (title==null) {
					title = (String) metadataObj.get("title");
				}
				if (title!=null) {
					metadata.setTitle(title);
				}

				
				// loop array
				Set<String> authors = new HashSet<String>();
				Object contributor = metadataObj.get("contributor");
				if (contributor!=null) {
					setNamesFromContributor(metadataObj.get("contributor"), authors);
				}
				if (authors.isEmpty()==false) {
					metadata.setAuthors(authors.toArray(new String[0]));
				}
			}
			
	    	Collections.shuffle(words); // we re-hydrate words but in a plausible order to avoid nasty ngrams
	    	String string = String.join(" ", words);
	    	isProcessed = true;
	    	
	    	
	    	return new ByteArrayInputStream(string.getBytes("UTF-8"));
	    }
		
		private void setNamesFromContributor(Object object, Set<String> names) {
			if (object instanceof JSONObject) {
				setNamesFromName(((JSONObject) object).get("name"), names);
			} else if (object instanceof JSONArray) {
				Iterator<Object> iterator = ((JSONArray) object).iterator();
				while(iterator.hasNext()) {
					setNamesFromContributor(iterator.next(), names);
				}
			} else {
				throw new IllegalStateException("contributor should be an array or object");
			}
		}
		
		private void setNamesFromName(Object object, Set<String> names) {
			if (object instanceof String && ((String) object).trim().isEmpty()==false) {
				names.add((String) object); 
			} else if (object instanceof JSONArray) {
				Iterator<Object> iterator = ((JSONArray) object).iterator();
				while (iterator.hasNext()) {
					setNamesFromName(iterator.next(), names);
				}
			}
		}

		@Override
		public DocumentMetadata getMetadata() throws IOException {
			return isProcessed ? this.metadata : storedDocumentSourceStorage.getStoredDocumentSourceMetadata(id);
		}

		@Override
		public String getUniqueId() {
			return id;
		}
		
	}
}

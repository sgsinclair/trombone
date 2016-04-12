/**
 * 
 */
package org.voyanttools.trombone.input.expand;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.UriInputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.storage.file.FileStoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;


/**
 * @author sgs
 *
 */
public class ObApiSearchExpander implements Expander {

	/**
	 * all parameters sent, only some of which may be relevant to some expanders
	 */
	private FlexibleParameters parameters;
	
	/**
	 * the stored document storage strategy
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;

	/**
	 * @param parameters 
	 * @param storedDocumentSourceStorage 
	 * 
	 */
	public ObApiSearchExpander(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.input.expand.Expander#getExpandedStoredDocumentSources(org.voyanttools.trombone.model.StoredDocumentSource)
	 */
	@Override
	public List<StoredDocumentSource> getExpandedStoredDocumentSources(StoredDocumentSource storedDocumentSource) throws IOException {
		List<StoredDocumentSource> sourceDocumentSources = new ArrayList<StoredDocumentSource>();
		File localOldBaileyDir = null;
		if (storedDocumentSourceStorage instanceof FileStoredDocumentSourceStorage) {
			File dummyFile = ((FileStoredDocumentSourceStorage) storedDocumentSourceStorage).getDocumentSourceDirectory("dummy");
			File rootData = dummyFile.getParentFile().getParentFile().getParentFile();
			localOldBaileyDir = new File(rootData, "OldBaileyXmlDocuments");
		}
		InputStream is = null;
		String jsonString;
		try {
			is = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
			jsonString = IOUtils.toString(is);
		} finally {
			if (is!=null) {is.close();}
		}
		
		JSONParser parser = new JSONParser();
		JSONObject obj;
		try {
			obj = (JSONObject) parser.parse(jsonString);
		} catch (ParseException e) {
			throw new IOException("Unable to parse JSON results: "+storedDocumentSource);
		}
		JSONArray hits = (JSONArray) obj.get("hits");
		List<String> ids = new ArrayList<String>();
		for (int i=0; i<hits.size(); i++) {
			ids.add((String) hits.get(i));
		}
		
		for (String id : ids) {
			InputSource inputSource;
			if (localOldBaileyDir!=null && localOldBaileyDir.exists() && new File(localOldBaileyDir, id+".xml").exists()) {
				inputSource = new FileInputSource(new File(localOldBaileyDir, id+".xml"));
			} else {
				String uriString = "http://www.oldbaileyonline.org//obapi/text?div="+id;
				URI uri;
				try {
					uri = new URI(uriString);
				}
				catch (URISyntaxException e) {
					throw new IllegalArgumentException("The URI provided by the parameters has a problem: "+uriString, e);
				}
				inputSource = new UriInputSource(uri);
			}
			inputSource.getMetadata().setDocumentFormat(DocumentFormat.OLDBAILEYXML);
			sourceDocumentSources.add(storedDocumentSourceStorage.getStoredDocumentSource(inputSource));
		}

		// we have to switch the inputFormat for the next step (and hope that it sticks)
		parameters.setParameter("inputFormat", DocumentFormat.OLDBAILEYXML.name());
		return sourceDocumentSources;
	}

}

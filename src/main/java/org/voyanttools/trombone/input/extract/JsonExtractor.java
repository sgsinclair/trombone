/**
 * 
 */
package org.voyanttools.trombone.input.extract;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.LangDetector;


/**
 * @author sgs
 *
 */
public class JsonExtractor implements Extractor {
	private static final long serialVersionUID = -8659873836740839314L;
	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	private FlexibleParameters parameters;

	/**
	 * 
	 */
	public JsonExtractor(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
	}

	@Override
	public InputSource getExtractableInputSource(StoredDocumentSource storedDocumentSource) throws IOException {
		FlexibleParameters localParameters = parameters.clone();
		
		String[] relevantParameters = new String[]{"jsonContentPointer","jsonTitlePointer","jsonAuthorPointer","jsonPubPlacePointer","jsonPublisherPointer","jsonPubDatePointer","jsonKeywordPointer","jsonCollectionPointer","jsonExtraMetadataPointer"};
		StringBuilder parametersBuilder = new StringBuilder();
		for (String p : relevantParameters) {
			if (localParameters.getParameterValue(p, "").isEmpty()==false) {
				parametersBuilder.append(p);
				for (String s : localParameters.getParameterValues(p)) {
					parametersBuilder.append(s);
				}
			}
		}
		
		// add any other parameters
		return new ExtractableJsonInputSource(DigestUtils.md5Hex(storedDocumentSource.getId()+relevantParameters+String.valueOf(serialVersionUID)), storedDocumentSource, localParameters);

	}
	
	private class ExtractableJsonInputSource implements InputSource {
		private String id;
		private String storedDocumentSourceId;
		private StoredDocumentSource storedDocumentSource;
		private DocumentMetadata metadata;
		private boolean isProcessed = false;
		private FlexibleParameters localParameters;
		
		ExtractableJsonInputSource(String id, StoredDocumentSource storedDocumentSource, FlexibleParameters localParameters) {
			this.id = id;
			this.storedDocumentSourceId = storedDocumentSource.getId();
			this.storedDocumentSource = storedDocumentSource;
			this.metadata = storedDocumentSource.getMetadata().asParent(storedDocumentSourceId, DocumentMetadata.ParentType.EXTRACTION);
			this.metadata.setLocation(storedDocumentSource.getMetadata().getLocation());
			this.metadata.setDocumentFormat(DocumentFormat.JSON);
			this.localParameters = localParameters;
		}
		
		@Override
		public InputStream getInputStream() throws IOException {
			
			// load line
			InputStream is = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
			JsonReader jsonReader = Json.createReader(is);
			
			JsonStructure jsonStructure = jsonReader.read();
			
			String location = metadata.getLocation();
			// try to find title if needed
			String[] titles = getValuesFromStructure(jsonStructure, "jsonTitlePointer", location);
			if (titles.length>0) {
				metadata.setTitles(titles);
			}
			
			// try to find author if needed
			String[] authors = getValuesFromStructure(jsonStructure, "jsonAuthorPointer", location);
			if (authors.length>0) {
				metadata.setAuthors(authors);
			}

			// try to find publplace if needed
			String[] pubPlaces = getValuesFromStructure(jsonStructure, "jsonPubPlacePointer", location);
			if (pubPlaces.length>0) {
				metadata.setPubPlaces(pubPlaces);
			}

			// try to find title if needed
			String[] publishers = getValuesFromStructure(jsonStructure, "jsonPublisherPointer", location);
			if (publishers.length>0) {
				metadata.setPublishers(publishers);
			}

			// try to find pubDates if needed
			String[] pubDates = getValuesFromStructure(jsonStructure, "jsonPubDatePointer", location);
			if (pubDates.length>0) {
				metadata.setPubDates(pubDates);
			}
			
			// try to find pubDates if needed
			String[] collections = getValuesFromStructure(jsonStructure, "jsonCollectionPointer", location);
			if (collections.length>0) {
				metadata.setCollections(collections);
			}
			
			// try to find pubDates if needed
			String[] keywords = getValuesFromStructure(jsonStructure, "jsonKeywordPointer", location);
			if (keywords.length>0) {
				metadata.setKeywords(keywords);
			}
			
			for (String extra : localParameters.getParameterValues("jsonExtraMetadataXpath")) {
				for (String x :extra.split("(\r\n|\r|\n)+")) {
					x = x.trim();
					String[] parts = x.split("=");
					if (parts.length>1) {
						String key = parts[0].trim();
						String pointer = StringUtils.join(Arrays.copyOfRange(parts, 1, parts.length), "=").trim();
						String[] values = getValuesFromStructure(jsonStructure, pointer, location);
						if (values.length>0) {
							metadata.setExtras(key, values);
						}
					}
				}
			}
			
			String string;
			if (localParameters.containsKey("jsonContentPointer")) {
				String[] strings = getValuesFromStructure(jsonStructure, "jsonContentPointer", location);
				string = StringUtils.join(strings, "\n\n");
			} else { // if nothing defined, use all JSON, which is probably ugly
				string = jsonStructure.toString();
			}
			
			if (metadata.getTitle().isEmpty()) {
				metadata.setTitle(string.substring(0, 20));
			}

			// try to determine language
			metadata.setLanguageCode(LangDetector.langDetector.detect(string, parameters));
			
	        isProcessed = true;

	    	return new ByteArrayInputStream(string.getBytes("UTF-8"));
		}
		
		private String[] getValuesFromStructure(JsonStructure jsonStructure, String param, String location) throws IOException {
			String jsonPointer = parameters.getParameterValue(param, "");
			if (jsonPointer.trim().isEmpty()) {return new String[0];}
			JsonValue jsonValue;
			try {
				jsonValue = jsonStructure.getValue(jsonPointer);
			} catch (JsonException e) {
				throw new IOException("Unable to find the specified: "+jsonPointer+" for document "+location);
			}
			switch (jsonValue.getValueType()) {
			// we don't handle object type, what would we do with it?
			case ARRAY:
				JsonArray jsonArray = jsonValue.asJsonArray();
				return jsonArray.getValuesAs(JsonString.class).toArray(new String[0]);
			case NUMBER:
				return new String[] {String.valueOf(((JsonNumber) jsonValue).numberValue())};
			case STRING:
				return new String[] {((JsonString) jsonValue).getString()};
			case TRUE:
			case FALSE:
				return new String[]{jsonValue.toString()};
			default:
				return new String[0];
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

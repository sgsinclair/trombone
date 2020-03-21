package org.voyanttools.trombone.model;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.voyanttools.trombone.storage.Storage;

public class Categories {
	
	private Map<String, HashSet<String>> categories;

	public Categories() {
		categories = new HashMap<String, HashSet<String>>();
	}
	
	public boolean isEmpty() {
		return categories.isEmpty();
	}
	
	public boolean hasCategory(String category) {
		return categories.containsKey(category);
	}

	public Collection<String> getCategory(String category) {
		return categories.get(category);
	}
	
	public static Categories getCategories(Storage storage, Corpus corpus, String id) throws IOException {
		
		if (id.trim().isEmpty()) {
			return new Categories();
		}
		
		if (id.equals("auto")) {
			File resourcesDir = new File(Categories.class.getResource("/org/voyanttools/trombone/categories").getFile());
			for (String lang : corpus.getLanguageCodes()) {
				if (new File(resourcesDir, "categories."+lang+".txt").exists()) {
					return getCategories(storage, corpus, "categories."+lang+".txt");
				}
			}
			// we've tried all the language codes and none match, so return empty categories
			return new Categories();
		}
		
		if (id.length()==2) { // looks like language code
			return getCategories(storage, corpus, "categories."+id+".txt");
		}
		
		if (id.matches("categories\\.\\w\\w\\..*?txt")) { // looks like local resource
			return getCategories(Categories.class.getResource("/org/voyanttools/trombone/categories/"+id));
		}
		
		// good 'ol resource
		String contents = storage.retrieveString(id, Storage.Location.object);
		return getCategories(contents);
		
		
	}
	
	private static Categories getCategories(URL url) throws IOException {
		URI uri;
		try {
			uri = url.toURI();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Unable to create URI for categories: "+url);
		}
		StringBuilder contentBuilder = new StringBuilder();
	    Stream<String> stream = Files.lines( Paths.get(uri), StandardCharsets.UTF_8);
        stream.forEach(s -> contentBuilder.append(s).append("\n"));
        stream.close();
		return getCategories(contentBuilder.toString());
		
	}
	private static Categories getCategories(String contents) {
		if (contents.trim().startsWith("{")==false || contents.contains("categories")==false) {
			throw new IllegalArgumentException("Unable to find categories.");
		}
		Categories categories = new Categories();
		StringReader stringReader = new StringReader(contents);
		JsonReader reader = Json.createReader(stringReader);
		JsonObject root = reader.readObject();
		JsonObject cats = root.getJsonObject("categories");
		for (String key : cats.keySet()) {
			JsonArray wordsArray = cats.getJsonArray(key);
			HashSet<String> words = new HashSet<String>();
			for (int i=0,len=wordsArray.size(); i<len; i++) {
				words.add(wordsArray.getJsonString(i).getString());
			}
			categories.categories.put(key, words);
		}
		stringReader.close();
		return categories;
	}
	
}

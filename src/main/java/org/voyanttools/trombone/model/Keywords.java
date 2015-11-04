/*******************************************************************************
 * Trombone is a flexible text processing and analysis library used
 * primarily by Voyant Tools (voyant-tools.org).
 * 
 * Copyright (©) 2007-2012 Stéfan Sinclair & Geoffrey Rockwell
 * 
 * This file is part of Trombone.
 * 
 * Trombone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.UriInputSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("keywords")
public class Keywords {
	
	private static String COMMA_SEPARATOR = ",";
	private static String HTTP_PREFIX = "http://";
	private static String STOPWORDS_FILE_PREFIX = "stop.";
	private static String KEYWORDS_PREFIX = "keywords-";
	private static String COMMENT = "#";
	
	private Set<String> keywords;

	/**
	 * 
	 */
	public Keywords() {
		keywords = new LinkedHashSet<String>();
	}
	
	public boolean isKeyword(String keyword) {
		return keywords.contains(keyword);
	}

	public void load(Storage storage, String[] references) throws IOException {
		for (String ref : references) {
			ref = ref.trim();
			if (ref.contains(",")) { // comma-separated references
				load(storage, ref.split(COMMA_SEPARATOR));
			}
			else if (ref.startsWith(HTTP_PREFIX)) {
				StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
				URI uri;
				try {
					uri = new URI(ref);
				} catch (URISyntaxException e) {
					throw new IOException("Bad URI provided for keywords: "+ref);
				}
				InputSource inputSource = new UriInputSource(uri);
				StoredDocumentSource storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
				InputStream inputStream = null;
				try {
					inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
					List<String> keys = IOUtils.readLines(inputStream);
					add(keys);
				}
				finally {
					if (inputStream!=null) {
						inputStream.close();
					}
				}
			}
			else if (ref.startsWith(STOPWORDS_FILE_PREFIX)) {
				URI uri;
				try {
					uri = this.getClass().getResource("/org/voyanttools/trombone/keywords").toURI();
				} catch (URISyntaxException e) {
					throw new IOException("Unable to find local stopwords directory", e);
				}
				File dir = new File(uri.getPath());
				File file = new File(dir, ref);
				List<String> refs = FileUtils.readLines(file);
				add(refs);
			}
			else if (ref.startsWith(KEYWORDS_PREFIX)) {
				try {
					List<String> refs = storage.retrieveStrings(ref.substring(KEYWORDS_PREFIX.length()));
					add(refs);
				} catch (IOException e) {
					throw new IOException("Unable to load keyword file: "+ref);
				}
			}
			else { // individual term, so let's add it
				keywords.add(ref);
			}
		}
	}
	
	public void sort() {
		List<String> strings = new ArrayList<String>(keywords);
		Collections.sort(strings, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return Normalizer.normalize(s1, Normalizer.Form.NFD).compareToIgnoreCase(Normalizer.normalize(s2, Normalizer.Form.NFD));
			}
		});
		keywords.clear();
		keywords.addAll(strings);
	}
	
	Collection<String> getKeywords() {
		return keywords;
	}
	
	private void add(Collection<String> keywords) {
		for (String keyword : keywords) {
			if (keyword.startsWith(COMMENT)==false) {
				this.keywords.add(keyword);
			}
		}
	}

}

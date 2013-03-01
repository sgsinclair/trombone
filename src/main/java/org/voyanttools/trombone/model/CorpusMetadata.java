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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;


import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.collections.MapConverter;

import edu.stanford.nlp.util.StringUtils;

/**
 * @author sgs
 *
 */
public class CorpusMetadata {
	
	private Properties properties;
	
	public CorpusMetadata(String id) {
		properties = new Properties();
		setProperty("id", id);
	}
	
	public List<String> getDocumentIds() {
		return Arrays.asList(getProperty("documentIds", "").split(","));
	}

	public String getId() {
		return getProperty("id");
	}

	public void setDocumentIds(Collection<String> ids) {
		setProperty("documentIds", StringUtils.join(ids, ","));
	}
	
	private String getProperty(String key) {
		return properties.getProperty(key);
	}
	
	private String getProperty(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}
	
	private void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	public Properties getProperties() {
		return properties;
	}

}

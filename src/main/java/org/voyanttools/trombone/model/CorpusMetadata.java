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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import edu.stanford.nlp.util.StringUtils;

/**
 * @author sgs
 *
 */
public class CorpusMetadata implements PropertiesWrapper {
	
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

	public void setCreatedTime(long time) {
		properties.setProperty("createdTime", String.valueOf(time));
	}

	public long getCreatedTime() {
		return Long.valueOf(properties.getProperty("createdTime", "0"));
	}

	public void setTokensCount(TokenType tokenType, int totalWordTokens) {
		properties.setProperty("tokensCount-"+tokenType.name(), String.valueOf(totalWordTokens));
	}

	public void setTypesCount(TokenType tokenType, int totalWordTokens) {
		properties.setProperty("typesCount-"+tokenType.name(), String.valueOf(totalWordTokens));
	}
	
	public int getTokensCount(TokenType tokenType) {
		return Integer.valueOf(properties.getProperty("tokensCount-"+tokenType.name(), "0"));
	}
	
	public int getTypesCount(TokenType tokenType) {
		return Integer.valueOf(properties.getProperty("typesCount-"+tokenType.name(), "0"));
	}
	
	public static class CorpusMetadataConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return CorpusMetadata.class == type;
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer,
				MarshallingContext context) {
			final CorpusMetadata corpusMetadata = ((CorpusMetadata) source);
			
			
//			writer.startNode("id");
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "id", String.class);
			writer.setValue(corpusMetadata.getId());
			writer.endNode();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "documentsCount", Integer.class);
			writer.setValue(String.valueOf(corpusMetadata.getDocumentIds().size()));
			writer.endNode();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "createdTime", Integer.class);
			writer.setValue(String.valueOf(String.valueOf(corpusMetadata.getCreatedTime())));
			writer.endNode();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "createdDate", String.class);
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(corpusMetadata.getCreatedTime());
			writer.setValue(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(calendar.getTime()));
			writer.endNode();

			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "lexicalTokensCount", Integer.class);
			writer.setValue(String.valueOf(corpusMetadata.getTokensCount(TokenType.lexical)));
			writer.endNode();

			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "lexicalTypesCount", Integer.class);
			writer.setValue(String.valueOf(corpusMetadata.getTypesCount(TokenType.lexical)));
			writer.endNode();

		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader,
				UnmarshallingContext context) {
			// TODO Auto-generated method stub
			return null;
		}

	}
}

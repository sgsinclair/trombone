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

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.voyanttools.trombone.tool.util.ToolSerializer;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author sgs
 *
 */
public class CorpusMetadata implements Serializable {
	
	private FlexibleParameters parameters;
	
	public CorpusMetadata(String id) {
		parameters = new FlexibleParameters();
		setProperty("id", id);
	}
	
	public CorpusMetadata(FlexibleParameters parameters) {
		this.parameters = parameters;
	}
	
	public List<String> getDocumentIds() {
		String[] ids = parameters.getParameterValues("documentIds");
		return ids.length==1 && ids[0].isEmpty() ? new ArrayList<String>() : Arrays.asList(parameters.getParameterValues("documentIds"));
	}

	public String getId() {
		return getProperty("id");
	}

	public String getAlias() {
		return parameters.getParameterValue("alias");
	}
	
	public void setDocumentIds(Collection<String> ids) {
		parameters.setParameter("documentIds", ids.toArray(new String[0]));
	}
	
	private String getProperty(String key) {
		return parameters.getParameterValue(key);
	}
	
	private String getProperty(String key, String defaultValue) {
		return parameters.getParameterValue(key, defaultValue);
	}
	
	private void setProperty(String key, String value) {
		parameters.setParameter(key, value);
	}

	private void setProperty(String key, String[] values) {
		parameters.setParameter(key, values);
	}

	public void setCreatedTime(long time) {
		setProperty("createdTime", String.valueOf(time));
	}

	public long getCreatedTime() {
		return Long.valueOf(getProperty("createdTime", "0"));
	}

	public void setTokensCount(TokenType tokenType, int totalWordTokens) {
		setProperty("tokensCount-"+tokenType.name(), String.valueOf(totalWordTokens));
	}

	public void setTypesCount(TokenType tokenType, int totalWordTokens) {
		setProperty("typesCount-"+tokenType.name(), String.valueOf(totalWordTokens));
	}
	
	public void setTypesCountMean(TokenType tokenType, float f) {
		setTypesCountMean(tokenType.name(), f);
	}

	public void setTypesCountMean(String field, float f) {
		setProperty("typesCountMean"+field, String.valueOf(f));
	}
	
	public void setTypesCountStdDev(TokenType tokenType, float f) {
		setTypesCountStdDev(tokenType.name(), f);
	}
	
	public void setTypesCountStdDev(String field, float f) {
		setProperty("typesCountMean"+field, String.valueOf(f));
	}
	
	public int getTokensCount(TokenType tokenType) {
		return Integer.valueOf(getProperty("tokensCount-"+tokenType.name(), "0"));
	}
	
	public int getTypesCount(TokenType tokenType) {
		return Integer.valueOf(getProperty("typesCount-"+tokenType.name(), "0"));
	}
	
	public float getTypesCountMean(TokenType tokenType) {
		return Float.valueOf(getProperty("typesCountMean-"+tokenType.name(), "0"));
	}
	public float getTypesCountStdDev(TokenType tokenType) {
		return Float.valueOf(getProperty("typesCountStdDev-"+tokenType.name(), "0"));
	}
	
	public void setPasswords(CorpusAccess mode, String[] passwords) {
		setProperty(mode.name().toLowerCase()+"Passwords", passwords);
	}

	public String[] getAccessPasswords(CorpusAccess mode) {
		return parameters.getParameterValues(mode.name().toLowerCase()+"Passwords");
	}

	public void setNoPasswordAccess(CorpusAccess corpusAccess) {
		setProperty("noPasswordAccess", corpusAccess.name());
	}
	
	public CorpusAccess getNoPasswordAccess() {
		return CorpusAccess.getForgivingly(this.getProperty("noPasswordAccess"));
	}
	
	public String[] getLanguageCodes() {
		return parameters.getParameterValues("languageCodes");
	}

	public void setLanguageCodes(String[] languageCodes) {
		parameters.setParameter("languageCodes", languageCodes);
	}
	
	public String getTitle() {
		return parameters.getParameterValue("title", "");
	}
	
	public void setTitle(String title) {
		parameters.setParameter("title", title);
	}
	
	public String getSubTitle() {
		return parameters.getParameterValue("subTitle", "");
	}
	
	public void setSubTitle(String subTitle) {
		parameters.setParameter("subTitle", subTitle);
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
			
			writer.startNode("id");
			writer.setValue(corpusMetadata.getId());
			writer.endNode();
			
			String alias = corpusMetadata.getAlias();
			if (alias!=null && alias.isEmpty()==false) {
				writer.startNode("alias");
				writer.setValue(alias);
				writer.endNode();
			}
			
			writer.startNode("title");
			writer.setValue(corpusMetadata.getTitle());
			writer.endNode();
			
			writer.startNode("subTitle");
			writer.setValue(corpusMetadata.getSubTitle());
			writer.endNode();
			
			ToolSerializer.startNode(writer, "documentsCount", Integer.class);
			writer.setValue(String.valueOf(corpusMetadata.getDocumentIds().size()));
			ToolSerializer.endNode(writer);
			
			ToolSerializer.startNode(writer, "createdTime", Integer.class);
			writer.setValue(String.valueOf(String.valueOf(corpusMetadata.getCreatedTime())));
			ToolSerializer.endNode(writer);
			
			writer.startNode("createdDate");
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(corpusMetadata.getCreatedTime());
			writer.setValue(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(calendar.getTime()));
			writer.endNode();

			ToolSerializer.startNode(writer, "lexicalTokensCount", Integer.class);
			writer.setValue(String.valueOf(corpusMetadata.getTokensCount(TokenType.lexical)));
			ToolSerializer.endNode(writer);

			ToolSerializer.startNode(writer, "lexicalTypesCount", Integer.class);
			writer.setValue(String.valueOf(corpusMetadata.getTypesCount(TokenType.lexical)));
			ToolSerializer.endNode(writer);

			writer.startNode("noPasswordAccess");
			writer.setValue(corpusMetadata.getNoPasswordAccess().name());
			writer.endNode();
			
			ToolSerializer.startNode(writer, "languageCodes", List.class);
	        context.convertAnother(corpusMetadata.getLanguageCodes());
	        ToolSerializer.endNode(writer);
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader,
				UnmarshallingContext context) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	public FlexibleParameters getFlexibleParameters() {
		return parameters;
	}

}

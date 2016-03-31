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
package org.voyanttools.trombone.input.extract;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.DefaultHtmlMapper;
import org.apache.tika.parser.html.HtmlMapper;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.LangDetector;
import org.xml.sax.SAXException;

/**
 * @author sgs
 *
 */
public class TikaExtractor implements Extractor {
	
	private ParseContext context;
	private Parser parser;
	private Detector detector;
	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	private FlexibleParameters parameters;
	
	TikaExtractor(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
		context = new ParseContext();
		detector = new DefaultDetector();
		parser = new AutoDetectParser(detector);
		context.set(Parser.class, parser);
		context.set(HtmlMapper.class, new CustomHtmlMapper());
	}

	public InputSource getExtractableInputSource(StoredDocumentSource storedDocumentSource) throws IOException {
		return new ExtractableTikaInputSource(DigestUtils.md5Hex(storedDocumentSource.getId()+"tika-extracted"), storedDocumentSource);
	}

	private class CustomHtmlMapper extends DefaultHtmlMapper {
		
		@Override
		public String mapSafeElement(String name) {
			return name.toLowerCase();
		}

		@Override
		public String mapSafeAttribute(String elementName, String attributeName) {
			return attributeName.toLowerCase();
		}

		public boolean isDiscardElement(String name) {
			return super.isDiscardElement(name) || name.equalsIgnoreCase("iframe");
		}
		
	}
	
	private class ExtractableTikaInputSource implements InputSource {
		
		private String id;
		private StoredDocumentSource storedDocumentSource;
		private DocumentMetadata metadata;
		private boolean isProcessed = false;
		
		private ExtractableTikaInputSource(String id, StoredDocumentSource storedDocumentSource) {
			this.id = id;
			this.storedDocumentSource = storedDocumentSource;
			this.metadata = storedDocumentSource.getMetadata();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			org.apache.tika.metadata.Metadata extractedMetadata = new org.apache.tika.metadata.Metadata();
	
// added this to override poor unicode detection for non-ascii characters, but upgrade of tika have resolved the issue
//			if (metadata.getDocumentFormat()==DocumentFormat.TEXT) {
//				extractedMetadata.set(Metadata.CONTENT_TYPE, MediaType.TEXT_PLAIN.toString());
//			}
			
	        StringWriter sw = new StringWriter(); 
	        SAXTransformerFactory factory = (SAXTransformerFactory) 
	                 SAXTransformerFactory.newInstance(); 
	        
	        
	        // Try with a document containing various tables and formattings 
	        InputStream input = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
	        
	        // do a first pass to convert various formats to simple HTML
	        try { 
	            TransformerHandler handler = factory.newTransformerHandler(); 
	            handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "html"); 
	            handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes"); 
	            handler.setResult(new StreamResult(sw));
	            parser.parse(input, handler, extractedMetadata, context);
	        } catch (Exception e) {
	        	throw new IOException("Unable to parse document: "+storedDocumentSource.getMetadata(), e);
			} finally { 
	            input.close(); 
	        }
	        String extractedContent = sw.toString();

	        
	        for (String name : extractedMetadata.names()) {
	        	String value = extractedMetadata.get(name);
	        	if (value.trim().isEmpty()) {continue;}
	        	if (name.equals("title") || name.equals("dc:title")) {
	        		DocumentFormat f = metadata.getDocumentFormat();
	        		// don't set title if it's already there for text or unknown format
	        		if (!metadata.getTitle().isEmpty() && f!=DocumentFormat.UNKNOWN && f!=DocumentFormat.TEXT) {
		            	metadata.setTitle(value);
	        		}
	        	}
	        	else if (name.toLowerCase().equals("meta:author") || name.toLowerCase().equals("author")) {
	        		metadata.setAuthor(value);
	        	}
	        	else if (name.toLowerCase().equals("keywords")) {
	        		metadata.setKeywords(value);
	        	}
	        	else {
	        		metadata.setExtra(name, value);
	        	}
	        }
	        
	        // now extract the body from the simple HTML – we should be able to cheat since we already have processed content
	        int start = extractedContent.indexOf("<body");
	        int end = extractedContent.indexOf("</body");
	        if (start > -1 && end > start) {
	        	int startend = extractedContent.indexOf('>', start)+1;
	        	if (startend>start && startend<end) {
		        	extractedContent = extractedContent.substring(startend+1, end);
	        	}
	        }
	        
	        DocumentFormat format = storedDocumentSource.getMetadata().getDocumentFormat();
	        if (format==DocumentFormat.PDF) {
	        	extractedContent = extractedContent.replaceAll("\\s+\\&\\#xD;\\s+", " ");
	        	extractedContent = extractedContent.replaceAll("\\s+&nbsp;", " ");
	        	extractedContent = extractedContent.replaceAll("<p/>", "");
	        }
	        else if (format==DocumentFormat.TEXT || format==DocumentFormat.UNKNOWN) {
	        	extractedContent = extractedContent.replaceAll("&#xD;</p>", "</p>");
	        	extractedContent = extractedContent.replaceAll("&#xD;&#xD;+", "</p>\n      <p>");
	        	extractedContent = extractedContent.replaceAll("&#xD;", "<br />\n      ");
	        }
	        
			String lang = LangDetector.langDetector.detect(extractedContent);
			metadata.setLanguageCode(lang);
	        
	        if (parameters.containsKey("inputRemoveUntil")) {
	        	Pattern pattern = Pattern.compile(parameters.getParameterValue("inputRemoveUntil"));
	        	Matcher matcher = pattern.matcher(extractedContent);
	        	if (matcher.find()) {
	        		extractedContent = extractedContent.substring(matcher.start());
	        	}
	        }
	        if (parameters.containsKey("inputRemoveUntilAfter")) {
	        	Pattern pattern = Pattern.compile(parameters.getParameterValue("inputRemoveUntilAfter"));
	        	Matcher matcher = pattern.matcher(extractedContent);
	        	if (matcher.find()) {
	        		extractedContent = extractedContent.substring(matcher.end());
	        	}
	        }
	        if (parameters.containsKey("inputRemoveFrom")) {
	        	Pattern pattern = Pattern.compile(parameters.getParameterValue("inputRemoveFrom"));
	        	Matcher matcher = pattern.matcher(extractedContent);
	        	if (matcher.find()) {
	        		extractedContent = extractedContent.substring(0, matcher.start()-1);
	        	}
	        }
	        if (parameters.containsKey("inputRemoveFromAfter")) {
	        	Pattern pattern = Pattern.compile(parameters.getParameterValue("inputRemoveFromAfter"));
	        	Matcher matcher = pattern.matcher(extractedContent);
	        	if (matcher.find()) {
	        		extractedContent = extractedContent.substring(0, matcher.end());
	        	}
	        }
	        
	        isProcessed = true;
	        
	        return new ByteArrayInputStream(extractedContent.getBytes("UTF-8"));
		}

		@Override
		public DocumentMetadata getMetadata() throws IOException {
			
			return isProcessed ? this.metadata : storedDocumentSourceStorage.getStoredDocumentSourceMetadata(id);
		}

		@Override
		public String getUniqueId() {
			return this.id;
		}
		
	}

}

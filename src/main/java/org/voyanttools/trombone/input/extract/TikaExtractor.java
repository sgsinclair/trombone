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

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.DefaultHtmlMapper;
import org.apache.tika.parser.html.HtmlMapper;
import org.voyanttools.trombone.document.DocumentFormat;
import org.voyanttools.trombone.document.Metadata;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.InputStreamInputSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.xml.sax.SAXException;

/**
 * @author sgs
 *
 */
public class TikaExtractor {
	
	private ParseContext context;
	private Parser parser;
	private Detector detector;
	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	
	TikaExtractor(StoredDocumentSourceStorage storedDocumentSourceStorage) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		context = new ParseContext();
		detector = new DefaultDetector();
		parser = new AutoDetectParser(detector);
		context.set(Parser.class, parser);
		context.set(HtmlMapper.class, new CustomHtmlMapper());
	}

	InputSource getInputSource(StoredDocumentSource storedDocumentSource) throws IOException {
		String id = storedDocumentSource.getId();
		Metadata metadata = storedDocumentSource.getMetadata();
		org.apache.tika.metadata.Metadata extractedMetadata = new org.apache.tika.metadata.Metadata();
		
        StringWriter sw = new StringWriter(); 
        SAXTransformerFactory factory = (SAXTransformerFactory) 
                 SAXTransformerFactory.newInstance(); 
        
        // Try with a document containing various tables and formattings 
        InputStream input = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(id);
        try { 
            TransformerHandler handler = factory.newTransformerHandler(); 
            handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "html"); 
            handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes"); 
            handler.setResult(new StreamResult(sw));
            parser.parse(input, handler, extractedMetadata, context);
        } catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TikaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally { 
            input.close(); 
        }
        
        for (String name : extractedMetadata.names()) {
        	String value = extractedMetadata.get(name);
        	if (value.trim().isEmpty()) {continue;}
        	if (name.equals("title") || name.equals("dc:title")) {
            	metadata.setTitle(value);
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
        
        String extractedContent = sw.toString();
        if (storedDocumentSource.getMetadata().getDocumentFormat()==DocumentFormat.PDF) {
        	extractedContent = sw.toString().replaceAll("\\s+\\&\\#xD;\\s+", " ");
        	extractedContent = extractedContent.replaceAll("\\s+&nbsp;", " ");
        	extractedContent = extractedContent.replaceAll("<p/>", "");
        }
        
        return new InputStreamInputSource(id+"-extract", metadata, new ByteArrayInputStream(extractedContent.getBytes("UTF-8")));
	}

	private class CustomHtmlMapper extends DefaultHtmlMapper {
		public boolean isDiscardElement(String name) {
			return super.isDiscardElement(name) || name.equalsIgnoreCase("iframe");
		}
		
	}
}

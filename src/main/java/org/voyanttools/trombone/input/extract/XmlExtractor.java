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

import it.svario.xpathapi.jaxp.XPathAPI;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathException;

import net.sf.saxon.lib.NamespaceConstant;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.xml.XMLParser;
import org.voyanttools.trombone.document.DocumentFormat;
import org.voyanttools.trombone.document.Metadata;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.StoredDocumentSourceInputSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

/**
 * @author sgs
 *
 */
public class XmlExtractor implements Extractor {

	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	private FlexibleParameters parameters;
	
	/**
	 * the Transformer used to produce XML output from nodes
	 */
	private Transformer transformer;

	public XmlExtractor(
			StoredDocumentSourceStorage storedDocumentSourceStorage,
			FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new IllegalStateException(
					"Unable to create XML transformer.", e);
		}
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		// for some reason XPathAPI doesn't work properly with the default
		// XPathFactory, so we'll use Saxon
		System.setProperty("javax.xml.xpath.XPathFactory:"
				+ NamespaceConstant.OBJECT_MODEL_SAXON,
				"net.sf.saxon.xpath.XPathFactoryImpl");
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.input.extract.Extractor#getInputSource(org.voyanttools.trombone.document.StoredDocumentSource)
	 */
	@Override
	public InputSource getExtractableInputSource(StoredDocumentSource storedDocumentSource)
			throws IOException {
		
		
		if (parameters.getParameterValue("inputFormat","").isEmpty()==false) {
			DocumentFormat format = DocumentFormat.valueOf(parameters.getParameterValue("inputFormat").toUpperCase());
			Map<String, String> defaultsMap = new HashMap<String, String>();
			switch(format) {
			case RSS:
				defaultsMap.put("xmlContentXpath", "//item/description");
				defaultsMap.put("xmlTitleXpath", parameters.getParameterBooleanValue("splitDocuments") ? "//item/title" : "//channel/title");
				defaultsMap.put("xmlAuthorXpath", parameters.getParameterBooleanValue("splitDocuments") ? "//item/author|//item/dc:creator" : "//channel/author|//channel/dc:creator");
				break;
			case ATOM:
				defaultsMap.put("xmlContentXpath", "//summary or //content");
				defaultsMap.put("xmlTitleXpath", "//title");
				defaultsMap.put("xmlAuthorXpath", "//author");
				break;
			case TEI:
			case TEICORPUS:
				defaultsMap.put("xmlContentXpath", "//text");
				defaultsMap.put("xmlTitleXpath", "//teiHeader//title");
				defaultsMap.put("xmlAuthorXpath", "//teiHeader//author");
				break;
			}
			
			// update parameters
			for (Map.Entry<String, String> entry : defaultsMap.entrySet()) {
				String key = entry.getKey();
				if (parameters.getParameterValue(key,"").isEmpty()==true) {
					parameters.setParameter(key, entry.getValue());
				}
			}
		}
		
		String[] relevantParameters = new String[]{"xmlContentXpath","xmlTitleXpath","xmlAuthorXpath"};
		StringBuilder parametersBuilder = new StringBuilder();
		for (String p : relevantParameters) {
			if (parameters.getParameterValue(p, "").isEmpty()==false) {
				parametersBuilder.append(p).append(parameters.getParameterValue(p));
			}
		}
		
		// no special parameters and nothing to extract from XML, so just return the original stored document
		if (parametersBuilder.length()==0) {
			return new StoredDocumentSourceInputSource(storedDocumentSourceStorage, storedDocumentSource);
		}
		
		return new ExtractableXmlInputSource(DigestUtils.md5Hex(storedDocumentSource.getId()+relevantParameters), storedDocumentSource);
	}

	private class ExtractableXmlInputSource implements InputSource {
		
		
		private String id;
		
		private String storedDocumentSourceId;

		private StoredDocumentSource storedDocumentSource;
		
		private Metadata metadata;
		
		private boolean isProcessed = false;
		
		private ExtractableXmlInputSource(String id, StoredDocumentSource storedDocumentSource) {
			this.id = id;
			this.storedDocumentSourceId = storedDocumentSource.getId();
			this.storedDocumentSource = storedDocumentSource;
			this.metadata = storedDocumentSource.getMetadata();
		}

		@Override
		public InputStream getInputStream() throws IOException {

			InputStream inputStream = null;
			Document doc;
			try {

				inputStream = storedDocumentSourceStorage
						.getStoredDocumentSourceInputStream(storedDocumentSourceId);
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				doc = builder.parse(inputStream);

			} catch (ParserConfigurationException e) {
				throw new IOException("Error with XML parser configuration for "
						+ storedDocumentSource, e);
			} catch (SAXException e) {
				throw new IOException("Error with XML parsing for "
						+ storedDocumentSource, e);
			} finally {
				if (inputStream != null)
					inputStream.close();
			}
			
			// try to find title if needed
			String title = getNodesAsStringFromParametersValue(doc, "xmlTitleXpath");
			if (title.isEmpty()==false) {
				metadata.setTitle(title);
			}

			// try to find title if needed
			String author = getNodesAsStringFromParametersValue(doc, "xmlAuthorXpath");
			if (author.isEmpty()==false) {
				metadata.setAuthor(author);
			}

			String xmlContentXpath = parameters.getParameterValue("xmlContentXpath","");
			// we don't need to extract content from the source, so just use the source XML
			
			if (xmlContentXpath.isEmpty()) {
				return storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSourceId);
			}
			
			List<Node> docs;
			try {
				docs = XPathAPI.selectListOfNodes(doc.getFirstChild(), xmlContentXpath, doc.getFirstChild());
			} catch (XPathException e) {
				throw new IllegalArgumentException(
						"A problem was encountered proccesing this XPath query: " + xmlContentXpath, e);
			}
			
			Node newParentNode;
			
			// just use the single node as root
			if (docs.size()==1) {
				newParentNode = docs.get(0);
			}
			
			// encapsulate child nodes in document root
			else {
				newParentNode = doc.getFirstChild().cloneNode(false);
				for (Node node : docs) {
					newParentNode.appendChild(node);
				}
			}
			

			StringWriter sw = new StringWriter(); // no need to close
			Result streamResult = new StreamResult(sw);
			try {
				
				transformer.transform(new DOMSource(newParentNode), streamResult);
			} catch (TransformerException e) {
				throw new IOException(
						"Unable to transform node during XML extraction: "+storedDocumentSource);
			}
	
			String string = sw.toString();
			byte[] bytes = string.getBytes("UTF-8");
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
	        try {
	        	Properties p = System.getProperties();
				com.cybozu.labs.langdetect.Detector detector = DetectorFactory.create();
				byteArrayInputStream.mark(0);
				String text = new Tika(new DefaultDetector(), new XMLParser()).parseToString(byteArrayInputStream);
				byteArrayInputStream.reset();
				detector.append(text);
				String lang = detector.detect();
				metadata.setLanguageCode(lang);
			} catch (LangDetectException e) {
				throw new IOException("Unable to detect language", e);
			} catch (TikaException e) {
				throw new IOException("Unable to extract text for language detection", e);
			}
	        
	        isProcessed = true;

	        return new ByteArrayInputStream(bytes);
			
		}

		private String getNodesAsStringFromParametersValue(Node doc, String parameterKey) {
			String xpath = parameters.getParameterValue(parameterKey,"");
			if (xpath.isEmpty()==false) {
				List<String> titles;
				try {
					titles = XPathAPI.selectNodeListAsStrings(doc.getFirstChild(), xpath);
				}
				catch (XPathException e) {
					throw new IllegalArgumentException(
							"A problem was encountered proccesing this XPath query: " + xpath, e);
				}
				Set set = new HashSet(titles); // eliminate duplicates
				return StringUtils.join(titles,", ").trim();
			}
			return "";
		}

		@Override
		public Metadata getMetadata() throws IOException {
			return isProcessed ? this.metadata : storedDocumentSourceStorage.getStoredDocumentSourceMetadata(id);
		}

		@Override
		public String getUniqueId() {
			return this.id;
		}
	}
	

}

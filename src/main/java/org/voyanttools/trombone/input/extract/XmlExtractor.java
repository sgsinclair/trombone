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

//import it.svario.xpathapi.jaxp.XPathAPI;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.exception.TikaException;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.xpath.XPathFactoryImpl;

/**
 * @author sgs
 *
 */
public class XmlExtractor implements Extractor, Serializable {
	
	
	private static final long serialVersionUID = -8659873836740839314L;
	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	private FlexibleParameters parameters;
	
	/**
	 * the Transformer used to produce XML output from nodes
	 */
	private Transformer transformer;
	
	private XPathFactory xpathFactory;

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
		
		xpathFactory = new XPathFactoryImpl();
		
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
			Map<String, String[]> defaultsMap = new HashMap<String, String[]>();
			switch(format) {
			case RSS:
			case RSS2:
				defaultsMap.put("xmlContentXpath", new String[]{"//item/description"});
				defaultsMap.put("xmlTitleXpath", new String[]{parameters.getParameterBooleanValue("splitDocuments") ? "//item/title" : "//channel/title"});
				defaultsMap.put("xmlAuthorXpath", new String[]{parameters.getParameterBooleanValue("splitDocuments") ? "//item/author|//item/*[local-name()='creator']" : "//channel/author|//channel/*[local-name()='creator']"});
				break;
			case ATOM:
				defaultsMap.put("xmlContentXpath", new String[]{"//summary or //content"});
				defaultsMap.put("xmlTitleXpath", new String[]{"//title"});
				defaultsMap.put("xmlAuthorXpath", new String[]{"//author"});
				break;
			case TEI:
			case TEICORPUS:
				defaultsMap.put("xmlContentXpath", new String[]{"//*[local-name()='text']"});
				defaultsMap.put("xmlTitleXpath", new String[]{"//*[local-name()='teiHeader']//*[local-name()='title']"});
				defaultsMap.put("xmlAuthorXpath", new String[]{"//*[local-name()='teiHeader']//*[local-name()='author']"});
				break;
			case EEBODREAM:
				defaultsMap.put("xmlContentXpath", new String[]{"/EEBO/TEXTS/TEXT"});
//				defaultsMap.put("xmlTitleXpath", "/EEBO/HEADER/FILEDESC/SOURCEDESC/BIBLFULL/TITLESTMT/TITLE[1]");
//				defaultsMap.put("xmlAuthorXpath", "/EEBO/HEADER/FILEDESC/SOURCEDESC/BIBLFULL/TITLESTMT/AUTHOR");
//				defaultsMap.put("xmlPubPlaceXpath", "/EEBO/HEADER/FILEDESC/SOURCEDESC/BIBLFULL/PUBLICATIONSTMT/PUBPLACE");
//				defaultsMap.put("xmlPublisherXpath", "/EEBO/HEADER/FILEDESC/SOURCEDESC/BIBLFULL/PUBLICATIONSTMT/PUBLISHER");
				defaultsMap.put("xmlTitleXpath", new String[]{"/EEBO/dreamheader/opendata/teiHeader/fileDesc/titleStmt/title|/EEBO/dreamheader/tcpdata/fileDesc/titleStmt/title"});
				defaultsMap.put("xmlAuthorXpath", new String[]{"/EEBO/dreamheader/opendata/teiHeader/fileDesc/titleStmt/author/persName|/EEBO/dreamheader/tcpdata/fileDesc/titleStmt/author"});
				defaultsMap.put("xmlPubDateXpath", new String[]{"/EEBO/dreamheader/opendata/teiHeader/fileDesc/publicationStmt/date|/EEBO/dreamheader/tcpdata/fileDesc/publicationStmt/date"});
				defaultsMap.put("xmlPubPlaceXpath", new String[]{"/EEBO/dreamheader/opendata/teiHeader/fileDesc/publicationStmt/pubPlace|/EEBO/dreamheader/tcpdata/fileDesc/publicationStmt/pubPlace"});
				defaultsMap.put("xmlPublisherXpath", new String[]{"/EEBO/dreamheader/tcpdata/fileDesc/publicationStmt/publisher"}); // don't look at opendata part for now because publisher may be there but with child nodes
				defaultsMap.put("xmlExtraMetadataXpath", new String[]{
					"authorSex=/EEBO/dreamheader/opendata/teiHeader/fileDesc/titleStmt/author/sex",
					"authorLink=/EEBO/dreamheader/opendata/teiHeader/fileDesc/titleStmt/author/idno[@type eq 'viaf']/@ref",
					"titleLink=(/EEBO/dreamheader/opendata/teiHeader/fileDesc/sourceDesc/ref[@type eq 'oclc']/@target)[1]",
					"tcpId=/EEBO/dreamheader/opendata/teiHeader/fileDesc/idno[@type eq 'tcp']"
				});
				defaultsMap.put("xmlExtractorTemplate", new String[]{"dream-extraction.xsl"});
				break;
			case DTOC:
				defaultsMap.put("xmlTitleXpath", new String[]{"(//head//title)[1]"});
				defaultsMap.put("xmlAuthorXpath", new String[]{"(//head//author)[1]"});
				break;
			}
			
			// update parameters
			for (Map.Entry<String, String[]> entry : defaultsMap.entrySet()) {
				String key = entry.getKey();
				if (parameters.getParameterValue(key,"").isEmpty()==true) {
					parameters.setParameter(key, entry.getValue());
				}
			}
		}
		
		String[] relevantParameters = new String[]{"xmlContentXpath","xmlTitleXpath","xmlAuthorXpath","xmlPubPlaceXpath","xmlPublisherXpath","xmlPubDateXpath","xmlExtraMetadataXpath"};
		StringBuilder parametersBuilder = new StringBuilder();
		for (String p : relevantParameters) {
			if (parameters.getParameterValue(p, "").isEmpty()==false) {
				parametersBuilder.append(p);
				for (String s : parameters.getParameterValues(p)) {
					parametersBuilder.append(s);
				}
			}
		}
		
		/* This was skipped, but we probably need to extract anyway to strip XML comments, detect language, etc.
		 * 
		// no special parameters and nothing to extract from XML, so just return the original stored document
		if (parametersBuilder.length()==0) {
			return new StoredDocumentSourceInputSource(storedDocumentSourceStorage, storedDocumentSource);
		}
		*/
		
		return new ExtractableXmlInputSource(DigestUtils.md5Hex(storedDocumentSource.getId()+relevantParameters+String.valueOf(serialVersionUID)), storedDocumentSource);
	}

	private class ExtractableXmlInputSource implements InputSource {
		
		
		private String id;
		
		private String storedDocumentSourceId;

		private StoredDocumentSource storedDocumentSource;
		
		private DocumentMetadata metadata;
		
		private boolean isProcessed = false;
		
		private ExtractableXmlInputSource(String id, StoredDocumentSource storedDocumentSource) {
			this.id = id;
			this.storedDocumentSourceId = storedDocumentSource.getId();
			this.storedDocumentSource = storedDocumentSource;
			this.metadata = storedDocumentSource.getMetadata().asParent(storedDocumentSourceId);
			this.metadata.setLocation(storedDocumentSource.getMetadata().getLocation());
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
				factory.setFeature("http://xml.org/sax/features/validation", false);
				factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
				factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
				factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
				factory.setIgnoringComments(true);
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
			
			if (parameters.containsKey("xmlExtractorTemplate")) {

				Source source = null;
				String xmlExtractorTemplate = parameters.getParameterValue("xmlExtractorTemplate");
			
				URI templateUrl;
				try {
					templateUrl = this.getClass().getResource("/org/voyanttools/trombone/templates/"+xmlExtractorTemplate).toURI();
				} catch (URISyntaxException e1) {
					throw new IOException("Unable to find local template directory", e1);
				}
				File file = new File(templateUrl);
				if (file.exists()) {
					source = new StreamSource(file);
				}
				
				if (source!=null) {
					DOMResult result = new DOMResult();
					try {
						Transformer extractorTransformer = TransformerFactory.newInstance().newTransformer(source);
						extractorTransformer.transform(new DOMSource(doc), result);
					} catch (TransformerException e) {
						throw new IOException("Unable to transform document during expansion "+metadata, e);
					}
					try {
						doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
					} catch (ParserConfigurationException e) {
						throw new IllegalStateException("Unable to create new XML document during templated extraction.", e);
					}
					doc = (Document) result.getNode();
//					   DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
//					    LSSerializer lsSerializer = domImplementation.createLSSerializer();
//					    System.out.println(lsSerializer.writeToString(doc).substring(0, 3000));
				}
				else {
					throw new IOException("Unable to find extractor template "+xmlExtractorTemplate);
				}
			}
			
			
			// try to find title if needed
			String[] titles = getNodesAsStringsFromParametersValue(doc, "xmlTitleXpath");
			if (titles.length>0) {
				metadata.setTitles(titles);
			}
			
			// try to find author if needed
			String[] authors = getNodesAsStringsFromParametersValue(doc, "xmlAuthorXpath");
			if (authors.length>0) {
				metadata.setAuthors(authors);
			}

			// try to find publplace if needed
			String[] pubPlaces = getNodesAsStringsFromParametersValue(doc, "xmlPubPlaceXpath");
			if (pubPlaces.length>0) {
				metadata.setPubPlaces(pubPlaces);
			}

			// try to find title if needed
			String[] publishers = getNodesAsStringsFromParametersValue(doc, "xmlPublisherXpath");
			if (publishers.length>0) {
				metadata.setPublishers(publishers);
			}

			// try to find title if needed
			String[] pubDates = getNodesAsStringsFromParametersValue(doc, "xmlPubDateXpath");
			if (pubDates.length>0) {
				metadata.setPubDates(pubDates);
			}
			
			for (String extra : parameters.getParameterValues("xmlExtraMetadataXpath")) {
				String[] parts = extra.split("=");
				if (parts.length>1) {
					String key = parts[0].trim();
					String xpath = StringUtils.join(Arrays.copyOfRange(parts, 1, parts.length)).trim();
					String[] values = getNodesAsStringsFromXpath(doc, xpath);
					if (values.length>0) {
						metadata.setExtras(key, values);
					}
				}
			}
			
			// if no XPath is defined, consider the whole source XML (but allow for additional metadata ot be identified
			String xmlContentXpath = parameters.getParameterValue("xmlContentXpath","/");
			
			NodeList nodeList;
			XPath xpath = xpathFactory.newXPath();
			try {
				nodeList = (NodeList) xpath.evaluate(xmlContentXpath, doc.getDocumentElement(), XPathConstants.NODESET);
			} catch (XPathExpressionException e) {
				throw new IllegalArgumentException(
						"A problem was encountered proccesing this XPath query: " + xmlContentXpath, e);
			}
			
			Node newParentNode;
			// just use the single node as root
			if (nodeList.getLength()==1) {
				newParentNode = nodeList.item(0);
			}
			
			// encapsulate child nodes in document root
			else {
				newParentNode = doc.getDocumentElement().cloneNode(false);
				for (int i=0, len=nodeList.getLength(); i<len; i++) {
					newParentNode.appendChild(nodeList.item(i));
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
//			String string = StringEscapeUtils.unescapeXml(sw.toString());
//			byte[] bytes = string.getBytes("UTF-8");
//			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
	        try {
	        	Properties p = System.getProperties();
				com.cybozu.labs.langdetect.Detector detector = DetectorFactory.create();
				// use default detector and parser
				String text = new Tika(new DefaultDetector(), new XmlOrHtmlTikaParser()).parseToString(new ByteArrayInputStream(string.getBytes("UTF-8")));
				if (text.trim().isEmpty()==false) {
					detector.append(text);
					String lang = detector.detect();
					metadata.setLanguageCode(lang);
				}
			} catch (LangDetectException e) {
				// fail silently
				if (parameters.getParameterBooleanValue("verbose")) {
					System.out.println("Unable to detect language for "+storedDocumentSource.getMetadata());
				}
			} catch (TikaException e) {
				throw new IOException("Unable to extract text for language detection", e);
			}
	        
	        isProcessed = true;

	        return new ByteArrayInputStream(StringEscapeUtils.unescapeXml(string).getBytes("UTF-8"));
			
		}

		private String[] getNodesAsStringsFromParametersValue(Document doc, String parameterKey) {
			String xpathString = parameters.getParameterValue(parameterKey,"");
			return getNodesAsStringsFromXpath(doc, xpathString);
		}

		private String[] getNodesAsStringsFromXpath(Document doc, String xpathString) {
			String[] strings = new String[0];
			if (xpathString.isEmpty()==false) {
				Set<String> values = new HashSet<String>();
				XPath xpath = xpathFactory.newXPath();
				NodeList nodeList;
				try {
					nodeList = (NodeList) xpath.evaluate(xpathString, doc.getDocumentElement(), XPathConstants.NODESET);
				}
				catch (XPathExpressionException e) {
					throw new IllegalArgumentException(
							"A problem was encountered proccesing this XPath query: " + xpathString, e);
				}
				for (int i=0, len=nodeList.getLength(); i<len; i++) {
					values.add(nodeList.item(i).getTextContent());
				}
				return values.toArray(strings);
			}
			return strings;
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

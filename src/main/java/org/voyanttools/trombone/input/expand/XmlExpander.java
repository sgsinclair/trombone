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
package org.voyanttools.trombone.input.expand;

import it.svario.xpathapi.jaxp.XPathAPI;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

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
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * An expander that looks for sub-documents within an XML document, especially
 * if the {@code xmlDocumentsXpath} parameter is set to a valid XPath
 * expression. The XPath expression should now support namespaces declared in
 * the root element. When a single XPath expression is provided, documents are
 * created from each of the matching {@link Node}s. When multiple XPath
 * expressions are provided, all the nodes matching each XPath expression are
 * combined into one document (so one document per XPath expression). To simply
 * extract all of the content from one XML document into one source document,
 * don't use an expander, use the xmlContentXpath parameter instead (which will
 * be handled by the XML parser).
 * 
 * @author "Stéfan Sinclair"
 */
class XmlExpander implements Expander {

	/**
	 * all parameters sent, only some of which may be relevant to some expanders
	 */
	private FlexibleParameters parameters;

	/**
	 * the stored document storage strategy
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;

	/**
	 * the Transformer used to produce XML output from nodes
	 */
	private Transformer transformer;

	/**
	 * Create a new instance of this expander (this should only be done by
	 * {@link StoredDocumentSourceExpander}.
	 * 
	 * @param storedDocumentSourceStorage
	 *            a stored storage strategy
	 * @param parameters
	 *            that may be relevant to this expander, including
	 *            {@code xmlDocumentsXapth}
	 */
	XmlExpander(StoredDocumentSourceStorage storedDocumentSourceStorage,
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
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.voyanttools.trombone.input.expand.Expander#
	 * getExpandedStoredDocumentSources
	 * (org.voyanttools.trombone.document.StoredDocumentSource)
	 */
	public List<StoredDocumentSource> getExpandedStoredDocumentSources(
			StoredDocumentSource storedDocumentSource) throws IOException {

		List<StoredDocumentSource> childStoredDocumentSources = new ArrayList<StoredDocumentSource>();

		String[] xmlDocumentsXpaths = parameters
				.getParameterValues("xmlDocumentsXpath");

		// check to see if we need to set xmlDocumentsXpath using defaults for format
		if (xmlDocumentsXpaths.length == 0 && parameters.getParameterBooleanValue("splitDocuments") && parameters.getParameterValue("inputFormat","").isEmpty()==false) {
			DocumentFormat format = DocumentFormat.valueOf(parameters.getParameterValue("inputFormat").toUpperCase());
			switch (format) {
			case RSS:
				xmlDocumentsXpaths = new String[]{"//item"}; break;
			case ATOM:
				xmlDocumentsXpaths = new String[]{"//entry"}; break;
			case TEICORPUS:
				xmlDocumentsXpaths = new String[]{"//TEI"}; break;
			}
		}

		String joinedXmlDocumentsXpaths = StringUtils.join(xmlDocumentsXpaths);
		
		
		if (xmlDocumentsXpaths.length == 0) {			
			childStoredDocumentSources.add(storedDocumentSource);
			return childStoredDocumentSources;
		}

		DocumentMetadata parentMetadata = storedDocumentSource.getMetadata();
		String parentId = storedDocumentSource.getId();
		String multipleExpandedStoredDocumentSourcesPrefix = DigestUtils
				.md5Hex(joinedXmlDocumentsXpaths);
		childStoredDocumentSources = storedDocumentSourceStorage
				.getMultipleExpandedStoredDocumentSources(parentId,
						multipleExpandedStoredDocumentSourcesPrefix);
		if (childStoredDocumentSources != null
				&& childStoredDocumentSources.isEmpty() == false) {
			return childStoredDocumentSources;
		}

		// for some reason XPathAPI doesn't work properly with the default
		// XPathFactory, so we'll use Saxon
		System.setProperty("javax.xml.xpath.XPathFactory:"
				+ NamespaceConstant.OBJECT_MODEL_SAXON,
				"net.sf.saxon.xpath.XPathFactoryImpl");

		InputStream inputStream = null;
		Document doc;
		try {

			inputStream = storedDocumentSourceStorage
					.getStoredDocumentSourceInputStream(storedDocumentSource
							.getId());
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setFeature("http://xml.org/sax/features/validation", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
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

		// each node is a separate document
		if (xmlDocumentsXpaths.length == 1) {
			childStoredDocumentSources.addAll(getChildStoredDocumentSources(
					doc, xmlDocumentsXpaths[0], parentId, parentMetadata));
		}

		// each xpath is a separate document
		else {
			childStoredDocumentSources.addAll(getChildStoredDocumentSources(
					doc, xmlDocumentsXpaths, parentId, parentMetadata));
		}

		storedDocumentSourceStorage.setMultipleExpandedStoredDocumentSources(
				parentId, childStoredDocumentSources,
				multipleExpandedStoredDocumentSourcesPrefix);
		return childStoredDocumentSources;
	}

	/**
	 * Get a list of stored document sources. Matching nodes for each XPath
	 * expression are concatenated into a single document (one document per
	 * XPath).
	 * 
	 * @param doc
	 *            the {@link Document} to be searched
	 * @param xmlDocumentsXpaths
	 *            the list of XPath expressions to find nodes
	 * @param parentId
	 *            the ID of the stored parent document
	 * @param parentMetadata
	 *            the metadata of the stored parent document
	 * @return a list of {@link StoredDocumentSource}s
	 * @throws IOException
	 *             an exception that occurs during processing
	 */
	private List<StoredDocumentSource> getChildStoredDocumentSources(
			Document doc, String[] xmlDocumentsXpaths, String parentId,
			DocumentMetadata parentMetadata) throws IOException {
		List<StoredDocumentSource> childStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		for (int i = 0, len = xmlDocumentsXpaths.length; i < len; i++) {

			List<Node> docs;
			try {
				docs = XPathAPI.selectListOfNodes(doc.getDocumentElement(),
						xmlDocumentsXpaths[i], doc.getDocumentElement());
			} catch (XPathException e) {
				throw new IllegalArgumentException(
						"A problem was encountered proccesing this XPath query: "
								+ xmlDocumentsXpaths[i], e);
			}
			if (docs.isEmpty()) {
				continue;
			}
			Node newParentNode = doc.getDocumentElement().cloneNode(false);
			for (Node node : docs) {
				newParentNode.appendChild(node);
			}
			StoredDocumentSource childStoredDocumentSource = getChildStoredDocumentSource(
					newParentNode, parentId, parentMetadata,
					xmlDocumentsXpaths[i] + "[" + (i) + "]");
			childStoredDocumentSources.add(childStoredDocumentSource);
		}
		return childStoredDocumentSources;
	}

	/**
	 * Get a list of stored document sources. Each node matching the specified
	 * XPath expression becomes a separate document.
	 * 
	 * @param doc
	 *            the {@link Document} to be searched
	 * @param xmlDocumentsXpath
	 *            the XPath expressions to find nodes
	 * @param parentId
	 *            the ID of the stored parent document
	 * @param parentMetadata
	 *            the metadata of the stored parent document
	 * @return a list of {@link StoredDocumentSource}s
	 * @throws IOException
	 *             an exception that occurs during processing
	 */
	private List<StoredDocumentSource> getChildStoredDocumentSources(
			Document doc, String xmlDocumentsXpath, String parentId,
			DocumentMetadata parentMetadata) throws IOException {
		List<StoredDocumentSource> childStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		List<Node> docs;
		try {
			docs = XPathAPI.selectListOfNodes(doc.getDocumentElement(),
					xmlDocumentsXpath, doc.getDocumentElement());
		} catch (XPathException e) {
			throw new IllegalArgumentException(
					"A problem was encountered proccesing this XPath query: "
							+ xmlDocumentsXpath, e);
		}
		for (int i = 0, len = docs.size(); i < len; i++) {
			StoredDocumentSource childStoredDocumentSource = getChildStoredDocumentSource(
					docs.get(i), parentId, parentMetadata, xmlDocumentsXpath
							+ "[" + (i) + "]");
			childStoredDocumentSources.add(childStoredDocumentSource);

		}
		return childStoredDocumentSources;
	}

	/**
	 * Get a {@link StoredDocumentSource} from the specified {@link Node} and
	 * parent information.
	 * 
	 * @param node
	 *            the {@link Node} from with to produce an XML document
	 * @param parentId
	 *            the ID of the stored parent document
	 * @param parentMetadata
	 *            the metadata of the stored parent document
	 * @param location
	 *            the approximate XPath location that can help generate a unique
	 *            identifier
	 * @return a {@link StoredDocumentSource}
	 * @throws IOException
	 *             an exception that occurs during IO processing
	 */
	private StoredDocumentSource getChildStoredDocumentSource(Node node,
			String parentId, DocumentMetadata parentMetadata, String location)
			throws IOException {
		StringWriter sw = new StringWriter(); // no need to close
		Result streamResult = new StreamResult(sw);
		try {
			transformer.transform(new DOMSource(node), streamResult);
		} catch (TransformerException e) {
			throw new IOException(
					"Unable to transform node from stored document: "
							+ parentId + " (" + parentMetadata + ")");
		}
		DocumentMetadata metadata = parentMetadata.asParent();
		metadata.setModified(parentMetadata.getModified());
		metadata.setSource(Source.STRING);
		metadata.setLocation(location);
		metadata.setDocumentFormat(DocumentFormat.XML);
		String id = DigestUtils.md5Hex(parentId + location);
		InputSource inputSource = new StringInputSource(id, metadata,
				sw.toString());
		return storedDocumentSourceStorage
				.getStoredDocumentSource(inputSource);
	}
}

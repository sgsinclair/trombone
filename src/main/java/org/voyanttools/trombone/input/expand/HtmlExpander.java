/**
 * 
 */
package org.voyanttools.trombone.input.expand;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.JsoupHelper;

/**
 * @author sgs
 *
 */
public class HtmlExpander implements Expander {

	/**
	 * all parameters sent, only some of which may be relevant to some expanders
	 */
	private FlexibleParameters parameters;

	/**
	 * the stored document storage strategy
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;

	/**
	 * 
	 */
	public HtmlExpander(StoredDocumentSourceStorage storedDocumentSourceStorage,
			FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.input.expand.Expander#getExpandedStoredDocumentSources(org.voyanttools.trombone.model.StoredDocumentSource)
	 */
	@Override
	public List<StoredDocumentSource> getExpandedStoredDocumentSources(StoredDocumentSource storedDocumentSource)
			throws IOException {
		
		List<StoredDocumentSource> childStoredDocumentSources = new ArrayList<StoredDocumentSource>();

		// if this query doesn't exist than it's all one document and will be handled by extractor
		if (!parameters.containsKey("htmlDocumentsQuery") || parameters.getParameterValue("htmlDocumentsQuery").trim().isEmpty()) {
			childStoredDocumentSources.add(storedDocumentSource);
			return childStoredDocumentSources;
		}
		
		InputStream inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
		
		Document doc;
		try {
			// TODO: determine character set implications and usefulness of baseUri
			doc = Jsoup.parse(inputStream, null, "");
		} catch (Exception e) {
			throw new IOException("Unable to HTML parse document: "+storedDocumentSource.getId()+" ("+storedDocumentSource.getMetadata().getLocation()+")", e);
		}
		
		// we know we have a query if we've gotten this far
		String selector = parameters.getParameterValue("htmlDocumentsQuery");		
		Elements elements;
		try {
			elements = doc.select(selector);
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse query: "+selector, e);
		}
		
		String groupSelector = parameters.getParameterValue("htmlGroupByQuery","").trim();
		String parentId = storedDocumentSource.getId();
		DocumentMetadata parentMetadata = storedDocumentSource.getMetadata();
		List<String> orderedGroups = new ArrayList<String>();
		Map<String, List<Element>> mappedSources = new HashMap<String, List<Element>>();
		for (int i=0, len=elements.size(); i<len; i++) {
			Element element = elements.get(i);
			if (groupSelector.isEmpty()==false) {
				String string = JsoupHelper.getQueryValue(element, groupSelector);
				if (string!=null && string.isEmpty()==false) {
					if (mappedSources.containsKey(string)==false) {
						mappedSources.put(string, new ArrayList<Element>());
						orderedGroups.add(string);
					}
					mappedSources.get(string).add(element);
					continue; 
				}
			}
			StoredDocumentSource sds = getStoredDocumentSource(parentId, parentMetadata, doc, element, "("+selector+")["+i+"]");
			childStoredDocumentSources.add(sds);
		}
		for (String group : orderedGroups) {
			Element root = null;
			List<Element> els = mappedSources.get(group);
			if (els.isEmpty()) {continue;}
			if (els.size()==1) {root=els.get(0);}
			else if (els.size()>1) {
				root = new Element("div");
				for (Element el : els) {
					root.appendChild(el);
				}
			}
			if (root!=null) {
				StoredDocumentSource sds = getStoredDocumentSource(parentId, parentMetadata, doc, root, selector+" (group: "+group+")");
				childStoredDocumentSources.add(sds);
			}
		}
		return childStoredDocumentSources;
	}
	
	private StoredDocumentSource getStoredDocumentSource(String parentId, DocumentMetadata parentMetadata, Document doc, Element element, String location) throws IOException {
		DocumentMetadata metadata = parentMetadata.asParent(parentId, DocumentMetadata.ParentType.EXPANSION);
		metadata.setModified(parentMetadata.getModified());
		metadata.setSource(Source.STRING);
		metadata.setLocation(location);
		metadata.setDocumentFormat(DocumentFormat.HTML);
		String id = DigestUtils.md5Hex(parentId + location);
		InputSource inputSource = new StringInputSource(id, metadata, element.outerHtml());
		return storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
	}
}

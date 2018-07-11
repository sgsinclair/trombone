/**
 * 
 */
package org.voyanttools.trombone.input.extract;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.JsoupHelper;
import org.voyanttools.trombone.util.LangDetector;

/**
 * @author sgs
 *
 */
public class HtmlExtractor implements Extractor, Serializable {
	
	private static final long serialVersionUID = 1L;
	private static String[] QUERIES = new String[] {"htmlContentQuery","htmlAuthorQuery","htmlTitleQuery","htmlPublisherQuery","htmlPubDateQuery","htmlKeywordQuery","htmlCollectionQuery","htmlExtraMetadataQuery"};
	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	private FlexibleParameters parameters;

	/**
	 * 
	 */
	public HtmlExtractor(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.input.extract.Extractor#getExtractableInputSource(org.voyanttools.trombone.model.StoredDocumentSource)
	 */
	@Override
	public InputSource getExtractableInputSource(StoredDocumentSource storedDocumentSource) throws IOException {
		StringBuilder id = new StringBuilder(storedDocumentSource.getId()).append("html-extracted").append(serialVersionUID);
		// not sure why we can't use all params, but just in case
		for (String param : QUERIES) {
			if (parameters.containsKey(param)) {
				id.append(param).append(parameters.getParameterValue(param));
			}
		}
		return new ExtractableHtmlInputSource(DigestUtils.md5Hex(id.toString()), storedDocumentSource);
	}

	boolean hasQueries() {
		for (String key : QUERIES) {
			if (!parameters.getParameterValue(key, "").isEmpty()) {
				return true;
			}
			
		}
		return false;
	}
	
	private class ExtractableHtmlInputSource implements InputSource {
		private String id;
		private StoredDocumentSource storedDocumentSource;
		private DocumentMetadata metadata;
		private boolean isProcessed = false;


		public ExtractableHtmlInputSource(String id, StoredDocumentSource storedDocumentSource) {
			this.id = id;
			this.storedDocumentSource = storedDocumentSource;
			this.metadata = storedDocumentSource.getMetadata().asParent(id, DocumentMetadata.ParentType.EXTRACTION);
			this.metadata.setLocation(storedDocumentSource.getMetadata().getLocation());
			this.metadata.setDocumentFormat(DocumentFormat.HTML);
		}

		@Override
		public InputStream getInputStream() throws IOException {
			InputStream inputStream = null;
			Document doc;
			try {
				inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
				doc = Jsoup.parse(inputStream, null, "");
				inputStream.close();
			} catch (IOException e) {
				throw new IOException("Unable to read from stored document stream: "+storedDocumentSource.getId()+" ("+storedDocumentSource.getMetadata().getLocation()+")");
			}
			finally {
				if (inputStream != null)
					inputStream.close();
			}
			
			String extractedContent = "";
			
			// use specified query or look for body (could be multiple bodies)
			String htmlContentQuery = parameters.getParameterValue("htmlContentQuery", "body");
			Elements elements = doc.select(htmlContentQuery);			
			// if no content matched and not content query then we may have a document without a body, so use the entire document
			String string;
			if (elements.isEmpty() && parameters.containsKey("htmlContentQuery")==false) {
				extractedContent = doc.outerHtml();
				string = doc.text();
			} else {
				string = elements.text();
				extractedContent = elements.outerHtml();
			}
			
			metadata.setLanguageCode(LangDetector.langDetector.detect(string, parameters));

			
			String value = JsoupHelper.getQueryValue(doc, parameters.getParameterValue("htmlTitleQuery", "head title"));
			if (value!=null && value.isEmpty()==false) {
				metadata.setTitle(value);
			}

			String[] values = JsoupHelper.getQueryValues(doc, parameters.getParameterValue("htmlAuthorQuery", "head meta[name=author]@content"));
			if (values!=null && values.length>0) {
				metadata.setAuthors(values);
			}
			
			values = JsoupHelper.getQueryValues(doc, parameters.getParameterValue("htmlKeywordQuery", "head meta[name=keywords]@content"));
			if (values!=null && values.length>0) {
				metadata.setKeywords(values);
			}

			values = JsoupHelper.getQueryValues(doc, parameters.getParameterValue("htmlPublisherQuery", "head meta[name=publisher]@content"));
			if (values!=null && values.length>0) {
				metadata.setPublishers(values);
			}

			values = JsoupHelper.getQueryValues(doc, parameters.getParameterValue("htmlPubDateQuery", "head meta[name=pubdate]@content"));
			if (values!=null && values.length>0) {
				metadata.setPubDates(values);
			}

			values = JsoupHelper.getQueryValues(doc, parameters.getParameterValue("htmlCollectionQuery", "head meta[name=collection]@content"));
			if (values!=null && values.length>0) {
				metadata.setCollections(values);
			}

			values = JsoupHelper.getQueryValues(doc, parameters.getParameterValue("htmlKeywordQuery", "head meta[name=keywords]@content"));
			if (values!=null && values.length>0) {
				metadata.setAuthors(values);
			}

			
			for (String extra : parameters.getParameterValues("htmlExtraMetadataXpath")) {
				for (String x :extra.split("(\r\n|\r|\n)+")) {
					x = x.trim();
					String[] parts = x.split("=");
					if (parts.length>1) {
						String key = parts[0].trim();
						String query = StringUtils.join(Arrays.copyOfRange(parts, 1, parts.length), "=").trim();
						values = JsoupHelper.getQueryValues(doc, query);
						if (values!=null && values.length>0) {
							metadata.setExtras(key, values);
						}
					}
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

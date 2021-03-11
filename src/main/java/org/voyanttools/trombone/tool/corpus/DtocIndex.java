/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.io.IOUtils;
import org.voyanttools.trombone.input.extract.XmlExtractor;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class DtocIndex extends AbstractTool {
	
	String index = null;

	/**
	 * @param storage
	 * @param parameters
	 */
	public DtocIndex(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.corpus.AbstractCorpusTool#run(org.voyanttools.trombone.lucene.CorpusMapper)
	 */
	@Override
	public void run() throws IOException {
		
		Corpus corpus = CorpusManager.getCorpus(storage, parameters);

		String dtocIndexStringId = corpus.getId()+"-dtoc-index"+getVersion();
		
		// let's not bother cacheing since we have tool cacheing
		index = getIndex(corpus);

	}
	
	private String getIndex(Corpus corpus) throws IOException {
		String string;
		String id = getOriginalDocId(corpus);
		
		if (id != null) {
			StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
			
			DocumentMetadata metadata = storage.getStoredDocumentSourceStorage().getStoredDocumentSourceMetadata(id);
			
			StoredDocumentSource storedDocumentSource = new StoredDocumentSource(id, metadata);
	
			FlexibleParameters indexParams = new FlexibleParameters();
			indexParams.setParameter("xmlContentXpath", "//*[local-name()='div' and @type='index']");
			
			XmlExtractor extractor = new XmlExtractor(storedDocumentSourceStorage, indexParams);
			
			InputSource inputSource;
			
			try {
				inputSource = extractor.getExtractableInputSource(storedDocumentSource);
			} catch (Exception e) {
				throw new IllegalArgumentException("Unable to find index content ("+indexParams.getParameterValue("xmlContentXpath")+") in this document: "+metadata, e);
			}
			
			InputStream inputStream = null;
			
			try {
				inputStream = inputSource.getInputStream();
				string = IOUtils.toString(inputStream);
			}
			catch (IOException e) {
				throw new IOException("Unable to read index ("+indexParams.getParameterValue("xmlContentXpath")+") from DToC file: "+metadata, e);
			}
			finally {
				if (inputStream != null) {
					inputStream.close();
				}
			}
		} else {
			string = "";
		}
		
		return string;
		
	}
	
	private String getOriginalDocId(Corpus corpus) throws IOException {
        // check for a document with the isDtocIndex property
		for (String docId : corpus.getDocumentIds()) {
			IndexedDocument doc = corpus.getDocument(docId);
			String isDtocIndex = doc.getMetadata().getExtra("isDtocIndex");
			if (isDtocIndex != null && isDtocIndex.equals("true")) {
				return docId;
			}
		}
		
		// no index was specified, don't assume default (required for cwrc support)
		return null;
	}


}

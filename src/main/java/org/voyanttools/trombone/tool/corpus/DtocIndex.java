/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.tika.io.IOUtils;
import org.voyanttools.trombone.input.extract.XmlExtractor;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.tool.utils.AbstractTool;
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
		
		String id = getOriginalDocId(corpus);
		
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		
		DocumentMetadata metadata = storage.getStoredDocumentSourceStorage().getStoredDocumentSourceMetadata(id);
		
		StoredDocumentSource storedDocumentSource = new StoredDocumentSource(id, metadata);

		FlexibleParameters indexParams = new FlexibleParameters();
		indexParams.setParameter("xmlContentXpath", "//*[local-name()='div' and @type='index']");
		
		XmlExtractor extractor = new XmlExtractor(storedDocumentSourceStorage, indexParams);
		
		InputSource inputSource = extractor.getExtractableInputSource(storedDocumentSource);
		
		InputStream inputStream = null;
		String string;
		
		try {
			inputStream = inputSource.getInputStream();
			string = IOUtils.toString(inputStream);
		}
		catch (IOException e) {
			throw new IOException("Unable to read index from DToC file.", e);
		}
		finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
		
		return string;
		
	}
	
	private String getOriginalDocId(Corpus corpus) throws IOException {
		Properties properties = corpus.getDocument(0).getMetadata().getProperties();
		// this is the simplest possible scenario where a DTOC XML file was loaded and parsed, we need to handle other scenarios
		String id = (String) properties.get("parent_parent_id");
		return id;
	}


}

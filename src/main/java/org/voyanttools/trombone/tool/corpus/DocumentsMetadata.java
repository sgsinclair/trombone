/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("documentsMetadata")
public class DocumentsMetadata extends AbstractCorpusTool {
	
	List<IndexedDocument> documents = new ArrayList<IndexedDocument>();

	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentsMetadata(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	protected void run(Corpus corpus, StoredToLuceneDocumentsMapper corpusMapper)
			throws IOException {
		for (IndexedDocument doc : corpus) {
			documents.add(doc);
		}
	}

}

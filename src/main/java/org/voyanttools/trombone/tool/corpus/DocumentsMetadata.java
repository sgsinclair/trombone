/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("documentsMetadata")
public class DocumentsMetadata extends AbstractCorpusTool {
	
	private int total = 0;
	private List<IndexedDocument> documents = new ArrayList<IndexedDocument>();

	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentsMetadata(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	protected void run(Corpus corpus) throws IOException {
		total = corpus.size();
		int start = parameters.getParameterIntValue("start", 0);
		int limit = parameters.getParameterIntValue("limit", Integer.MAX_VALUE);
		int index = 0;
		for (IndexedDocument doc : corpus) {
			if (index>=start && documents.size()<limit) {
				documents.add(doc);
			}
			index++;
		}
	}

}

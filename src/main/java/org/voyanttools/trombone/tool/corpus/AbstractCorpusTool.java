/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.search.IndexSearcher;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public abstract class AbstractCorpusTool extends AbstractTool {

	public AbstractCorpusTool(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.utils.RunnableTool#run()
	 */
	@Override
	public void run() throws IOException {
		Corpus corpus = CorpusManager.getCorpus(storage, parameters);
		run(corpus);
//		StoredToLuceneDocumentsMapper corpusMapper = new StoredToLuceneDocumentsMapper(storage, corpus.getDocumentIds());
//		run(corpus, corpusMapper);
	}
	
	protected List<String> getCorpusStoredDocumentIdsFromParameters(Corpus corpus) throws IOException {
		
		List<String> ids = new ArrayList<String>();
		
		// add IDs
		for (String docId : parameters.getParameterValues("docId")) {
			if (docId.isEmpty()==false) {
				ids.add(docId);
			}
		}
		
		// add indices
		// check first if we have real values
		String[] docIndices = parameters.getParameterValues("docIndex");
		if (docIndices.length>0 && docIndices[0].isEmpty()==false) {
			for (int docIndex : parameters.getParameterIntValues("docIndex")) {
				ids.add(corpus.getDocument(docIndex).getId());
			}
		}
		
		// no docs defined, so consider all
		if (ids.isEmpty()) {
			ids.addAll(corpus.getDocumentIds());
		}
		
		return ids;
		
	}
	
	protected CorpusMapper getStoredToLuceneDocumentsMapper(Corpus corpus) throws IOException {
		
		AtomicReader reader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getDirectoryReader());
		return new CorpusMapper(storage, corpus);
	}
	
	public abstract void run(Corpus corpus) throws IOException;
//	protected abstract void run(Corpus corpus, StoredToLuceneDocumentsMapper corpusMapper) throws IOException;


}

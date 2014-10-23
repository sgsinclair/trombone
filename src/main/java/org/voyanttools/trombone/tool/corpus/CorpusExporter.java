/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.io.Writer;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.search.IndexSearcher;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class CorpusExporter extends AbstractCorpusTool {

	private Corpus corpus = null;
	
	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusExporter(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.corpus.AbstractCorpusTool#run(org.voyanttools.trombone.model.Corpus)
	 */
	@Override
	protected void run(Corpus corpus) throws IOException {
		this.corpus = corpus;
	}

	public void run(Corpus corpus, Writer writer) throws IOException {
		AtomicReader reader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getDirectoryReader());
		StoredToLuceneDocumentsMapper corpusMapper = getStoredToLuceneDocumentsMapper(new IndexSearcher(reader), corpus);
		for (String id : corpus.getDocumentIds()) {
			String document = reader.document(corpusMapper.getLuceneIdFromDocumentId(id)).get(TokenType.lexical.name());
		}
	}

}

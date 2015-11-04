/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.List;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.DocumentNgram;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class DocumentNgramsTree extends DocumentNgrams {

	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentNgramsTree(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		super.runQueries(corpusMapper, stopwords, queries);
		createTree(getNgrams());
	}

	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		super.runAllTerms(corpusMapper, stopwords);
		createTree(getNgrams());
	}
	
	private void createTree(List<DocumentNgram> ngrams) {
		// TODO Auto-generated method stub
		
	}
}

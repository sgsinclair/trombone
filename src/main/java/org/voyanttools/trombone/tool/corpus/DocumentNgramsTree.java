/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.search.SpanQueryParser;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.Gram;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.DocumentNgram;
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

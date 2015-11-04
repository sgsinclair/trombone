/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.search.FieldPrefixAwareSimpleQueryParser;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class CorpusFacets extends AbstractTerms {

	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusFacets(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.corpus.AbstractTerms#runQueries(org.voyanttools.trombone.lucene.CorpusMapper, org.voyanttools.trombone.model.Keywords, java.lang.String[])
	 */
	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords,
			String[] queries) throws IOException {
		
		FieldPrefixAwareSimpleQueryParser parser = new FieldPrefixAwareSimpleQueryParser(corpusMapper.getLeafReader(), storage.getLuceneManager().getAnalyzer());
		Map<String, Query> queriesMap = parser.getQueriesMap(queries, false);
		Set<Term> terms = null;
		for (Map.Entry<String, Query> queryEntry : queriesMap.entrySet()) {
			Query query = queryEntry.getValue();
			
		}
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.corpus.AbstractTerms#runAllTerms(org.voyanttools.trombone.lucene.CorpusMapper, org.voyanttools.trombone.model.Keywords)
	 */
	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords)
			throws IOException {
		// TODO Auto-generated method stub

	}

}

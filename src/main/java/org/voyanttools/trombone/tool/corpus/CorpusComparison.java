package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.List;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

public class CorpusComparison extends AbstractTerms {

	public CorpusComparison(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		
		/* In this case we have a primary corpus and we're interested in finding over-represented words compared to a comparison
		 * corpus. For the efficiency we'll limit ourselves to a subset of terms.
		 */
		FlexibleParameters params = new FlexibleParameters();
		params.setParameter("minRawFreq", 2);
		int typesCount = corpusMapper.getCorpus().getCorpusMetadata().getTypesCount(tokenType);
		params.setParameter("stopList", parameters.getParameterValue("stopList"));
		CorpusTerms corpusTermsTool = new CorpusTerms(storage, parameters);
		corpusTermsTool.runAllTerms(corpusMapper, stopwords);
		List<CorpusTerm> corpusTerms = corpusTermsTool.getCorpusTerms();
		
	}


}

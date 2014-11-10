/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.mapdb.DB;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusTermMinimal;
import org.voyanttools.trombone.model.CorpusTermMinimalsDB;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class CorpusTermMinimals extends AbstractCorpusTool {
	
	private CorpusTermMinimalsDB corpusTermMinimalsDB = null;
	
	private TokenType tokenType;

	/**
	 * 
	 */
	public CorpusTermMinimals(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		tokenType = TokenType.getTokenTypeForgivingly(parameters.getParameterValue("tokenType", "lexical"));
	}

	@Override
	public void run(Corpus corpus) throws IOException {
		run(corpus, true);
	}
	
	public void run(Corpus corpus, boolean readOnly) throws IOException {

		corpusTermMinimalsDB = new CorpusTermMinimalsDB(storage, corpus, tokenType, readOnly);
		
		if (corpusTermMinimalsDB.isEmpty()) {
			AtomicReader reader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getDirectoryReader());
			StoredToLuceneDocumentsMapper corpusMapper = getStoredToLuceneDocumentsMapper(new IndexSearcher(reader), corpus);
			Bits docIdSet = corpusMapper.getDocIdOpenBitSet();
			
			// now we look for our term frequencies
			Terms terms = reader.terms(tokenType.name());
			TermsEnum termsEnum = terms.iterator(null);
			DocsEnum docsEnum = null;
			
			String termString;
			
			int documentsCount = corpus.size();
			
			DescriptiveStatistics stats = new DescriptiveStatistics();
			List<CorpusTermMinimal> corpusTermMinimalsList = new ArrayList<CorpusTermMinimal>();
			while(true) {
				
				BytesRef term = termsEnum.next();
				
				if (term != null) {
					termString = term.utf8ToString();
					docsEnum = termsEnum.docs(docIdSet, docsEnum, DocsEnum.FLAG_FREQS);
					int doc = docsEnum.nextDoc();
					int termFreq = 0;
					int inDocumentsCount = 0;
					while(doc!=DocsEnum.NO_MORE_DOCS) {
						inDocumentsCount++;
						int freq = docsEnum.freq();
						termFreq += freq;
						doc = docsEnum.nextDoc();
					}
					if (termFreq>0) {
						stats.addValue(termFreq);
						CorpusTermMinimal corpusTermMinimal = new CorpusTermMinimal(termString, termFreq, inDocumentsCount, documentsCount, 0);
						corpusTermMinimalsList.add(corpusTermMinimal);
					}
				}
				else {
					break; // no more terms
				}
			}
			float mean = (float) stats.getMean();
			float stdDev = (float) stats.getStandardDeviation();
			corpus.getCorpusMetadata().setTypesCountMean(tokenType, mean);
			corpus.getCorpusMetadata().setTypesCountStdDev(tokenType, stdDev);
			
			for (CorpusTermMinimal c : corpusTermMinimalsList) {
				c.setZscore((float) c.getRawFreq()-mean/stdDev);
				corpusTermMinimalsDB.put(c.getTerm(), c);
			}
			
			corpusTermMinimalsDB.commit();
			corpusTermMinimalsDB.close();
			corpusTermMinimalsDB = new CorpusTermMinimalsDB(storage, corpus, tokenType, true); // reopen as read-only

		}
	}
	
	protected static CorpusTermMinimalsDB getCorpusTermMinimalsDB(Storage storage, Corpus corpus, TokenType tokenType) throws IOException {
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"tokenType="+tokenType.name()});
		CorpusTermMinimals ctm = new CorpusTermMinimals(storage, parameters);
		ctm.run(corpus, true);
		return ctm.getCorpusTermMinimalsDB();
	}

	public CorpusTermMinimalsDB getCorpusTermMinimalsDB() {
		return corpusTermMinimalsDB;
	}
}

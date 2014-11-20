package org.voyanttools.trombone.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.CorpusMapper;
<<<<<<< HEAD
=======
import org.voyanttools.trombone.lucene.queries.CorpusFilter;
>>>>>>> 1188f2e92189734b70f52c9d0f93efbc82e2f2d2
import org.voyanttools.trombone.storage.Storage;

public class CorpusTermMinimalsDB extends AbstractDB {

	Map<String, CorpusTermMinimal> map;
	
	private CorpusTermMinimalsDB(Storage storage, Corpus corpus, TokenType tokenType, boolean readOnly) {
		this(storage, corpus, tokenType.name(), readOnly);
	}
	private CorpusTermMinimalsDB(Storage storage, Corpus corpus, String field, boolean readOnly) {
		super(storage, getName(corpus, field), readOnly);
		map = db.getHashMap(field);
	}
	public boolean isEmpty() {
		return map.isEmpty();
	}
	public void put(String term, CorpusTermMinimal c) {
		map.put(term,  c);
	}
	public CorpusTermMinimal get(String term) {
		return map.get(term);
	}
	public Collection<CorpusTermMinimal> values() {
		return map.values();
	}
	private static String getName(Corpus corpus, String field) {
		return corpus.getId()+"-corpusTermMinimals-"+field;
	}
	private synchronized static boolean exists(Storage storage, Corpus corpus, String field) {
		return AbstractDB.exists(storage, getName(corpus, field));
	}
	public static synchronized CorpusTermMinimalsDB getInstance(CorpusMapper corpusMapper, TokenType tokenType) throws IOException {
		return getInstance(corpusMapper, tokenType.name());
	}

	public static synchronized CorpusTermMinimalsDB getInstance(CorpusMapper corpusMapper, String field) throws IOException {
		Storage storage = corpusMapper.getStorage();
		AtomicReader reader = corpusMapper.getAtomicReader();
		Corpus corpus = corpusMapper.getCorpus();
		if (!exists(storage, corpus, field)) {
			Terms terms = reader.terms(field);
			TermsEnum termsEnum = terms.iterator(null);
			DocsEnum docsEnum = null;
			String termString;
			int documentsCount = corpus.size();
			DescriptiveStatistics stats = new DescriptiveStatistics();
			List<CorpusTermMinimal> corpusTermMinimalsList = new ArrayList<CorpusTermMinimal>();
			DocIdSet docIdSet = corpusMapper.getDocIdBitSet();
			DocIdSetIterator docIdSetIterator;
			int doc;
			int termFreq;
			int inDocumentsCount;
			int docsEnumDoc;
			while(true) {
				BytesRef term = termsEnum.next();
				if (term != null) {
					termString = term.utf8ToString();
					docsEnum = termsEnum.docs(reader.getLiveDocs(), docsEnum, DocsEnum.FLAG_FREQS);
					docIdSetIterator = docIdSet.iterator();
					doc = docIdSetIterator.nextDoc();
					termFreq = 0;
					inDocumentsCount=0;
					docsEnumDoc = docsEnum.nextDoc();
					while (doc!=DocIdSetIterator.NO_MORE_DOCS && docsEnumDoc!=DocsEnum.NO_MORE_DOCS) {
						if (doc>docsEnumDoc) {
							docsEnumDoc = docsEnum.advance(doc);
						}
						if (docsEnumDoc!=DocsEnum.NO_MORE_DOCS) {
							termFreq += docsEnum.freq();
							inDocumentsCount++;
						}
						doc = docIdSetIterator.nextDoc();
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
			corpus.getCorpusMetadata().setTypesCountMean(field, mean);
			corpus.getCorpusMetadata().setTypesCountStdDev(field, stdDev);
			CorpusTermMinimalsDB corpusTermMinimalsDB = new CorpusTermMinimalsDB(storage, corpus, field, false);
			for (CorpusTermMinimal c : corpusTermMinimalsList) {
				c.setZscore((float) c.getRawFreq()-mean/stdDev);
				corpusTermMinimalsDB.put(c.getTerm(), c);
			}
			corpusTermMinimalsDB.commit();
			corpusTermMinimalsDB.close();
		}
		return new CorpusTermMinimalsDB(storage, corpus, field, true);
	}
<<<<<<< HEAD
=======
	public static CorpusTermMinimalsDB getInstance(CorpusMapper corpusMapper, TokenType tokenType) throws IOException {
		return getInstance(corpusMapper.getStorage(), corpusMapper.getAtomicReader(), corpusMapper. getCorpus(), tokenType);
		// TODO Auto-generated method stub
		
	}

>>>>>>> 1188f2e92189734b70f52c9d0f93efbc82e2f2d2
}

package org.voyanttools.trombone.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.DocIdBitSet;
import org.voyanttools.trombone.lucene.CorpusMapper;

public class CorpusTermMinimalsDB extends AbstractDB {

	Map<String, CorpusTermMinimal> map;
	
	private CorpusTermMinimalsDB(CorpusMapper corpusMapper, String field, boolean readOnly) {
		super(corpusMapper.getStorage(), getName(corpusMapper.getCorpus(), field), readOnly);
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
	private synchronized static boolean exists(CorpusMapper corpusMapper, String field) {
		return AbstractDB.exists(corpusMapper.getStorage(), getName(corpusMapper.getCorpus(), field));
	}
	public static synchronized CorpusTermMinimalsDB getInstance(CorpusMapper corpusMapper, TokenType tokenType) throws IOException {
		return getInstance(corpusMapper, tokenType.name());
	}

	public static synchronized CorpusTermMinimalsDB getInstance(CorpusMapper corpusMapper, String field) throws IOException {
		if (!exists(corpusMapper, field)) {
			if (corpusMapper.getCorpus().size()==corpusMapper.getAtomicReader().numDocs()) {
				buildFromReaderTerms(corpusMapper, field); // TODO: is this any faster than going through documents?
			}
			else {
				buildFromDocumentTermVectors(corpusMapper, field);
			}
		}
		return new CorpusTermMinimalsDB(corpusMapper, field, true);
	}
	private static void buildFromDocumentTermVectors(CorpusMapper corpusMapper, String field) throws IOException {
		AtomicReader reader = corpusMapper.getAtomicReader();
		Map<String, AtomicInteger> inDocumentsCountMap = new HashMap<String, AtomicInteger>();
		Map<String, AtomicInteger> rawFreqsMap = new HashMap<String, AtomicInteger>();
		TermsEnum termsEnum = null;
		for (int doc : corpusMapper.getLuceneIds()) {
			Terms terms = reader.getTermVector(doc, "lexical");
			if (terms!=null) {
				termsEnum = terms.iterator(termsEnum);
				if (termsEnum!=null) {
					BytesRef bytesRef = termsEnum.next();
					while (bytesRef!=null) {
						String term = bytesRef.utf8ToString();
						if (!inDocumentsCountMap.containsKey(term)) {
							inDocumentsCountMap.put(term, new AtomicInteger());
							rawFreqsMap.put(term, new AtomicInteger());
						}
						inDocumentsCountMap.get(term).incrementAndGet();
						rawFreqsMap.get(term).addAndGet((int) termsEnum.totalTermFreq());
						bytesRef = termsEnum.next();
					}
				}
			}
		}
		
		// calculate aggregate stats
		SummaryStatistics stats = new SummaryStatistics();
		for (AtomicInteger ai : rawFreqsMap.values()) {stats.addValue((int) ai.get());}
		float mean = (float) stats.getMean();
		float stdDev = (float) stats.getStandardDeviation();		
		CorpusTermMinimal corpusTermMinimal;
		int documentsCount = corpusMapper.getCorpus().size();
		int rawFreq;
		// create map
		CorpusTermMinimalsDB corpusTermMinimalsDB = new CorpusTermMinimalsDB(corpusMapper, field, false);
		for (Map.Entry<String, AtomicInteger> entry : inDocumentsCountMap.entrySet()) {
			String term = entry.getKey();
			rawFreq = entry.getValue().get();
			corpusTermMinimal = new CorpusTermMinimal(term, rawFreqsMap.get(term).get(), inDocumentsCountMap.get(term).get(), documentsCount, ((float) rawFreq-mean) / stdDev);
			corpusTermMinimalsDB.put(term, corpusTermMinimal);
		}
		corpusTermMinimalsDB.commit();
		corpusTermMinimalsDB.close();		
	}
	
	private static void buildFromReaderTerms(CorpusMapper corpusMapper, String field) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		Terms terms = corpusMapper.getAtomicReader().terms(field);
		TermsEnum termsEnum = terms.iterator(null);
		DocsEnum docsEnum = null;
		String termString;
		int documentsCount = corpus.size();
		DescriptiveStatistics stats = new DescriptiveStatistics();
		List<CorpusTermMinimal> corpusTermMinimalsList = new ArrayList<CorpusTermMinimal>();
		DocIdBitSet docIdSet = corpusMapper.getDocIdBitSet();
		int doc;
		int termFreq;
		int inDocumentsCount;
		BytesRef term = termsEnum.next();
		while(term!=null) {
			if (term != null) {
				termFreq = 0;
				inDocumentsCount = 0;
				docsEnum = termsEnum.docs(docIdSet, docsEnum, DocsEnum.FLAG_FREQS);
				doc = docsEnum.nextDoc();
				while (doc!=DocsEnum.NO_MORE_DOCS) {
					termFreq += docsEnum.freq();
					inDocumentsCount++;
					doc = docsEnum.nextDoc();
				}
				if (termFreq>0) {
					termString = term.utf8ToString();
					stats.addValue(termFreq);
					CorpusTermMinimal corpusTermMinimal = new CorpusTermMinimal(termString, termFreq, inDocumentsCount, documentsCount, 0);
					corpusTermMinimalsList.add(corpusTermMinimal);
				}
			}
			term = termsEnum.next();
		}
		float mean = (float) stats.getMean();
		float stdDev = (float) stats.getStandardDeviation();
		corpus.getCorpusMetadata().setTypesCountMean(field, mean);
		corpus.getCorpusMetadata().setTypesCountStdDev(field, stdDev);
		CorpusTermMinimalsDB corpusTermMinimalsDB = new CorpusTermMinimalsDB(corpusMapper, field, false);
		for (CorpusTermMinimal c : corpusTermMinimalsList) {
			c.setZscore(((float) c.getRawFreq()-mean)/stdDev);
			corpusTermMinimalsDB.put(c.getTerm(), c);
		}
		corpusTermMinimalsDB.commit();
		corpusTermMinimalsDB.close();		
	}
}

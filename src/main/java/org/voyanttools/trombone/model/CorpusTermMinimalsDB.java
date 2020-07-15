package org.voyanttools.trombone.model;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
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
	public boolean exists(String term) {
		return map.containsKey(term);
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
//			if (corpusMapper.getCorpus().size()==corpusMapper.getLeafReader().numDocs()) {
//				buildFromReaderTerms(corpusMapper, field); // TODO: is this any faster than going through documents?
//			}
//			else {
				buildFromDocumentTermVectors(corpusMapper, field);
//			}
		}
		return new CorpusTermMinimalsDB(corpusMapper, field, true);
	}
	private static void buildFromDocumentTermVectors(CorpusMapper corpusMapper, String field) throws IOException {
		IndexReader reader = corpusMapper.getIndexReader();
		Map<String, AtomicInteger> inDocumentsCountMap = new HashMap<String, AtomicInteger>();
		Map<String, AtomicInteger> rawFreqsMap = new HashMap<String, AtomicInteger>();
		TermsEnum termsEnum = null;
		for (int doc : corpusMapper.getLuceneIds()) {
			Terms terms = reader.getTermVector(doc, field);
			if (terms!=null) {
				termsEnum = terms.iterator();
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
}

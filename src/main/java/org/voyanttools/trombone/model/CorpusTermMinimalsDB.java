package org.voyanttools.trombone.model;

import java.util.Map;

import org.voyanttools.trombone.storage.Storage;

public class CorpusTermMinimalsDB extends AbstractDB {

	Map<String, CorpusTermMinimal> map;
	public CorpusTermMinimalsDB(Storage storage, Corpus corpus, TokenType tokenType, boolean readOnly) {
		super(storage, corpus.getId()+"-corpusTermMinimals-all", readOnly);
		map = db.getHashMap(tokenType.name());
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

}

package org.voyanttools.trombone.lucene.search;

import org.apache.lucene.index.FilterLeafReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.util.Bits;

public class FilteredCorpusReader extends FilterLeafReader {
	
	private Bits bits;

	public FilteredCorpusReader(LeafReader in, Bits liveBits) {
		super(in);
		this.bits = liveBits;
	}
	
	@Override
	public Bits getLiveDocs() {
		return bits;
	}

	@Override
	public CacheHelper getCoreCacheHelper() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CacheHelper getReaderCacheHelper() {
		// TODO Auto-generated method stub
		return null;
	}
	
}

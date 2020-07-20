package org.voyanttools.trombone.lucene.search;

import java.io.IOException;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FilterDirectoryReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.util.BitSet;

public class FilteredCorpusDirectoryReader extends FilterDirectoryReader {

	private BitSet bits;
	
	public FilteredCorpusDirectoryReader(DirectoryReader in, BitSet bitSet) throws IOException {
		this(in, new FilteredCorpusReaderWrapper(bitSet));
		this.bits = bitSet;
	}
	
	public FilteredCorpusDirectoryReader(DirectoryReader in, SubReaderWrapper wrapper) throws IOException {
		super(in, wrapper);
	}

	@Override
	protected DirectoryReader doWrapDirectoryReader(DirectoryReader in) throws IOException {
		return new FilteredCorpusDirectoryReader(in, bits);
	}

	@Override
	public CacheHelper getReaderCacheHelper() {
		// TODO Auto-generated method stub
		return null;
	}

	public static class FilteredCorpusReaderWrapper extends SubReaderWrapper {

		private BitSet bits;
		
		public FilteredCorpusReaderWrapper(BitSet bitSet) {
			this.bits = bitSet;
		}
		
		@Override
		public LeafReader wrap(LeafReader reader) {
			return new FilteredCorpusReader(reader, bits);
		}
		
	}
}

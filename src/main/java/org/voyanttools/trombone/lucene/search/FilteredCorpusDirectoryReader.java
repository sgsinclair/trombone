package org.voyanttools.trombone.lucene.search;

import java.io.IOException;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FilterDirectoryReader;

public class FilteredCorpusDirectoryReader extends FilterDirectoryReader {

	public FilteredCorpusDirectoryReader(DirectoryReader in, SubReaderWrapper wrapper) throws IOException {
		super(in, wrapper);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected DirectoryReader doWrapDirectoryReader(DirectoryReader in) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CacheHelper getReaderCacheHelper() {
		// TODO Auto-generated method stub
		return null;
	}

}

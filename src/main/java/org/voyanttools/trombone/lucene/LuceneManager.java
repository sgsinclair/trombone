/*******************************************************************************
 * Trombone is a flexible text processing and analysis library used
 * primarily by Voyant Tools (voyant-tools.org).
 * 
 * Copyright (©) 2007-2012 Stéfan Sinclair & Geoffrey Rockwell
 * 
 * This file is part of Trombone.
 * 
 * Trombone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.lucene;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.LockObtainFailedException;

/**
 * @author sgs
 *
 */
public interface LuceneManager extends Comparable<LuceneManager> {
	
	public DirectoryReader getDirectoryReader(String corpus) throws CorruptIndexException, IOException;

	public void addDocument(String corpus, Document document) throws CorruptIndexException, IOException;

	public IndexWriter getIndexWriter(String corpus) throws CorruptIndexException, LockObtainFailedException, IOException;

	public Analyzer getAnalyzer(String corpus);
	
	public boolean directoryExists(String corpus) throws IOException;

	public void setDirectoryReader(String corpus, DirectoryReader indexReader);
	
	public void close(String corpus) throws IOException;
	
	public void closeAll() throws IOException;

	public long getLastAccessed();

}

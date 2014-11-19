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

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.search.IndexSearcher;
import org.junit.Test;
import org.voyanttools.trombone.Controller;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.corpus.CorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

/**
 * @author sgs
 *
 */
public class StoredToLuceneDocumentMapperTest {

	@Test
	public void test() throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();		
		FlexibleParameters parameters;
		CorpusCreator creator;
		
		parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("tiny/01.txt")});
		creator = new CorpusCreator(storage, parameters);
		creator.run();

		parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("tiny/02-09.zip")});
		creator = new CorpusCreator(storage, parameters);
		creator.run();
		String corpusId = creator.getStoredId();

		parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("tiny/10.txt")});
		creator = new CorpusCreator(storage, parameters);
		creator.run();
		
		Corpus corpus = storage.getCorpusStorage().getCorpus(corpusId);
		CorpusMapper corpusMapper = new CorpusMapper(storage, corpus);
		
		storage.destroy();
	}

}

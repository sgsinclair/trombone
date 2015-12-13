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
package org.voyanttools.trombone.storage.memory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.CorpusStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
class MemoryCorpusStorage implements CorpusStorage {
	
	private Map<String, Corpus> corpusMap = new HashMap<String, Corpus>();
	private Map<String, FlexibleParameters> parametersMap = new HashMap<String, FlexibleParameters>();

	@Override
	public Corpus getCorpus(String id) throws IOException {
		if (corpusMap.containsKey(id)) {
			return corpusMap.get(id);
		}
		else {
			throw new IOException("This corpus was not found. It's possible that it's been removed or that it never existed (that the corpus ID is wrong)");
		}
	}

	@Override
	public void storeCorpus(Corpus corpus, FlexibleParameters parameters) throws IOException {
		String id = corpus.getId();
		if (corpusMap.containsKey(id)) {
			throw new IOException("This corpus already exists: "+id);
		}
		else {
			corpusMap.put(id, corpus);
			parametersMap.put(id, parameters);
		}
	}

	@Override
	public boolean corpusExists(String id) {
		return corpusMap.containsKey(id);
	}

}

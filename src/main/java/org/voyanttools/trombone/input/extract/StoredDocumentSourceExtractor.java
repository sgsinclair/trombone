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
package org.voyanttools.trombone.input.extract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.voyanttools.trombone.document.DocumentFormat;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class StoredDocumentSourceExtractor {
	
	/**
	 * all parameters sent, only some of which may be relevant to some expanders
	 */
	private FlexibleParameters parameters;

	/**
	 * the storage strategy to use for storing document sources
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	
	private TikaExtractor tikaExtractor;

	public StoredDocumentSourceExtractor(
			StoredDocumentSourceStorage storedDocumentSourceStorage,
			FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
		this.tikaExtractor = new TikaExtractor(storedDocumentSourceStorage);
	}
	
	public List<StoredDocumentSource> getExtractedStoredDocumentSources(List<StoredDocumentSource> storedDocumentSources) throws IOException {
		List<StoredDocumentSource> extractedStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		for (StoredDocumentSource storedDocumentSource : storedDocumentSources) {
			extractedStoredDocumentSources.add(getExtractedStoredDocumentSources(storedDocumentSource));
		}
		return extractedStoredDocumentSources;
	}

	public StoredDocumentSource getExtractedStoredDocumentSources(
			StoredDocumentSource storedDocumentSource) throws IOException {
		DocumentFormat format = storedDocumentSource.getMetadata().getDocumentFormat();
		if (format==DocumentFormat.XML) {
			return storedDocumentSource;
		}
		else {
			ExtractableStoredDocumentSource extractableStoredDocumentSource = new TikaExtractableStoredDocumentSource(tikaExtractor, storedDocumentSource, parameters);
			return  storedDocumentSourceStorage.getStoredDocumentSource(extractableStoredDocumentSource);
		}
	}

}

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
import java.io.InputStream;

import javax.xml.transform.sax.TransformerHandler;

import org.apache.commons.codec.digest.DigestUtils;
import org.voyanttools.trombone.document.Metadata;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class TikaExtractableStoredDocumentSource implements ExtractableStoredDocumentSource {
	
	private String id;
	
	private StoredDocumentSource storedDocumentSource;
	
	private Metadata metadata;
	
	private TikaExtractor tikaExtractor;

	TikaExtractableStoredDocumentSource(TikaExtractor tikaExtractor, StoredDocumentSource storedDocumentSource, FlexibleParameters parameters) {
		id = DigestUtils.md5Hex(storedDocumentSource.getId()+"-extracted");
		this.tikaExtractor = tikaExtractor;
		this.storedDocumentSource = storedDocumentSource;
		this.metadata = storedDocumentSource.getMetadata();
	}

	public InputStream getInputStream() throws IOException {
		InputSource inputSource = tikaExtractor.getInputSource(storedDocumentSource);
		this.metadata = inputSource.getMetadata();		
		return inputSource.getInputStream();
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public String getUniqueId() {
		return id;
	}

}

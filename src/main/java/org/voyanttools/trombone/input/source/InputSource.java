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
package org.voyanttools.trombone.input.source;

import java.io.IOException;
import java.io.InputStream;

import org.voyanttools.trombone.document.Metadata;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;

/**
 * This interface defines a flexible input source that can be from a variety of
 * {@link Source} locations (URI, File, String, Stream). Typically an {@link InputSource}
 * is created and then stored using a {@link StoredDocumentSourceStorage} (which
 * also returns a {@link StoredDocumentSource}).
 * 
 * @author Stéfan Sinclair
 */
public interface InputSource {

	/**
	 * Get the {@link InputStream} associated with the input source – callers should
	 * make sure to close the stream when finished.
	 * 
	 * @return the {@link InputStream} of this input source
	 * @throws IOException if an IO exception occurs
	 */
	public InputStream getInputStream() throws IOException;

	/**
	 * Get the {@link Metadata} associated with this input source.
	 * 
	 * @return the {@link Metadata} associated with this input source.
	 */
	public Metadata getMetadata();

	/**
	 * Get the unique ID associate with this input source. This should me a hash
	 * code that represents several characteristics of the input source and allows
	 * it to retrieve the same source (based on the characteristics) and differentiate
	 * with different input sources.
	 * 
	 * @return the unique ID associate with this input source
	 */
	public String getUniqueId();

}

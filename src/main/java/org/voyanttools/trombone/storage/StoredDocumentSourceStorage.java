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
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.voyanttools.trombone.document.Metadata;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.source.InputSource;

/**
 * @author sgs
 *
 */
public interface StoredDocumentSourceStorage {

	public StoredDocumentSource getStoredDocumentSourceId(InputSource inputSource) throws IOException;

	public Metadata getStoredDocumentSourceMetadata(String id) throws IOException;

	public InputStream getStoredDocumentSourceInputStream(String id) throws FileNotFoundException;

	public List<StoredDocumentSource> getMultipleExpandedStoredDocumentSources(String id) throws IOException;

	public List<StoredDocumentSource> getMultipleExpandedStoredDocumentSources(String id, String prefix) throws IOException;

	public void setMultipleExpandedStoredDocumentSources(String id, List<StoredDocumentSource> archivedStoredDocumentSources) throws IOException;

	public void setMultipleExpandedStoredDocumentSources(String id, List<StoredDocumentSource> archivedStoredDocumentSources, String prefix) throws IOException;
}

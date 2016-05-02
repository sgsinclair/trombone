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
package org.voyanttools.trombone.input.expand;

import java.io.IOException;
import java.util.List;

import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.StoredDocumentSource;

/**
 * A strategy for expanding a {@link StoredDocumentSource}, including for
 * multi-file archives and multi-document XML files.
 * 
 * @author Stéfan Sinclair
 */
public interface Expander {

	/**
	 * Get a list of expanded {@link StoredDocumentSource}s. If a
	 * StoredDocumentSource doesn't need to be expanded it will just be added to
	 * a new list and returned. Note that though this method is public, only the
	 * {@link StoredDocumentSourceExpander#getExpandedStoredDocumentSources(StoredDocumentSource)}
	 * should be called publicly, this method should only be called by other
	 * classes implementing this interface with default visibility.
	 * 
	 * @param storedDocumentSource the stored document source to expand (or add as is)
	 * @return a list of exapnded {@link StoredDocumentSource}s
	 * @throws IOException an IO Exception
	 */
	public List<StoredDocumentSource> getExpandedStoredDocumentSources(
			StoredDocumentSource storedDocumentSource) throws IOException;
}

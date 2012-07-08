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
package org.voyanttools.trombone.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.voyanttools.trombone.document.Metadata;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.source.InputSource;

/**
 * This interface defines methods for interacting with
 * {@link StoredDocumentSource}s using a storage strategy defined by the
 * implementing class.
 * 
 * @author Stéfan Sinclair
 */
public interface StoredDocumentSourceStorage {

	/**
	 * Get the {@link StoredDocumentSource} that corresponds to the
	 * {@link InputSource} provided (if it's not already stored it will be).
	 * 
	 * @param inputSource
	 *            the {@link InputSource} from which to derive the
	 *            {@link StoredDocumentSource}
	 * @return the {@link StoredDocumentSource}
	 * @throws IOException
	 *             an IO exception during storage or retrieval
	 */
	public StoredDocumentSource getStoredDocumentSourceId(
			InputSource inputSource) throws IOException;

	/**
	 * Get the {@link Metadata} associated with the {@link StoredDocumentSource}
	 * specified by the ID.
	 * 
	 * @param id
	 *            the ID of the {@link StoredDocumentSource}
	 * @return the {@link Metadata} associated with the
	 *         {@link StoredDocumentSource} specified by the ID
	 * @throws IOException
	 *             an IO exception during retrieval of the
	 *             {@link StoredDocumentSource} (including if the
	 *             StoredDocumentSource doesn't exist)
	 */
	public Metadata getStoredDocumentSourceMetadata(String id)
			throws IOException;

	/**
	 * Get the {@link InputStream} associated with the
	 * {@link StoredDocumentSource} specified by the ID as it was stored when
	 * created with an {@link InputSource}.
	 * 
	 * @param id
	 *            the ID of the {@link StoredDocumentSource}
	 * @return the {@link InputStream} associated with the
	 *         {@link StoredDocumentSource} specified by the ID
	 * @throws IOException
	 *             an IO exception during retrieval of the {@link InputStream}
	 */
	public InputStream getStoredDocumentSourceInputStream(String id)
			throws IOException;

	/**
	 * Get a list of expanded {@link StoredDocumentSource}s for the
	 * {@link StoredDocumentSource} specified by the ID (or an empty list of no
	 * expanded list is stored).
	 * 
	 * @param id
	 *            the ID of the {@link StoredDocumentSource}
	 * @return a list of expanded {@link StoredDocumentSource}s for the
	 *         {@link StoredDocumentSource} specified by the ID
	 * @throws IOException
	 *             an IO exception during retrieval of the list of
	 *             {@link StoredDocumentSource}s (including if the
	 *             StoredDocumentSource doesn't exist)
	 */
	public List<StoredDocumentSource> getMultipleExpandedStoredDocumentSources(
			String id) throws IOException;

	/**
	 * Get a list of expanded {@link StoredDocumentSource}s for the
	 * {@link StoredDocumentSource} specified by the ID (or an empty list of no
	 * expanded list is stored). The additional prefix parameter is used to
	 * specify an identifier for the parameters used during expansion (it should
	 * be a hash code of some kind)
	 * 
	 * @param id
	 *            the ID of the {@link StoredDocumentSource}
	 * @param prefix
	 *            a prefix that specifies an identifier for the parameters used
	 *            during expansion
	 * @return a list of expanded {@link StoredDocumentSource}s for the
	 *         {@link StoredDocumentSource} specified by the ID
	 * @throws IOException
	 *             an IO exception during retrieval of the list of
	 *             {@link StoredDocumentSource}s (including if the
	 *             StoredDocumentSource doesn't exist)
	 */
	public List<StoredDocumentSource> getMultipleExpandedStoredDocumentSources(
			String id, String prefix) throws IOException;

	/**
	 * Set a list of expanded {@link StoredDocumentSource}s for the
	 * {@link StoredDocumentSource} specified by the ID.
	 * 
	 * @param id
	 *            the ID of the {@link StoredDocumentSource}
	 * @param archivedStoredDocumentSources
	 *            a list of expanded {@link StoredDocumentSource}s
	 * @throws IOException
	 *             an IO exception during storage
	 */
	public void setMultipleExpandedStoredDocumentSources(String id,
			List<StoredDocumentSource> archivedStoredDocumentSources)
			throws IOException;

	/**
	 * Set a list of expanded {@link StoredDocumentSource}s for the
	 * {@link StoredDocumentSource} specified by the ID. The additional prefix
	 * parameter is used to specify an identifier for the parameters used during
	 * expansion (it should be a hash code of some kind)
	 * 
	 * @param id
	 *            the ID of the {@link StoredDocumentSource}
	 * @param archivedStoredDocumentSources
	 *            a list of expanded {@link StoredDocumentSource}s
	 * @param prefix
	 *            a prefix that specifies an identifier for the parameters used
	 *            during expansion
	 * @throws IOException
	 *             an IO exception during storage
	 */
	public void setMultipleExpandedStoredDocumentSources(String id,
			List<StoredDocumentSource> archivedStoredDocumentSources,
			String prefix) throws IOException;
}

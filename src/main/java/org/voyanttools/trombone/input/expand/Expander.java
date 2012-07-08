/**
 * 
 */
package org.voyanttools.trombone.input.expand;

import java.io.IOException;
import java.util.List;

import org.voyanttools.trombone.document.StoredDocumentSource;

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

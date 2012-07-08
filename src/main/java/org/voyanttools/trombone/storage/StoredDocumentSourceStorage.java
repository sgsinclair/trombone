/**
 * 
 */
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

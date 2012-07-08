/**
 * 
 */
package org.voyanttools.trombone.input.expand;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.voyanttools.trombone.document.Metadata;
import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.InputStreamInputSource;
import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;

/**
 * An expander for compressed files. This is supported through the Apache 
 * Commons Compress library and supports common cases for a range of formats
 * like "bzip2", "bz2", "gzip", "gz", "pack200", "xz" (though not
 * all of these are tested). Note that for multi-file archives (like "tar"
 * and "zip") the {@link ArchiveExpander} should be used.
 * 
 * @author "Stéfan Sinclair"
 */
class CompressedExpander implements Expander {
	
	/**
	 * the primary expander (child documents are expanded with this)
	 */
	private Expander expander;
	
	/**
	 * the stored document storage strategy
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;

	/**
	 * Create a new instance of this expander (this should only be done by
	 * {@link StoredDocumentSourceExpander}.
	 * 
	 * @param storedDocumentSourceStorage a stored storage strategy
	 * @param storedDocumentSoruceExpander a reference to the primary expander
	 */
	CompressedExpander(StoredDocumentSourceStorage storedDocumentSourceStorage, StoredDocumentSourceExpander storedDocumentSoruceExpander) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.expander = storedDocumentSoruceExpander;
	}


	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.input.expand.Expander#getExpandedStoredDocumentSources(org.voyanttools.trombone.document.StoredDocumentSource)
	 */
	public List<StoredDocumentSource> getExpandedStoredDocumentSources(
			StoredDocumentSource storedDocumentSource) throws IOException {

		List<StoredDocumentSource> expandedDocumentSources = new ArrayList<StoredDocumentSource>();

		// first try to see if we've been here already
		String parentId = storedDocumentSource.getId();
		expandedDocumentSources = storedDocumentSourceStorage.getMultipleExpandedStoredDocumentSources(parentId);
		if (expandedDocumentSources!=null && expandedDocumentSources.isEmpty()==false) {
			return expandedDocumentSources;
		}
		
		Metadata metadata = storedDocumentSource.getMetadata();
		String filename = metadata.getLocation();
		if (filename==null || filename.isEmpty()==true) {filename="uncompressed";}
		
		InputStream inputStream = null;
		try {
			
			inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(parentId);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
			InputStream newInputStream = new CompressorStreamFactory().createCompressorInputStream(bufferedInputStream);
			
			Metadata childMetadata = metadata.asParent();
			String modifiedFilename = Pattern.compile("\\.(bzip2|bz2|gz|gzip|xz)$", Pattern.CASE_INSENSITIVE).matcher(filename).replaceFirst("");
			childMetadata.setLocation(modifiedFilename);
			childMetadata.setModified(metadata.getModified());
			childMetadata.setSource(Source.STREAM);
			String id = DigestUtils.md5Hex(parentId+"uncompressed");
			InputSource decompressedInputSource = new InputStreamInputSource(id, childMetadata, newInputStream);
			StoredDocumentSource decompressedStoredDocumentSource = storedDocumentSourceStorage.getStoredDocumentSourceId(decompressedInputSource);
			expandedDocumentSources.addAll(this.expander.getExpandedStoredDocumentSources(decompressedStoredDocumentSource)); // expand this recursively

			
			storedDocumentSourceStorage.setMultipleExpandedStoredDocumentSources(storedDocumentSource.getId(), expandedDocumentSources);
			return expandedDocumentSources;
		} catch (CompressorException e) {
			throw new IOException("A problem was encountered reading this compressed file: "+storedDocumentSource.getMetadata().getLocation(), e);
		}
		finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}

}

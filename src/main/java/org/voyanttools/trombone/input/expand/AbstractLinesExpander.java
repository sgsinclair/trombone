/**
 * 
 */
package org.voyanttools.trombone.input.expand;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.lang3.StringUtils;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.DocumentMetadata.ParentType;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public abstract class AbstractLinesExpander implements Expander {

	/**
	 * all parameters sent, only some of which may be relevant to some expanders
	 */
	private FlexibleParameters parameters;
	
	/**
	 * the stored document storage strategy
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;

	/**
	 * 
	 */
	protected AbstractLinesExpander(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
	}
	
	abstract DocumentFormat getChildDocumentFormat();

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.input.expand.Expander#getExpandedStoredDocumentSources(org.voyanttools.trombone.model.StoredDocumentSource)
	 */
	@Override
	public List<StoredDocumentSource> getExpandedStoredDocumentSources(StoredDocumentSource storedDocumentSource)
			throws IOException {
		
		List<StoredDocumentSource> storedDocumentSources = new ArrayList<StoredDocumentSource>();
		// first try to see if we've been here already
		String id = storedDocumentSource.getId();
		List<StoredDocumentSource> archivedStoredDocumentSources = storedDocumentSourceStorage.getMultipleExpandedStoredDocumentSources(id);
		if (archivedStoredDocumentSources!=null && archivedStoredDocumentSources.isEmpty()==false) {
			return archivedStoredDocumentSources;
		}
		
		DocumentMetadata documentMetadata = storedDocumentSource.getMetadata();
		DocumentMetadata docMetadata = null;
		
		InputStream inputStream = null;
		try {
			inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
			InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
			BufferedReader br = new BufferedReader(isr);
			String line;
			String childId;
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) {continue;}
				childId =  DigestUtils.md5Hex(line);
				docMetadata = documentMetadata.asParent(childId, ParentType.EXPANSION);
				docMetadata.setTitle(StringUtils.abbreviate(line.trim().substring(0, 100).replaceAll("\\s+", " "),50));
				docMetadata.setDocumentFormat(getChildDocumentFormat());
				docMetadata.setSource(Source.STRING);
				docMetadata.setLocation(documentMetadata.getLocation()+" ("+(storedDocumentSources.size()+1)+")");
				InputSource stringInputSource = new StringInputSource(childId, docMetadata, line);
				StoredDocumentSource sds = storedDocumentSourceStorage.getStoredDocumentSource(stringInputSource);
				storedDocumentSources.add(sds);
			}
		} catch (IOException e) {
			throw new IOException("Unable to expand source: "+id+" ("+documentMetadata.getLocation()+" ("+(storedDocumentSources.size()+1)+")");
		} finally {
			if (inputStream!=null) {
				inputStream.close();
			}
		}
		return storedDocumentSources;
	}

}

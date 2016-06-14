/**
 * 
 */
package org.voyanttools.trombone.input.extract;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.cxf.helpers.IOUtils;
import org.voyanttools.trombone.input.expand.Expander;
import org.voyanttools.trombone.input.expand.StoredDocumentSourceExpander;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.InputStreamInputSource;
import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.input.source.StoredDocumentSourceInputSource;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.DocumentMetadata.ParentType;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgsin
 *
 */
public class BagItExtractor implements Extractor {
	
	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	
	FlexibleParameters parameters;
	
	public BagItExtractor(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.input.extract.Extractor#getExtractableInputSource(org.voyanttools.trombone.model.StoredDocumentSource)
	 */
	@Override
	public InputSource getExtractableInputSource(StoredDocumentSource storedDocumentSource) throws IOException {
		ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();
		InputStream inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
		BufferedInputStream bis = new BufferedInputStream(inputStream);
		ArchiveInputStream archiveInputStream = null;
		
		String id = DigestUtils.md5Hex(storedDocumentSource.getId()+"-bagit");
		DocumentMetadata metadata = storedDocumentSource.getMetadata().asParent(id, ParentType.EXTRACTION);
		StoredDocumentSource extractedDocumentSource = null;
		try {
			archiveInputStream = archiveStreamFactory.createArchiveInputStream(bis);
			ArchiveEntry archiveEntry = archiveInputStream.getNextEntry();
			while (archiveEntry != null) {
				if (archiveEntry.isDirectory()==false) {
					final String filename = archiveEntry.getName();
					final File file = new File(filename);
					
					// these filenames are all hard-coded for CWRC for now, not sure how to generalize this
					if (file.getName().equals("MODS.bin")) { // get metadata if CWRC.bin doesn't have header
						DocumentMetadata docMetadata = getMetadata(archiveInputStream, "MODS");
						metadata.setTitle(docMetadata.getTitle());
						metadata.setAuthor(docMetadata.getAuthor());
					} else if (file.getName().equals("DC.xml")) { // get CWRC ID
						DocumentMetadata docMetadata = getMetadata(archiveInputStream, "DC");
						metadata.setExtra("cwrcIdentifier", docMetadata.getExtra("cwrcIdentifier"));
					} else if (file.getName().equals("CWRC.bin")) {
						InputSource is = new InputStreamInputSource(DigestUtils.md5Hex(UUID.randomUUID().toString()), metadata, new CloseShieldInputStream(archiveInputStream));
						StoredDocumentSource storedDocSource = storedDocumentSourceStorage.getStoredDocumentSource(is);
						
						FlexibleParameters params = new FlexibleParameters();
						
						// we'll have a peak at the file to see if we can determine its format, we do this here because otherwise it's treated much more generically
						InputStream storedInputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocSource.getId());
						String contents = IOUtils.readStringFromStream(storedInputStream);
						storedInputStream.close();
						if (contents.contains("<?xml") && contents.contains("<TEI") && contents.contains("GutenTag")) {
							params.setParameter("inputFormat", "GUTENTAG");
							metadata.setDefaultFormat(DocumentFormat.XML);
						}
						
						StoredDocumentSourceExtractor extractor = new StoredDocumentSourceExtractor(storedDocumentSourceStorage, params);
						extractedDocumentSource = extractor.getExtractedStoredDocumentSource(storedDocSource);
					}
				}
				archiveEntry = archiveInputStream.getNextEntry();
			}			
		} catch (ArchiveException e) {
			throw new IOException(e);
		} finally {
			if (archiveInputStream!=null) {archiveInputStream.close();}
		}
		
		if (extractedDocumentSource==null) {
			throw new IOException("Unable to find BagIt contents.");
		}
		
		return new StoredDocumentSourceInputSource(storedDocumentSourceStorage, extractedDocumentSource);

	}

	private InputSource getExtractableInputSource(ArchiveInputStream archiveInputStream, String inputFormat) throws IOException {
		FlexibleParameters params = new FlexibleParameters(new String[]{"inputFormat="+inputFormat});
		InputSource is = new InputStreamInputSource(DigestUtils.md5Hex(UUID.randomUUID().toString()), new DocumentMetadata(), new CloseShieldInputStream(archiveInputStream));
		StoredDocumentSource storedDocSource = storedDocumentSourceStorage.getStoredDocumentSource(is);
		XmlExtractor extractor = new XmlExtractor(storedDocumentSourceStorage, params);
		return extractor.getExtractableInputSource(storedDocSource);
	}

	private DocumentMetadata getMetadata(ArchiveInputStream archiveInputStream, String inputFormat) throws IOException {
		InputSource inputSource = getExtractableInputSource(archiveInputStream, inputFormat);
		inputSource.getInputStream().close(); // make sure it's read
		return inputSource.getMetadata();
	}
}

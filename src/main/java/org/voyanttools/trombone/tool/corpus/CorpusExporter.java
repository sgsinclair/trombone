/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.Stripper;

/**
 * @author sgs
 *
 */
public class CorpusExporter extends AbstractCorpusTool {

	private Corpus corpus = null;
	
	private Pattern FILENAME_PATTERN = Pattern.compile("^(.+?)(\\.[{\\p{L}\\d]+)$");
	
	private Pattern FILENAME_UNWANTED_CHARACTERS = Pattern.compile("[^\\p{L}\\d -]");
	
	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusExporter(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.corpus.AbstractCorpusTool#run(org.voyanttools.trombone.model.Corpus)
	 */
	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		this.corpus = corpusMapper.getCorpus();
	}

	public void run(Corpus corpus, OutputStream outputStream) throws IOException {
		
		// strategy for unique zip entry names
		Map<String, AtomicInteger> nameMapper = new HashMap<String, AtomicInteger>();
		
		Stripper stripper = new Stripper(Stripper.TYPE.ALL); // only used for text output
		
		ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
		String format = parameters.getParameterValue("documentFormat", "ORIGINAL").toUpperCase();
		String[] documentFilename = parameters.getParameterValues("documentFilename");
		if (format.equals("ORIGINAL")) {
			for (IndexedDocument document : corpus) {
				String fileEntryName = getFileEntryName(document.getMetadata(), documentFilename, nameMapper);
				ZipEntry e = new ZipEntry(fileEntryName);
				zipOutputStream.putNextEntry(e);
				InputStream inputStream = null;
				try {
					inputStream = storage.getStoredDocumentSourceStorage().getStoredDocumentSourceInputStream(document.getId());
					IOUtils.copy(inputStream, zipOutputStream);
				}
				finally {
					if (inputStream!=null) {
						inputStream.close();
					}
				}
				zipOutputStream.closeEntry();
			}
		}
		else {
			for (IndexedDocument document : corpus) {
				String fileEntryName = getFileEntryName(document.getMetadata(), documentFilename, nameMapper);
				String string = document.getDocumentString();
				if (format.equals("TXT") || format.equals("TEXT")) {
					string = stripper.strip(string);
					if (fileEntryName.endsWith("txt")==false) {fileEntryName+=".txt";}
				}
				else {
					if (fileEntryName.endsWith("html")==false) {fileEntryName+=".html";}
				}
				ZipEntry e = new ZipEntry(fileEntryName);
				zipOutputStream.putNextEntry(e);
				byte[] bytes = string.getBytes("UTF-8");
				zipOutputStream.write(bytes);
				zipOutputStream.closeEntry();
			}
		}
		zipOutputStream.close();
	}

	private String getFileEntryName(DocumentMetadata metadata, String[] documentFilename, Map<String, AtomicInteger> nameMapper) throws IOException {
		
		String filename = "";
		if (documentFilename.length>0) {
			Properties properties = metadata.getProperties();
			for (String fn : documentFilename) {
				for (String part : fn.split(",")) {
					part = part.trim();
					if (filename.isEmpty()==false) {filename+=" - ";}
					String p = (String) properties.get(part);
					if (p!=null && p.trim().isEmpty()==false) {
						p = FILENAME_UNWANTED_CHARACTERS.matcher(p).replaceAll("");
						if (p.length()>25) {
							p = p.substring(0, 25);
						}
						filename+=p.trim();
					}
					else {
						filename+="unknown "+part;
					}
				}
			}
//			filename = URLEncoder.encode(filename, "UTF-8"); // we're replacing characters instead
		}
		
		if (filename.isEmpty()) {
			filename = metadata.getLocation();
		}
		
		// try to get the document format based on the filename only (so http://example.come/ might be UNKNOWN even if it's html)
		DocumentFormat format = DocumentFormat.fromFilename(filename);
		if (format==DocumentFormat.UNKNOWN) {
			if (metadata.getSource()==Source.URI) {
				URI uri;
				try {
					uri = new URI(filename);
					String path = uri.getPath();
					if (path.isEmpty() || path.equals("/")) { // could be just domain name
						filename=uri.getHost();
					}
					else {
						filename=path.replaceAll("/", "_"); // replace slashes
					}
				} catch (URISyntaxException e) {
					filename = URLEncoder.encode(filename, "UTF-8");
				}
			}
			
			// add an extension
			Properties properties = metadata.getProperties();
			if (properties.containsKey("parent_location")) {
				filename+="."+DocumentFormat.fromFilename(properties.getProperty("parent_location")).getDefaultExtension();
			}
			else {
				filename+="."+metadata.getDocumentFormat().getDefaultExtension(); // add an extension
			}
		}	

		if (nameMapper.containsKey(filename)) {
			int i = nameMapper.get(filename).incrementAndGet();
			Matcher matcher = FILENAME_PATTERN.matcher(filename);
			if (matcher.find()) {
				filename = matcher.group(1)+" - "+i+matcher.group(2);
			}
			else {
				filename += " - "+i;
			}
		}
		else {
			nameMapper.put(filename, new AtomicInteger());
		}
		return filename;
	}

}

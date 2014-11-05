/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.search.IndexSearcher;
import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.model.TokenType;
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
	public void run(Corpus corpus) throws IOException {
		this.corpus = corpus;
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
			AtomicReader reader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getDirectoryReader());
			StoredToLuceneDocumentsMapper corpusMapper = getStoredToLuceneDocumentsMapper(new IndexSearcher(reader), corpus);
			for (IndexedDocument document : corpus) {
				String fileEntryName = getFileEntryName(document.getMetadata(), documentFilename, nameMapper);
				String string = reader.document(corpusMapper.getLuceneIdFromDocumentId(document.getId())).get(TokenType.lexical.name());
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
						if (p.length()>20) {
							p = p.substring(0, 20)+"…";
						}
						filename+=p;
					}
					else {
						filename+="unknown "+part;
					}
				}
			}
			filename = URLEncoder.encode(filename, "UTF-8");
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
			filename+="."+metadata.getDocumentFormat().getDefaultExtension(); // add an extension
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

/**
 * 
 */
package org.voyanttools.trombone.input.extract;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.parser.xml.XMLParser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * @author sgs
 *
 */
public class XmlOrHtmlTikaParser extends AbstractParser {

    private static final Set<MediaType> SUPPORTED_TYPES =
            Collections.unmodifiableSet(new HashSet<MediaType>(Arrays.asList(
                    MediaType.application("xml"),
                    MediaType.text("html"),
                    MediaType.application("xhtml+xml"))));
    
	/**
	 * 
	 */
	public XmlOrHtmlTikaParser() {
	}

	/* (non-Javadoc)
	 * @see org.apache.tika.parser.Parser#getSupportedTypes(org.apache.tika.parser.ParseContext)
	 */
	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return SUPPORTED_TYPES;
	}

	/* (non-Javadoc)
	 * @see org.apache.tika.parser.Parser#parse(java.io.InputStream, org.xml.sax.ContentHandler, org.apache.tika.metadata.Metadata, org.apache.tika.parser.ParseContext)
	 */
	@Override
	public void parse(InputStream stream, ContentHandler handler,
			Metadata metadata, ParseContext context) throws IOException,
			SAXException, TikaException {
		Detector detector = new DefaultDetector();
		MediaType mediaType = detector.detect(stream, metadata);
		if (mediaType==MediaType.TEXT_HTML) {
			new HtmlParser().parse(stream, handler, metadata, context);
		}
		else {
			new XMLParser().parse(stream, handler, metadata, context);
		}
	}

}

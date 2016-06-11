/**
 * 
 */
package org.voyanttools.trombone.tool.notebook;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.cxf.helpers.IOUtils;
import org.voyanttools.trombone.input.source.UriInputSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("notebook")
public class NotebookManager extends AbstractTool {
	
	String notebook = null; // notebook source (ID, URL, etc.)
	
	String jsonData = null; // notebook data as JSON

	public NotebookManager(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run() throws IOException {
		if (parameters.containsKey("jsonData")) { // this might be provided by Trombone
			jsonData = parameters.getParameterValue("jsonData");
		} 
		if (parameters.containsKey("notebook")) {
			notebook = parameters.getParameterValue("notebook");
			if (jsonData==null && notebook.startsWith("http")) {
				URI uri;
				try {
					uri = new URI(notebook);
				} catch (URISyntaxException e) {
					throw new IllegalArgumentException("Unable to parse URL: "+notebook);
				}
				UriInputSource inputSource = new UriInputSource(uri);
				InputStream is = null;
				try {
					is = inputSource.getInputStream();
					jsonData = IOUtils.readStringFromStream(is);
				} finally {
					if (is!=null) is.close();
				}
			}
		}
		if (jsonData==null) {
			throw new RuntimeException("Unable to locate requested notebook.");
		}
	}
}

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
package org.voyanttools.trombone.input.source;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class InputSourcesBuilder {
	
	private FlexibleParameters parameters;

	public InputSourcesBuilder(FlexibleParameters parameters) {
		this.parameters = parameters;
	}
	
	public static boolean hasParameterSources(FlexibleParameters parameters) {
		for (String p : new String[]{"file","string","uri","upload","archive"}) {
			if (parameters.getParameterValue(p,"").isEmpty()==false) {return true;}
		}
		return false;
	}

	public List<InputSource> getInputSources(Storage storage) throws IOException {
		List<InputSource> inputSources = getInputSources(storage, parameters);
		FlexibleParameters storedparams = parameters.deepClone();
		for (String key : new String[]{"upload", "string", "uri", "archive", "tool"}) storedparams.removeParameter(key);
		for (InputSource inputSource : inputSources) {
			inputSource.getMetadata().setQueryParameters(storedparams);
		}
		return inputSources;
	}
	
	private List<InputSource> getInputSources(Storage storage, FlexibleParameters params) throws IOException {
		File localFileSource = null;
		if (storage instanceof FileStorage && params.containsKey("localSource")) {
			String name = new File(params.getParameterValue("localSource")).getName();
			if (name.isEmpty()==false && name.contains("/")==false && name.contains(".")==false) {
				File rootData = ((FileStorage) storage).storageLocation.getParentFile();
				File sources = new File(rootData, "trombone-local-sources");
				localFileSource = new File(sources, new File(name).getName());
				if (localFileSource.exists()==false) {localFileSource=null;}
			}
		}
		List<InputSource> inputSources = new ArrayList<InputSource>();
		for (String file : params.getParameterValues("file")) {
			inputSources.addAll(getInputSources(new File(file)));
		}

		for (String file : params.getParameterValues("upload")) {
			File f = new File(file);
			InputSource inputSource = new FileInputSource(f);
			inputSource.getMetadata().setLocation(f.getName());
			inputSources.add(inputSource);
		}
		
		for (String string : params.getParameterValues("string")) {
			inputSources.add(new StringInputSource(string));
		}

		for (String uriString : params.getParameterValues("uri")) {
			URI uri;
			try {
				uri = new URI(uriString);
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException("The URI provided by the parameters has a problem: "+uriString, e);
			}
			if (localFileSource!=null) { // check for a local source
				String path = uri.getPath(); // look at the provided URL
				if (path.contains("..")==false) {
					String name = localFileSource.getName(); // consider only the path
					String dirname = "/"+name+"/";
					// find the starting point: after the local source directory name, if there, or the last portion if not
					int start = path.contains(dirname) ? path.lastIndexOf(dirname)+dirname.length()-1 :  path.lastIndexOf("/");
					String file = path.substring(start+1); // grab path
					if (file.isEmpty()==false) { // make sure we have value
						File localFile = new File(localFileSource, file); // construct a file
						if (localFile.exists()) { // we have a match!
							inputSources.addAll(getInputSources(localFile));
							continue; // don't add URI too
						}
					}
				}
			}
			inputSources.add(new UriInputSource(uri));
		}
		
		for (String archive : params.getParameterValues("archive")) {
			FlexibleParameters pms = params.deepClone();
			for (String key : new String[]{"upload", "string", "uri", "archive", "tool"}) pms.removeParameter(key);
			if (archive.startsWith("http")) {
				for (String uri : archive.split("(\r\n|\r|\n)+")) {
					if (uri.startsWith("http")) {
						pms.addParameter("uri", uri);
					}
				}
				if (pms.getParameterValues("uri").length>0) {
					inputSources.addAll(getInputSources(storage, pms));
				}
			}  else {
				pms.addParameter("string", archive);
				inputSources.addAll(getInputSources(storage, pms));
			}
		}
		return inputSources;
	}
	
	private List<InputSource> getInputSources(File file) throws IOException {
		List<InputSource> inputSources = new ArrayList<InputSource>();
		// directories don't get cached, so handle them differently
		if (file.isDirectory()) {
			final File[] files = file.listFiles();
			Arrays.sort(files); // make sure files are in sorted order
			for (File f : files) {
				inputSources.addAll(getInputSources(f));
			}
		} else {
			if (DocumentFormat.isSkippable(file)==false) {
				inputSources.add(new FileInputSource(file));
			}
		}
		return inputSources;
		
	}
}

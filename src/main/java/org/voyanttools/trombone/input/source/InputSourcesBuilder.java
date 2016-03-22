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
		for (String p : new String[]{"file","string","uri","upload"}) {
			if (parameters.getParameterValue(p,"").isEmpty()==false) {return true;}
		}
		return false;
	}

	public List<InputSource> getInputSources() throws IOException {
		List<InputSource> inputSources = new ArrayList<InputSource>();
		for (String file : parameters.getParameterValues("file")) {
			inputSources.addAll(getInputSources(new File(file)));
		}

		for (String file : parameters.getParameterValues("upload")) {
			File f = new File(file);
			InputSource inputSource = new FileInputSource(f);
			inputSource.getMetadata().setLocation(f.getName());
			inputSources.add(inputSource);
		}
		
		for (String string : parameters.getParameterValues("string")) {
			inputSources.add(new StringInputSource(string));
		}

		for (String uriString : parameters.getParameterValues("uri")) {
			URI uri;
			try {
				uri = new URI(uriString);
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException("The URI provided by the parameters has a problem: "+uriString, e);
			}
			inputSources.add(new UriInputSource(uri));
		}
		FlexibleParameters storedparams = parameters.deepClone();
		for (String key : new String[]{"upload", "string", "uri", "tool"}) storedparams.removeParameter(key);
		for (InputSource inputSource : inputSources) {
			inputSource.getMetadata().setQueryParameters(storedparams);
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

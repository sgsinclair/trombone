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
package org.voyanttools.trombone;

import java.io.IOException;
import java.util.List;

import org.voyanttools.trombone.document.StoredDocumentSource;
import org.voyanttools.trombone.input.expand.StoredDocumentSourceExpander;
import org.voyanttools.trombone.input.extract.StoredDocumentSourceExtractor;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.InputSourcesBuilder;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class Controller {

	private FlexibleParameters parameters;
	public Controller(FlexibleParameters parameters) {
		this.parameters = parameters;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if (args == null) {
			throw new NullPointerException("illegal arguments");
		}

		final FlexibleParameters parameters = new FlexibleParameters(args);

		final Controller controller = new Controller(parameters);
		controller.run();
	}

	private void run() throws IOException {
		
		// this will all change to use tools instead
		Storage storage = new MemoryStorage();
		if (InputSourcesBuilder.hasParameterSources(parameters)) {
			InputSourcesBuilder inputSourcesBuilder = new InputSourcesBuilder(parameters);
			List<InputSource> inputSources = inputSourcesBuilder.getInputSources();
			StoredDocumentSourceExpander sourcesExpander = new StoredDocumentSourceExpander(storage.getStoredDocumentSourceStorage(), parameters);
			List<StoredDocumentSource> expandedDocs = sourcesExpander.getExpandedStoredDocumentSources(inputSources);
			StoredDocumentSourceExtractor extractedBuilder = new StoredDocumentSourceExtractor(storage.getStoredDocumentSourceStorage(), parameters);
			List<StoredDocumentSource> extractedDocs = extractedBuilder.getExtractedStoredDocumentSources(expandedDocs);
			System.err.println(extractedDocs.size());
		}
		// TODO Auto-generated method stub
		
	}

}

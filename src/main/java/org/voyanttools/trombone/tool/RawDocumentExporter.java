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
package org.voyanttools.trombone.tool;

import java.io.BufferedInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.voyanttools.trombone.input.source.InputSourcesBuilder;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class RawDocumentExporter extends AbstractTool implements RawSerializable {

	String id = null;
	
	/**
	 * @param storage
	 * @param parameters
	 */
	public RawDocumentExporter(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.RunnableTool#run()
	 */
	@Override
	public void run() throws IOException {
		Writer writer = null;
		if (parameters.containsKey("outputFile")) {
			writer = new FileWriter(parameters.getParameterValue("outputFile"));
		}
		else {
			writer = new OutputStreamWriter(System.out);
			serialize(writer);
		}
		try {
			serialize(writer);
		}
		finally {
			if (writer!=null) {writer.close();}
		}
	}
	
	public void serialize(Writer writer) throws IOException {
		InputStream inputStream = null;
		try {
			if (InputSourcesBuilder.hasParameterSources(parameters)) {
				
			}
			inputStream = storage.getStoredDocumentSourceStorage().getStoredDocumentSourceInputStream(id);
			if (inputStream==null) {
				throw new IOException("Document not found: "+id);
			}
			BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
			// FIXME: copy this without closing writer buffer
			IOUtils.copy(bufferedInputStream, writer);
			inputStream.close();
		}
		finally {
			if (inputStream!=null) {inputStream.close();}
		}
	}

}

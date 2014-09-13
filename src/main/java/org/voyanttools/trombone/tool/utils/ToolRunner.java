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
package org.voyanttools.trombone.tool.utils;

import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.voyanttools.trombone.input.source.InputSourcesBuilder;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.ToolFactory;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
@XStreamAlias("results")
public class ToolRunner extends AbstractTool {
	
	private long duration;
	
	@XStreamOmitField
	private Writer writer;
	
	@XStreamImplicit
	List<RunnableTool> results = new ArrayList<RunnableTool>();
	
	/**
	 * 
	 */
	public ToolRunner(Storage storage, FlexibleParameters parameters, Writer writer) {
		super(storage, parameters);
		this.writer = writer;
	}
	
	public void run() throws IOException {

		ToolFactory toolFactory = new ToolFactory(storage, parameters);
		toolFactory.run();
		List<RunnableTool> tools = toolFactory.getRunnableTools();
		
		StringBuilder sb = new StringBuilder("cache-ToolRunner-").append(getVersion());
		for (RunnableTool tool : tools) {
			sb.append("-").append(tool.getClass().getSimpleName()).append(tool.getVersion());
		}
		sb.append("-").append(DigestUtils.md5Hex(parameters.toString()));
		
		String id = sb.toString();
		
		boolean hasParameterSources = InputSourcesBuilder.hasParameterSources(parameters);
		if (parameters.getParameterBooleanValue("noCache")==false && parameters.getParameterBooleanValue("reCache")==false && hasParameterSources==false && storage.isStored(id)) {
			Reader reader = storage.retrieveStringReader(id);
			IOUtils.copy(reader, writer);
			reader.close();
			writer.flush();
		}
		else {
			long start = Calendar.getInstance().getTimeInMillis();
			for (RunnableTool tool : tools) {
				tool.run();
				results.add(tool);
			}
			duration = Calendar.getInstance().getTimeInMillis() - start;

			ToolSerializer toolSerializer = new ToolSerializer(parameters, this);
			if (parameters.getParameterBooleanValue("noCache") || hasParameterSources==true) { // use the configured writer directly
				toolSerializer.run(writer); 
			}
			else { // try to cache
				Writer cacheWriter = storage.getStoreStringWriter(id);
				toolSerializer.run(cacheWriter);
				cacheWriter.close();
				Reader reader = storage.retrieveStringReader(id);
				IOUtils.copy(reader, writer); // now write from cache
				reader.close();
				writer.flush();
			}
		}
		
	}
	

	
	public List<RunnableTool> getRunnableToolResults() {
		return results;
	}


}

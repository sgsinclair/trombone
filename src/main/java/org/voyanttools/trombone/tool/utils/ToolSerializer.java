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

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JsonWriter;

/**
 * @author sgs
 *
 */
public class ToolSerializer implements RunnableTool {
	
	private FlexibleParameters parameters;
	
	private RunnableTool runnableTool;

	/**
	 * @throws IOException 
	 * 
	 */
	public ToolSerializer(FlexibleParameters parameters, RunnableTool runnableTool) {
		this.parameters = parameters;
		this.runnableTool = runnableTool;
	}
	
	public void run(Writer writer) {
		
		if (this.runnableTool instanceof ToolRunner) {
			List<RunnableTool> tools = ((ToolRunner) runnableTool).getRunnableToolResults();
			if (tools.isEmpty()==false && tools.get(tools.size()-1) instanceof RawSerializable) {
				
			}
		}
		
		XStream xs;
		if (parameters.getParameterValue("outputFormat", "").equals("xml")) {
			xs = new XStream();
		}
		else {
			xs = new XStream(new JsonHierarchicalStreamDriver() {
				@Override
				public HierarchicalStreamWriter createWriter(Writer writer) {
					return new JsonWriter(writer, JsonWriter.DROP_ROOT_MODE);
				}
			});
		}
		
		if (xs == null) return; // don't serialize results, therefore no output data is emitted
			
		xs.autodetectAnnotations(true);
		xs.toXML(runnableTool, writer);
		
	}

	public void run() throws IOException {
		if (parameters.containsKey("outputFile")) {
			Writer writer = new FileWriter(parameters.getParameterValue("outputFile"));
			run(writer);
			writer.close();
		}
		else {
			Writer writer = new OutputStreamWriter(System.out);
			run(writer);
			writer.close();
		}
	}
}

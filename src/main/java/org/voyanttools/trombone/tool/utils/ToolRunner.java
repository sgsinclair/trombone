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
import java.util.ArrayList;
import java.util.List;

import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.ToolFactory;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.ibm.icu.util.Calendar;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
@XStreamAlias("results")
public class ToolRunner implements RunnableTool {
	
	private long duration;
	
	private FlexibleParameters parameters;
	
	@XStreamOmitField
	private Storage storage;
	
	@XStreamImplicit
	List<RunnableTool> results = new ArrayList<RunnableTool>();
	
	/**
	 * 
	 */
	public ToolRunner(Storage storage, FlexibleParameters parameters) {
		this.storage = storage;
		this.parameters = parameters;
	}
	
	public void run() throws IOException {
		
		long start = Calendar.getInstance().getTimeInMillis();
		ToolFactory toolFactory = new ToolFactory(storage, parameters);
		toolFactory.run();
		List<RunnableTool> tools = toolFactory.getRunnableTools();
		for (RunnableTool tool : tools) {
			tool.run();
			results.add(tool);
		}
		
		duration = Calendar.getInstance().getTimeInMillis() - start;
	}
	
	public List<RunnableTool> getRunnableToolResults() {
		return results;
	}
	
}

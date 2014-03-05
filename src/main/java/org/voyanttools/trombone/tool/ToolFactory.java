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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.tool.utils.RunnableTool;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class ToolFactory extends AbstractTool {

	private List<RunnableTool> runnableTools = new ArrayList<RunnableTool>();
	
	/**
	 * 
	 */
	public ToolFactory(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}
	
	public RunnableTool getRunnableTool(String toolName) {
		try {
			final Class<?> toolClass = Class.forName(this.getClass().getPackage().getName() + "." + toolName);
			final Constructor<?> constructor = toolClass.getConstructor(Storage.class, FlexibleParameters.class);
			final Object toolInstance = constructor.newInstance(storage, parameters);
			if (toolInstance instanceof RunnableTool == false) {
				throw new IllegalArgumentException("Unable to instantiate unknown tool "+toolName+" that does not implement the RunnableTool interface");
			}			
			final RunnableTool tool = (RunnableTool) toolInstance;
			return tool;
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unable to instantiate tool "+ toolName + " (wrong package or expected constructor not available)", e);
		}		
	}
	
	public List<RunnableTool> getRunnableTools(Collection<String> toolNames) {
		List<RunnableTool> runnableTools = new ArrayList<RunnableTool>();
		for (String toolName : toolNames) {
			RunnableTool runnableTool = getRunnableTool(toolName);
			runnableTools.add(runnableTool);
		}
		return runnableTools;
	}

	@Override
	public void run() throws IOException {
		String[] toolNames = parameters.getParameterValues("tool");
		List<String> tools = new ArrayList<String>();
		for (String toolName : toolNames) {
			for (String tn : toolName.split(",\\s*")) {
				tools.add(tn);
			}
		}
		Arrays.asList(toolNames);
		runnableTools = getRunnableTools(tools);
	}
	
	public List<RunnableTool> getRunnableTools() {
		return runnableTools;
	}

}

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

import java.util.ArrayList;
import java.util.List;

import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class ToolResult {

	private List<Object> objects;
	
	private FlexibleParameters parameters;
	
	/**
	 * @param parameters 
	 * 
	 */
	public ToolResult(FlexibleParameters parameters) {
		this.parameters = parameters;
		objects = new ArrayList<Object>();
	}
	
	public void add(Object object) {
		objects.add(object);
	}

}

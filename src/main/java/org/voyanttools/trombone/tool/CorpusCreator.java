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

import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
@XStreamAlias("stepEnabledCorpusCreator")
public class CorpusCreator extends AbstractTool {

	private String nextCorpusCreatorStep = "";
	
	private String storedId;
	
	@XStreamOmitField
	private RealCorpusCreator realCorpusCreator;
	
	public CorpusCreator(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		realCorpusCreator = new RealCorpusCreator(storage, parameters);
	}

	@Override
	public void run() throws IOException {
		realCorpusCreator.run(Integer.MAX_VALUE);
		nextCorpusCreatorStep = realCorpusCreator.getNextCorpusCreatorStep();
		storedId = realCorpusCreator.getStoredId();
	}
	
//	String getNextCorpusCreatorStep() {
//		return nextCorpusCreatorStep;
//	}
//	
	public String getStoredId() {
		return storedId;
	}

}

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
package org.voyanttools.trombone.lucene.analysis;

import static org.uimafit.util.JCasUtil.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.uimafit.component.JCasAnnotator_ImplBase;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * @author Richard Eckart, Stéfan Sinclair
 */
public class MarkupFilter extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas aJcas) throws AnalysisEngineProcessException {
		List<Annotation> toRemove = new ArrayList<Annotation>();
		for (Token t : select(aJcas, Token.class)) {
			String text = t.getCoveredText();
			if (text.startsWith("<") && (text.endsWith(">"))) {
				toRemove.add(t);
			}
		}
		
		for (Annotation a: toRemove) {
			a.removeFromIndexes();
		}
	}

}

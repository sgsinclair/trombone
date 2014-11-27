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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.VariantsDB;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import edu.stanford.nlp.util.StringUtils;

/**
 * @author sgs
 *
 */
public abstract class AbstractTool implements RunnableTool {

	@XStreamOmitField
	protected FlexibleParameters parameters;
	
	@XStreamOmitField
	protected transient Storage storage;
	
	private static int VERSION = 1;
	
	@XStreamOmitField
	private boolean isVerbose;
	
	@XStreamOmitField
	private DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
	
	/**
	 * @param storage 
	 * 
	 */
	public AbstractTool(Storage storage, FlexibleParameters parameters) {
		this.storage = storage;
		this.parameters = parameters;
		this.isVerbose = parameters.getParameterBooleanValue("verbose");
	}
	
	public int getVersion() {
		return VERSION;
	}
	
	protected boolean isVerbose() {
		return isVerbose;
	}
	
	protected void log(String string) {
		log(string, null);
	}
	
	protected void log(String string, Calendar start) {
		if (isVerbose()) {
			Calendar now = Calendar.getInstance();
			System.out.println(dateFormat.format(now.getTime())+"\t"+string+(start!=null ? "("+(now.getTimeInMillis()-start.getTimeInMillis())+" ms)" : ""));
		}
	}

	public FlexibleParameters getParameters() {
		return parameters;
	}
	protected Keywords getStopwords(Corpus corpus) throws IOException {
		Keywords keywords = new Keywords();
		if (parameters.containsKey("stopList")) {
			if (parameters.getParameterValue("stopList", "").equals("auto")) {
				Map<String,String> langs = new HashMap<String,String>();
				for (IndexedDocument document : corpus) {
					String lang = document.getMetadata().getLanguageCode();
					if (langs.containsKey(lang)==false) {
						if (lang.isEmpty() || lang.equals("en")) {langs.put(lang, "stop.en.taporware.txt");}
						else if (lang.equals("de")) {langs.put(lang, "stop.de.german.txt");}
						else if (lang.equals("fr")) {langs.put(lang, "stop.fr.veronis.txt");}
						else if (lang.equals("hu")) {langs.put(lang, "stop.hu.hungarian.txt");}
						else if (lang.equals("it")) {langs.put(lang, "stop.it.italian.txt");}
						else if (lang.equals("no")) {langs.put(lang, "stop.no.norwegian.txt");}
						else if (lang.equals("se")) {langs.put(lang, "stop.se.swedish.txt");}
					}
				}
				if (langs.isEmpty()==false) {
					keywords.load(storage, langs.values().toArray(new String[0]));
				}
			}
			else {
				keywords.load(storage, parameters.getParameterValues("stopList"));
			}
		}
		return keywords;
	}
	
	protected String[] getQueries() throws IOException {
		return	getQueries(parameters.getParameterValues("query"));
	}
	protected String[] getQueries(String[] queryStrings) throws IOException {
		List<String> queries = new ArrayList<String>();
		VariantsDB variantsDB = null;
//			if (parameters.containsKey("variants")) {
//				variantsDB = new VariantsDB(storage, parameters.getParameterValue("variants"), true);
//			}
		for (String query : queryStrings) {
			for (String q : query.split("\\s*,\\s*")) {
				if (variantsDB!=null) {
					String[] variants = variantsDB.get(q);
					if (variants==null) {
						queries.add(q);
					}
					else {
						queries.add("("+StringUtils.join(variants, "|")+")");
					}
				}
				else {
					queries.add(q);
				}
			}
		}
		if (variantsDB!=null) {variantsDB.close();}
		return queries.toArray(new String[0]);
	}
}

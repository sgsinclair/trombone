/*******************************************************************************
 cam * Trombone is a flexible text processing and analysis library used
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
package org.voyanttools.trombone.input.extract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class StoredDocumentSourceExtractor {
	
	/**
	 * all parameters sent, only some of which may be relevant to some expanders
	 */
	private FlexibleParameters parameters;

	/**
	 * the storage strategy to use for storing document sources
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	
	private TikaExtractor tikaExtractor = null;
	
	private XmlExtractor xmlExtractor = null;
	
	private HtmlExtractor htmlExtractor = null;
	
	private BagItExtractor bagItExtractor = null;
	
	private JsonFeaturesExtractor jsonFeaturesExtractor = null; 

	private JsonExtractor jsonExtractor = null; 

//	static {
//		try {
////			DetectorFactory.loadProfiles("af","am","ar","az","be","bg","bn","bo","ca","cs","cy","da","de","dv","el","en","es","et","eu","fa","fi","fo","fr","ga","gn","gu","he","hi","hr","hu","hy","id","is","it","ja","jv","ka","kk","km","kn","ko","ky","lb","lij","ln","lt","lv","mi","mk","ml","mn","mr","mt","my","ne","nl","no","os","pa","pl","pnb","pt","qu","ro","ru","si","sk","so","sq","sr","sv","sw","ta","te","th","tk","tl","tr","tt","ug","uk","ur","uz","vi","yi","yo","zh-cn","zh-tw");
//		} catch (LangDetectException e) {
//			throw new IllegalStateException("Unable to initiate language detection profiles", e);
//		}
//	}

	
	public StoredDocumentSourceExtractor(
			StoredDocumentSourceStorage storedDocumentSourceStorage,
			FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
		
	}
	
	public List<StoredDocumentSource> getExtractedStoredDocumentSources(List<StoredDocumentSource> storedDocumentSources) throws IOException {
		boolean verbose = parameters.getParameterBooleanValue("verbose");
		
		/*
		// we will run in parallel, but since order matters, we need a map
		Map<String, StoredDocumentSource> map = new HashMap<String, StoredDocumentSource>();
		try {
			storedDocumentSources.parallelStream().forEach(storedDocumentSource -> {
				try {
					if (verbose) {
						System.out.println("Extracting document "+storedDocumentSource.getId()+" ("+storedDocumentSource.getMetadata().getLocation()+")");
					}
					StoredDocumentSource sds = getExtractedStoredDocumentSource(storedDocumentSource);
					if (sds!=null) {
						map.put(storedDocumentSource.getId(), sds);
					}
				} catch (Exception e) {
					throw new RuntimeException("Unable to extract document: "+storedDocumentSource.getId()+" ("+storedDocumentSource.getMetadata().getLocation()+")", e);
				}
			});
		} catch (Exception e) {
			throw new IOException(e.getMessage(), e);
		}
		
		return storedDocumentSources.stream()
			.map(StoredDocumentSource::getId)
			.filter(id -> map.containsKey(id))
			.map(id -> map.get(id))
			.collect(Collectors.toList());
		*/

		List<StoredDocumentSource> extractedStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		int processors = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(processors);
		List<Future<StoredDocumentSource>> list = new ArrayList<Future<StoredDocumentSource>>();
		for (StoredDocumentSource storedDocumentSource : storedDocumentSources) {
			Callable<StoredDocumentSource> worker = new CallableExtractor(this, storedDocumentSource, verbose);
			Future<StoredDocumentSource> submit = executor.submit(worker);
			list.add(submit);	
		}
		try {
			for (Future<StoredDocumentSource> future : list) {
				StoredDocumentSource sds = future.get();
				if (sds!=null) {
					extractedStoredDocumentSources.add(sds);
				}
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			throw new IllegalStateException("An error occurred during multi-threaded document expansion.", e);
		} catch (ExecutionException e) {
			executor.shutdownNow();
			throw new IllegalStateException("An error occurred during multi-threaded document expansion.", e);
		}
		executor.shutdown();
	
		return extractedStoredDocumentSources;

	}

	public StoredDocumentSource getExtractedStoredDocumentSource(
			StoredDocumentSource storedDocumentSource) throws IOException {
		
		DocumentFormat format;
		
		String inputFormatString = parameters.getParameterValue("inputFormat", "").toUpperCase();
		if (inputFormatString.isEmpty()==false && inputFormatString.toUpperCase().equals("PBLIT")==false) {
			format = DocumentFormat.getForgivingly(inputFormatString);
			if (format==DocumentFormat.UNKNOWN) {
				// allow this to be set, especially for XML with an input format definition
				format = storedDocumentSource.getMetadata().getDocumentFormat();
			}
		} else {
			format = storedDocumentSource.getMetadata().getDocumentFormat();
		}
		
		if (format==DocumentFormat.UNKNOWN && storedDocumentSource.getMetadata().getSource()==Source.STRING) {
			// first, try to see if there are XML parameters
			for (String key : parameters.getKeys()) {
				if (key.startsWith("xml") && parameters.getParameterValue(key).isEmpty()==false) {
					format = DocumentFormat.XML;
					break;
				}
			}
			
			// next try reading part of string
			if (format!=DocumentFormat.XML) {
				String string = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId()));
				format = DocumentFormat.fromString(string);
				if (format != DocumentFormat.UNKNOWN) {
					DocumentMetadata metadata = storedDocumentSource.getMetadata();
					metadata.setDefaultFormat(format);
					storedDocumentSourceStorage.updateStoredDocumentSourceMetadata(storedDocumentSource.getId(), metadata);
				}
			}
		}
		
		InputSource extractedInputSource;
		if (format.isXml()) {
			if (xmlExtractor==null) {xmlExtractor = new XmlExtractor(storedDocumentSourceStorage, parameters);}
			extractedInputSource =  xmlExtractor.getExtractableInputSource(storedDocumentSource);
		} else if (format==DocumentFormat.BAGIT) {
			if (bagItExtractor==null) {bagItExtractor = new BagItExtractor(storedDocumentSourceStorage, parameters);}
			extractedInputSource = bagItExtractor.getExtractableInputSource(storedDocumentSource);
		} else if (format==DocumentFormat.JSONFEATURES || format==DocumentFormat.JSONLINESFEATURES) {
			if (jsonFeaturesExtractor==null) {jsonFeaturesExtractor = new JsonFeaturesExtractor(storedDocumentSourceStorage, parameters);}
			extractedInputSource = jsonFeaturesExtractor.getExtractableInputSource(storedDocumentSource);
		} else if (format==DocumentFormat.JSON || format==DocumentFormat.JSONLINES) {
			if (jsonExtractor==null) {jsonExtractor = new JsonExtractor(storedDocumentSourceStorage, parameters);}
			extractedInputSource = jsonExtractor.getExtractableInputSource(storedDocumentSource);
		}
		// for now we'll only use the HTML extractor if there are special queries, otherwise use Tika
		else if (format==DocumentFormat.HTML && getHtmlExtractor().hasQueries()) {
			extractedInputSource = getHtmlExtractor().getExtractableInputSource(storedDocumentSource);
		} else {
			if (tikaExtractor==null) {tikaExtractor = new TikaExtractor(storedDocumentSourceStorage, parameters);}
			extractedInputSource =  tikaExtractor.getExtractableInputSource(storedDocumentSource);
		}
		return storedDocumentSourceStorage.getStoredDocumentSource(extractedInputSource);
	}
	
	private HtmlExtractor getHtmlExtractor() {
		if (htmlExtractor==null) {
			htmlExtractor = new HtmlExtractor(storedDocumentSourceStorage, parameters);
		}
		return htmlExtractor;
	}
	
	private class CallableExtractor implements Callable<StoredDocumentSource> {
		
		private StoredDocumentSourceExtractor extractor;
		private StoredDocumentSource storedDocumentSource;
		private boolean verbose;

		public CallableExtractor(
				StoredDocumentSourceExtractor storedDocumentSourceExtractor,
				StoredDocumentSource storedDocumentSource,
				boolean verbose) {
			this.extractor = storedDocumentSourceExtractor;
			this.storedDocumentSource = storedDocumentSource;
			this.verbose = verbose;
		}

		@Override
		public StoredDocumentSource call() throws Exception {
//			if (verbose) {System.out.println("extracting "+storedDocumentSource.getMetadata());}
			return this.extractor.getExtractedStoredDocumentSource(storedDocumentSource);
		}
		
	}

}

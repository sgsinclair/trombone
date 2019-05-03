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
package org.voyanttools.trombone.input.expand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.tika.io.IOUtils;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.model.CorpusAccess;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * This is the main stored document source expander that calls other expanders
 * as needed. When this class's {#link
 * {@link #getExpandedStoredDocumentSources(StoredDocumentSource)} is called
 * with a stored document source that doesn't need expansion, the same
 * StoredDocumentSource is return (but as part of a list).
 * 
 * @author Stéfan Sinclair
 */
public class StoredDocumentSourceExpander implements Expander {

	/**
	 * all parameters sent, only some of which may be relevant to some expanders
	 */
	private FlexibleParameters parameters;

	/**
	 * the storage strategy to use for storing document sources
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;

	/**
	 * the expander for compressed archives
	 */
	private Expander archiveExpander;

	/**
	 * the expander for compressed archives
	 */
	private Expander compressedExpander;

	/**
	 * the expander for XML documents
	 */
	private Expander xmlExpander;

	/**
	 * the expander for HTML documents
	 */
	private Expander htmlExpander;
	
	/**
	 * the expander for XSL documents
	 */
	private Expander xslExpander;

	private Expander obApiSearchJsonExpander;
	
	private Expander bagItExpander;
	
	private Expander jsonLinesFeaturesExpander;
	
	private Expander jsonLinesExpander;
	
	private Expander jsonExpander;

	/**
	 * Create a new instance of this expander with the specified storage
	 * strategy.
	 * 
	 * @param storedDocumentSourceStorage
	 *            the storage handler for document sources
	 */
	public StoredDocumentSourceExpander(
			StoredDocumentSourceStorage storedDocumentSourceStorage) {
		this(storedDocumentSourceStorage, new FlexibleParameters());
	}

	/**
	 * Create a new instance of this expander with the specified storage
	 * strategy.
	 * 
	 * @param storedDocumentSourceStorage
	 *            the storage handler for document sources
	 * @param parameters
	 *            that may be relevant to the expanders
	 */
	public StoredDocumentSourceExpander(
			StoredDocumentSourceStorage storedDocumentSourceStorage,
			FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.archiveExpander = null;
		this.compressedExpander = null;
		this.xmlExpander = null;
		this.htmlExpander = null;
		this.bagItExpander = null;
		this.parameters = parameters;
	}
	
	/*
	public List<StoredDocumentSource> getExpandedStoredDocumentSources(InputSource inputSource) throws IOException {
		List<InputSource> inputSources = new ArrayList<InputSource>();
		inputSources.add(inputSource);
		return getExpandedStoredDocumentSources(inputSources);
	}

	public List<StoredDocumentSource> getExpandedStoredDocumentSources(List<InputSource> inputSources) throws IOException {
		List<StoredDocumentSource> storedDocumentSources = new ArrayList<StoredDocumentSource>();
		ExecutorService executor = Executors.newCachedThreadPool();
		List<Future<StoredDocumentSource>> list = new ArrayList<Future<StoredDocumentSource>>();
		for (InputSource inputSource : inputSources) {
			Callable<StoredDocumentSource> worker = new CallableExpander(this.storedDocumentSourceStorage, inputSource);
			Future<StoredDocumentSource> submit = executor.submit(worker);
			list.add(submit);	
		}
		try {
			for (Future<StoredDocumentSource> future : list) {
				storedDocumentSources.add(future.get());
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException("An error occurred during multi-threaded document expansion.", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("An error occurred during multi-threaded document expansion.", e);
		}
		executor.shutdown();
		return storedDocumentSources;
	}
	*/
	
	public List<StoredDocumentSource> getExpandedStoredDocumentSources(
			StoredDocumentSource storedDocumentSource) throws IOException {

		List<StoredDocumentSource> storedDocumentSources = new ArrayList<StoredDocumentSource>();

		DocumentFormat format;
		
		format = storedDocumentSource.getMetadata().getDocumentFormat();

		String inputFormatString = parameters.getParameterValue("inputFormat", "").toUpperCase();
		
		if (inputFormatString.isEmpty()==false) {

			if (format!=DocumentFormat.ARCHIVE && format!=DocumentFormat.COMPRESSED) { // make sure it's not container format (where the inputFormat parameters probably applies to the contents, not the container)
				// is it ok to have unrecognized here?
				DocumentFormat f = DocumentFormat.getForgivingly(inputFormatString);
				if (f!=DocumentFormat.UNKNOWN) { // only set if we have a real format (could be an XML profile)
					format = f;
					
				}
			}
		}

		// if we have a string and an unknown format, we have to have a peek
		if (format == DocumentFormat.UNKNOWN && storedDocumentSource.getMetadata().getSource()==Source.STRING) {
			String string = IOUtils.toString(storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId()));
			format = DocumentFormat.fromString(string);
			if (format != DocumentFormat.UNKNOWN) {
				DocumentMetadata metadata = storedDocumentSource.getMetadata();
				metadata.setDefaultFormat(format);
				storedDocumentSourceStorage.updateStoredDocumentSourceMetadata(storedDocumentSource.getId(), metadata);
			}
		}

		if (format==DocumentFormat.BAGIT) {
			storedDocumentSources.addAll(expandBagIt(storedDocumentSource));
		}
		if (format.isArchive()) {
			storedDocumentSources.addAll(expandArchive(storedDocumentSource));
		}
		else if (format == DocumentFormat.COMPRESSED) {
			storedDocumentSources
					.addAll(expandCompressed(storedDocumentSource));
		}
		else if (format == DocumentFormat.XLSX) {
			storedDocumentSources.addAll(expandXsl(storedDocumentSource));
		}
		
		else if (format == DocumentFormat.OBAPISEARCHJSON) {
			storedDocumentSources.addAll(expandObApiSearchJson(storedDocumentSource));
		}

		else if (format.isXml()) {
			storedDocumentSources.addAll(expandXml(storedDocumentSource));
		}
		
		else if (format==DocumentFormat.HTML) {
			storedDocumentSources.addAll(expandHtml(storedDocumentSource));
		}
		
		else if (format==DocumentFormat.JSONLINESFEATURES) {
			// we set the non-consumptive mode password
			this.parameters.setParameter(CorpusAccess.ACCESS.name().toLowerCase()+"Password", Math.random());
			storedDocumentSources.addAll(expandJsonLinesFeatures(storedDocumentSource));
		}
		
		else if (format==DocumentFormat.JSONLINES) {
			storedDocumentSources.addAll(expandJsonLines(storedDocumentSource));
		}
		
		else if (format==DocumentFormat.JSON) {
			storedDocumentSources.addAll(expandJson(storedDocumentSource));
		}

		// no expansion needed or known
		else {
			storedDocumentSources.add(storedDocumentSource);
		}

		return storedDocumentSources;
	}

	private List<StoredDocumentSource> expandObApiSearchJson(StoredDocumentSource storedDocumentSource) throws IOException {
		if (this.obApiSearchJsonExpander==null) {
			this.obApiSearchJsonExpander = new ObApiSearchExpander(storedDocumentSourceStorage, parameters);
		}
		return obApiSearchJsonExpander.getExpandedStoredDocumentSources(storedDocumentSource);
	}

	List<StoredDocumentSource> expandXsl(StoredDocumentSource storedDocumentSource) throws IOException {
		if (this.xslExpander==null) {
			this.xslExpander = new XlsExpander(storedDocumentSourceStorage, parameters);
		}
		return this.xslExpander.getExpandedStoredDocumentSources(storedDocumentSource);
	}

	/**
	 * Expand the specified StoredDocumentSource archive and add it to the
	 * specified list of StoredDocumentSources.
	 * 
	 * @param storedDocumentSource
	 *            the stored document source to expand (or add as is)
	 * @return a list of expanded document sources
	 * @throws IOException
	 *             an IO Exception
	 */
	List<StoredDocumentSource> expandArchive(
			StoredDocumentSource storedDocumentSource) throws IOException {
		if (this.archiveExpander == null) {
			this.archiveExpander = new ArchiveExpander(
					storedDocumentSourceStorage, this, parameters);
		}
		return this.archiveExpander
				.getExpandedStoredDocumentSources(storedDocumentSource);
	}

	/**
	 * Expand the specified StoredDocumentSource archive and add it to the
	 * specified list of StoredDocumentSources.
	 * 
	 * @param storedDocumentSource
	 *            the stored document source to expand (or add as is)
	 * @return a list of expanded document sources
	 * @throws IOException
	 *             an IO Exception
	 */
	List<StoredDocumentSource> expandCompressed(
			StoredDocumentSource storedDocumentSource) throws IOException {
		if (this.compressedExpander == null) {
			this.compressedExpander = new CompressedExpander(
					storedDocumentSourceStorage, this);
		}
		return this.compressedExpander
				.getExpandedStoredDocumentSources(storedDocumentSource);
	}

	/**
	 * Expand the specified StoredDocumentSource archive and add it to the
	 * specified list of StoredDocumentSources.
	 * 
	 * @param storedDocumentSource
	 *            the stored document source to expand (or add as is)
	 * @return a list of expanded document sources
	 * @throws IOException
	 *             an IO Exception
	 */
	List<StoredDocumentSource> expandXml(
			StoredDocumentSource storedDocumentSource) throws IOException {
		if (this.xmlExpander == null) {
			this.xmlExpander = new XmlExpander(storedDocumentSourceStorage,
					parameters);
		}
		// this will deal fine when no expansion is needed
		return this.xmlExpander.getExpandedStoredDocumentSources(storedDocumentSource);
	}
	
	/**
	 * Expand the specified StoredDocumentSource archive and add it to the
	 * specified list of StoredDocumentSources.
	 * 
	 * @param storedDocumentSource
	 *            the stored document source to expand (or add as is)
	 * @return a list of expanded document sources
	 * @throws IOException
	 *             an IO Exception
	 */
	List<StoredDocumentSource> expandHtml(
			StoredDocumentSource storedDocumentSource) throws IOException {
		if (this.htmlExpander == null) {
			this.htmlExpander = new HtmlExpander(storedDocumentSourceStorage,
					parameters);
		}
		// this will deal fine when no expansion is needed
		return this.htmlExpander.getExpandedStoredDocumentSources(storedDocumentSource);
	}

	/**
	 * Expand the specified StoredDocumentSource archive and add it to the
	 * specified list of StoredDocumentSources.
	 * 
	 * @param storedDocumentSource
	 *            the stored document source to expand (or add as is)
	 * @return a list of expanded document sources
	 * @throws IOException
	 *             an IO Exception
	 */
	List<StoredDocumentSource> expandBagIt(
			StoredDocumentSource storedDocumentSource) throws IOException {
		if (this.bagItExpander == null) {
			this.bagItExpander = new BagItExpander(storedDocumentSourceStorage, parameters);
		}
		return this.bagItExpander.getExpandedStoredDocumentSources(storedDocumentSource);
	}

	List<StoredDocumentSource> expandJsonLinesFeatures(
			StoredDocumentSource storedDocumentSource) throws IOException {
		if (this.jsonLinesFeaturesExpander == null) {
			this.jsonLinesFeaturesExpander = new JsonLinesFeaturesExpander(storedDocumentSourceStorage, parameters);
		}
		return this.jsonLinesFeaturesExpander.getExpandedStoredDocumentSources(storedDocumentSource);
	}
	
	List<StoredDocumentSource> expandJsonLines(
			StoredDocumentSource storedDocumentSource) throws IOException {
		if (this.jsonLinesExpander == null) {
			this.jsonLinesExpander = new JsonLinesExpander(storedDocumentSourceStorage, parameters);
		}
		return this.jsonLinesExpander.getExpandedStoredDocumentSources(storedDocumentSource);
	}
	
	List<StoredDocumentSource> expandJson(
			StoredDocumentSource storedDocumentSource) throws IOException {
		if (this.jsonExpander == null) {
			this.jsonExpander = new JsonExpander(storedDocumentSourceStorage, parameters);
		}
		return this.jsonExpander.getExpandedStoredDocumentSources(storedDocumentSource);
	}
	
	private class CallableExpander implements Callable<StoredDocumentSource> {

		private StoredDocumentSourceStorage storedDocumentSourceStorage;
		private InputSource inputSource;
		public CallableExpander(StoredDocumentSourceStorage storedDocumentSourceStorage,
				InputSource inputSource) {
			this.storedDocumentSourceStorage = storedDocumentSourceStorage;
			this.inputSource = inputSource;
		}

		@Override
		public StoredDocumentSource call() throws Exception {
			return this.storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		}
		
	}
}

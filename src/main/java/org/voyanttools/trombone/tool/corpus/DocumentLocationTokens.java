/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.codec.digest.DigestUtils;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.DocumentLocationToken;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.nlp.CrimGeonameAnnotator;
import org.voyanttools.trombone.nlp.GeonamesAnnotator;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.Storage.Location;
import org.voyanttools.trombone.tool.progress.Progress;
import org.voyanttools.trombone.tool.progress.Progress.Status;
import org.voyanttools.trombone.tool.progress.Progressable;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public class DocumentLocationTokens extends AbstractTerms implements Progressable {
	
	@XStreamOmitField
	private Source source;
	
	private Progress progress = null;
	
	private List<DocumentLocationToken> locations = new ArrayList<DocumentLocationToken>();
	
	private enum Source {
		GEONAMES, CRIM;
		private static Source valueOfForgivingly(String source) {
			if (source!=null) {
				for (Source s : Source.values()) {
					if (s.name().equalsIgnoreCase(source)) {
						return s;
					}
				}
			}
			return GEONAMES;
		}
	}

	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentLocationTokens(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		source = Source.valueOfForgivingly(parameters.getParameterValue("source",""));
	}

	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		List<DocumentLocationToken> tokens = getLocationTokens(corpusMapper);
		if (tokens!=null) {
			fillLocations(tokens);
		}
	}

	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		List<DocumentLocationToken> tokens = getLocationTokens(corpusMapper);
		if (tokens!=null) {
			fillLocations(tokens);
		}
	}
	
	private void fillLocations(List<DocumentLocationToken> tokens) {
		for (int i=0, len=tokens.size(); i<len; i++) {
			if (i>=start) {
				locations.add(tokens.get(i));
			}
			if (locations.size()>limit) {break;}
		}
	}
	
	List<DocumentLocationToken> getLocationTokens(CorpusMapper corpusMapper) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("preferredCoordinates").append(parameters.getParameterValue("preferredCoordinates", ""));
		String id = "locationTokens-"+getVersion()+"-"+corpusMapper.getCorpus().getId()+"-"+source.name()+DigestUtils.md5Hex(sb.toString());
		if (storage.isStored(id, Location.cache)) {
			try {
				return (List<DocumentLocationToken>) storage.retrieve(id, Location.cache);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		} else {
			progress = Progress.retrieve(storage, id);
			if (progress.isNew()) {
				Source source = Source.valueOfForgivingly(parameters.getParameterValue("source", ""));
				switch(source) {
					case CRIM:
						getCrimLocationTokens(corpusMapper, progress);
						break;
					default:
						getGeonamesLocationTokens(corpusMapper, progress);
						break;
				}
			}
			return null;
		}
	}
	
	private void getCrimLocationTokens(CorpusMapper corpusMapper, Progress progress) throws IOException {
		String configFilename = System.getProperty("ca.crim.nlp.configfile","");
		if (configFilename.isEmpty()) {
			throw new IllegalStateException("No configuration file exists for CRIM NLP.");
		}
		File configFile = new File(configFilename);
		if (configFile.exists()==false) {
			throw new IllegalArgumentException("Configuration file not found for CRIM NLP.");
		}
		CrimGeonameAnnotator annotator = new CrimGeonameAnnotator(configFile, "en");
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				List<DocumentLocationToken> tokens;
				try {
					// TODO, it might be better to do this one document at a time
					progress.update(.5f, Status.RUNNING, "geolocationRunning", "Geolocation is running.");
					tokens = annotator.getDocumentLocationTokens(corpusMapper, parameters, progress);
					storage.store(tokens, progress.getId(), Location.cache);
					progress.update(1, Status.FINISHED, "geolocationFinished", "Geolocation has completed.");
				} catch (Exception e) {
					try {
						progress.update(1, Status.ABORTED, "geolocationException", "Geolocation has failed. "+e.getMessage());
					} catch (IOException e1) {
					}
					throw new RuntimeException(e);
				} finally {
					executorService.shutdown();
				}
			}
		});
	}
	
	private void getGeonamesLocationTokens(CorpusMapper corpusMapper, Progress progress) {
		ExecutorService executorService = Executors.newSingleThreadExecutor();

		executorService.execute(new Runnable() {
			@Override
			public void run() {
				GeonamesAnnotator annotator = new GeonamesAnnotator(storage, parameters);
				List<DocumentLocationToken> tokens;
				try {
					// TODO, it might be better to do this one document at a time
					progress.update(.5f, Status.RUNNING, "geolocationRunning", "Geolocation is running.");
					tokens = annotator.getDocumentLocationTokens(corpusMapper, parameters);
					storage.store(tokens, progress.getId(), Location.cache);
					progress.update(1, Status.FINISHED, "geolocationFinished", "Geolocation has completed.");
				} catch (Exception e) {
					try {
						progress.update(1, Status.ABORTED, "geolocationException", "Geolocation has failed. "+e.getMessage());
					} catch (IOException e1) {
					}
					throw new RuntimeException(e);
				} finally {
					executorService.shutdown();
				}
			}
		});
	}

	@Override
	public Progress getProgress() {
		return progress;
	}
}

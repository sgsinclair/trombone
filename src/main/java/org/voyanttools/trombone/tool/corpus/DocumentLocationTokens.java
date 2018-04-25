/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.DocumentLocationToken;
import org.voyanttools.trombone.model.Keywords;
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
		GEONAMES;
		private static Source valueOfForgivingly(String source) {
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
		String id = "locationTokens-"+getVersion()+"-"+corpusMapper.getCorpus().getId()+"-"+source.name();
		if (storage.isStored(id, Location.cache)) {
			try {
				return (List<DocumentLocationToken>) storage.retrieve(id, Location.cache);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		} else {
			progress = Progress.retrieve(storage, id);
			if (progress.isNew()) {
				Executors.newSingleThreadExecutor().execute(new Runnable() {
					@Override
					public void run() {
						GeonamesAnnotator annotator = new GeonamesAnnotator(storage, parameters);
						List<DocumentLocationToken> tokens;
						try {
							// TODO, it might be better to do this one document at a time
							progress.update(.5f, Status.RUNNING, "geolocationRunning", "Geolocation is running.");
							tokens = annotator.getDocumentLocationTokens(corpusMapper, parameters);
							storage.store(tokens, id, Location.cache);
							progress.update(1, Status.FINISHED, "geolocationFinished", "Geolocation has completed.");
						} catch (IOException e) {
							try {
								progress.update(1, Status.ABORTED, "geolocationException", "Geolocation has failed. "+e.getMessage());
							} catch (IOException e1) {
							}
							throw new RuntimeException(e);
						}
					}
				});
			}
			return null;
		}
	}

	@Override
	public Progress getProgress() {
		return progress;
	}
}

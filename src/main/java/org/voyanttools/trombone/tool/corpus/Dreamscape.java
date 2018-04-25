/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Confidence;
import org.voyanttools.trombone.model.CorpusLocation;
import org.voyanttools.trombone.model.CorpusLocationConnection;
import org.voyanttools.trombone.model.DocumentLocationToken;
import org.voyanttools.trombone.model.Location;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.progress.Progress;
import org.voyanttools.trombone.tool.progress.Progressable;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author sgs
 *
 */
@XStreamAlias("dreamscape")
@XStreamConverter(Dreamscape.DreamscapeConverter.class)
public class Dreamscape extends AbstractCorpusTool implements Progressable {
	
	private Progress progress = null;
	
	private List<CorpusLocation> locations = new ArrayList<CorpusLocation>();
	
	private List<CorpusLocationConnection> connections = new ArrayList<CorpusLocationConnection>();
	
	private List<DocumentLocationToken[]> connectionOccurrences = new ArrayList<DocumentLocationToken[]>();
	
	private int total = 0;
	

	public Dreamscape(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		FlexibleParameters params = new FlexibleParameters();
		DocumentLocationTokens documentLocationTokens = new DocumentLocationTokens(storage, params);
		List<DocumentLocationToken> tokens = documentLocationTokens.getLocationTokens(corpusMapper);
		
		if (tokens==null) {
			progress = documentLocationTokens.getProgress();
		} else {
			Map<String, List<String>> locationIdsToFormsMap = new HashMap<String, List<String>>();
			Map<String, Location> locationIdToLocationsMap = new HashMap<String, Location>();
			Map<String, AtomicInteger> locationConnectionToLocationIds = new HashMap<String, AtomicInteger>();
			Location location;
			String id;
			int docIndex = -1;
			String previousId = "";
			DocumentLocationToken previousDocumentLocationToken = null;
			int minPopulation = parameters.getParameterIntValue("minPopulation", 0);
			int start = parameters.getParameterIntValue("start", 0);
			int limit = parameters.getParameterIntValue("limit", Integer.MAX_VALUE);
			for (DocumentLocationToken token : tokens) {
				location = token.getLocation();
				if (location.getPopulation()<minPopulation) {continue;}
				id = location.getId();
				if (locationIdsToFormsMap.containsKey(id)==false) {
					locationIdsToFormsMap.put(id, new ArrayList<String>());
					locationIdToLocationsMap.put(id, location.clone());
				}
				locationIdsToFormsMap.get(id).add(token.getTerm());
				if (token.getDocIndex()>docIndex) {
					docIndex = token.getDocIndex();
					previousId = "";
					previousDocumentLocationToken = null;
				} else if (previousId.isEmpty()==false && previousId.equals(id)==false) {
					String combinedIds = previousId+" - "+id;
					if (locationConnectionToLocationIds.containsKey(combinedIds)==false) {
						locationConnectionToLocationIds.put(combinedIds, new AtomicInteger(1));
					}
					locationConnectionToLocationIds.get(combinedIds).incrementAndGet();
					if (total>=start && connectionOccurrences.size()<=limit) {
						connectionOccurrences.add(new DocumentLocationToken[] {previousDocumentLocationToken, token});
					}
				}
				previousId = id;
				previousDocumentLocationToken = token;
				total++;
			}
			
			// build locations
			if (parameters.getParameterBooleanValue("suppressLocations")==false) {
				for (Map.Entry<String, List<String>> entry : locationIdsToFormsMap.entrySet()) {
					id = entry.getKey();
					List<String> forms = entry.getValue();
					locations.add(
						new CorpusLocation(locationIdToLocationsMap.get(id), forms.stream()
							.distinct()
							.toArray(String[]::new), forms.size())
					);
					
				}
			}
			
			// build connections
			if (parameters.getParameterBooleanValue("suppressConnections")==false) {
				for (Map.Entry<String, AtomicInteger> entry : locationConnectionToLocationIds.entrySet()) {
					Location[] locations = Arrays.stream(entry.getKey().split(" - "))
						.map(s -> locationIdToLocationsMap.get(s))
						.toArray(Location[]::new);
					connections.add(new CorpusLocationConnection(locations, entry.getValue().get()));
				}
			
				Collections.sort(connections); // make sure in order of rawfreq
			}
			
		}
	}

	@Override
	public Progress getProgress() {
		return progress;
	}

	public static class DreamscapeConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return type.isAssignableFrom(Dreamscape.class);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			
			Dreamscape dreamscape = (Dreamscape) source;
			
			if (dreamscape.progress!=null) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "progress", String.class);
				context.convertAnother(dreamscape.progress);
				writer.endNode();
			}
			
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "locations", String.class);
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "total", Integer.class);
	        writer.setValue(String.valueOf(dreamscape.locations.size()));
	        writer.endNode();
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "locations", String.class);
			for (CorpusLocation location : dreamscape.locations) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, location.getId(), String.class);
		        writer.startNode("id");
		        writer.setValue(location.getId());
		        writer.endNode();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
		        writer.setValue(String.valueOf(location.getRawFreq()));
		        writer.endNode();
		        writer.startNode("label");
		        writer.setValue(location.getLocation().getBestName());
		        writer.endNode();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "forms", Map.class);
		        context.convertAnother(location.getForms());
		        writer.endNode();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "lat", Float.class);
		        writer.setValue(String.valueOf(location.getLocation().getLat()));
		        writer.endNode();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "lng", Float.class);
		        writer.setValue(String.valueOf(location.getLocation().getLng()));
		        writer.endNode();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "population", Float.class);
		        writer.setValue(String.valueOf(location.getLocation().getPopulation()));
		        writer.endNode();
		        writer.endNode();
			}
			writer.endNode();
			writer.endNode();

			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "connectionOccurrences", String.class);
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "total", Integer.class);
	        writer.setValue(String.valueOf(dreamscape.total));
	        writer.endNode();
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "connectionOccurrences", Map.class);
			for (DocumentLocationToken[] tokens : dreamscape.connectionOccurrences) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "connectionOccurrence", String.class);
		        int i = 0;
		        for (DocumentLocationToken token : tokens) {
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, i++==0 ? "source" : "target", String.class);
			        writer.startNode("term");
			        writer.setValue(token.getTerm());
			        writer.endNode();
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "docIndex", Integer.class);
			        writer.setValue(String.valueOf(token.getDocIndex()));			        
			        writer.endNode();
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "position", Integer.class);
			        writer.setValue(String.valueOf(token.getPosition()));	       
			        writer.endNode();
			        writer.startNode("location");
			        writer.setValue(token.getLocation().getId());
			        writer.endNode();
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "confidence", Integer.class);
			        writer.setValue(String.valueOf(token.getConfidence()));	       
			        writer.endNode();
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "confidences", String.class);
			        for (Confidence confidence : token.getConfidences()) {
				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, confidence.name(), String.class);
				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "value", Float.class);
				        writer.setValue(String.valueOf(confidence.getValue()));	       
				        writer.endNode();
				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "weight", Float.class);
				        writer.setValue(String.valueOf(confidence.getWeight()));	       
				        writer.endNode();				        
			        		writer.endNode();
			        }
			        writer.endNode();
			        writer.endNode();
		        }
				writer.endNode();
			}
			writer.endNode();
			writer.endNode();
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "connections", String.class);
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "total", Integer.class);
	        writer.setValue(String.valueOf(dreamscape.connections.size()));
	        writer.endNode();
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "connections", Map.class);
			for (CorpusLocationConnection connection : dreamscape.connections) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "connection", String.class);
				Location[] locations = connection.getLocations();
				writer.startNode("source");
		        writer.setValue(locations[0].getId());
		        writer.endNode();
				writer.startNode("target");
		        writer.setValue(locations[1].getId());
		        writer.endNode();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
		        writer.setValue(String.valueOf(connection.getRawFreq()));
		        writer.endNode();
		        writer.endNode();
			}
			writer.endNode();
			writer.endNode();


		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}

/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.LuceneHelper;
import org.voyanttools.trombone.model.Confidence;
import org.voyanttools.trombone.model.CorpusLocation;
import org.voyanttools.trombone.model.CorpusLocationConnection;
import org.voyanttools.trombone.model.DocumentKwicLocationToken;
import org.voyanttools.trombone.model.DocumentLocationToken;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.Kwic;
import org.voyanttools.trombone.model.Location;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.progress.Progress;
import org.voyanttools.trombone.tool.progress.Progressable;
import org.voyanttools.trombone.tool.util.ToolSerializer;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.GeonamesIterator;
import org.voyanttools.trombone.util.Stripper;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
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
	
	private List<DocumentKwicLocationToken[]> connectionOccurrences = new ArrayList<DocumentKwicLocationToken[]>();
	
	private int occurrencesTotal = 0;
	
	private List<DocumentKwicLocationToken> occurrences = new ArrayList<DocumentKwicLocationToken>();
	
	private int total = 0;
	

	public Dreamscape(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		FlexibleParameters params = new FlexibleParameters();
		if (parameters.containsKey("source")) {
			params.setParameter("source", parameters.getParameterValue("source"));
		}
		if (parameters.containsKey("preferredCoordinates")) {
			params.setParameter("preferredCoordinates", parameters.getParameterValue("preferredCoordinates"));
		}
		DocumentLocationTokens documentLocationTokens = new DocumentLocationTokens(storage, params);
		List<DocumentLocationToken> tokens = documentLocationTokens.getLocationTokens(corpusMapper);
		
		if (tokens==null) {
			progress = documentLocationTokens.getProgress();
			return;
		} else {
			
			// determine if we have any overrides
			Map<String, Location> locationIdToLocationsMap = new HashMap<String, Location>();
			Map<String, String> overrides = new HashMap<String, String>();
			if (parameters.containsKey("overridesId")) {
				String overridesString = storage.retrieveString(parameters.getParameterValue("overridesId"), Storage.Location.object);
				JSONObject all = new JSONObject(overridesString);
				for (String key : all.keySet()) {
					String val =  all.getString(key);
					overrides.put(key,val);
					if (val.isEmpty()==false) {
						locationIdToLocationsMap.put(val, null);
					}
				}
			}

			// load any locations that are mapped to ensure we have them
			if (locationIdToLocationsMap.isEmpty()==false) {
				GeonamesIterator iterator = new GeonamesIterator("en");
				Location reusableLocation = null;
				while(iterator.hasNext()) {
					reusableLocation = iterator.next(reusableLocation);
					if (locationIdToLocationsMap.containsKey(reusableLocation.getId())) {
						locationIdToLocationsMap.put(reusableLocation.getId(), reusableLocation.clone());
					}
				}
				iterator.close();
			}
			
			List<DocumentLocationToken[]> connectionOccurrences = new ArrayList<DocumentLocationToken[]>();
			Map<String, List<String>> locationIdsToFormsMap = new HashMap<String, List<String>>();
			Map<String, AtomicInteger> locationConnectionToLocationIds = new HashMap<String, AtomicInteger>();
			Location location;
			String id;
			int docIndex = -1;
			String previousId = "";
			DocumentLocationToken previousDocumentLocationToken = null;
			int minPopulation = parameters.getParameterIntValue("minPopulation", 0);
			int start = parameters.getParameterIntValue("start", 0);
			int limit = parameters.getParameterIntValue("limit", Integer.MAX_VALUE);
			Map<String, List<DocumentLocationToken>> locationIdsToTokens = new HashMap<String, List<DocumentLocationToken>>();
			for (String locationId : parameters.getParameterValues("locationId")) {
				locationIdsToTokens.put(locationId, new ArrayList<DocumentLocationToken>());
			}
			Set<String> connectionIds = new HashSet<String>();
			if (parameters.containsKey("sourceId") && parameters.containsKey("targetId")) {
				connectionIds.add(parameters.getParameterValue("sourceId"));
				connectionIds.add(parameters.getParameterValue("targetId"));
			}
			boolean filterHasLowerCaseForm = parameters.getParameterBooleanValue("filterHasLowerCaseForm");
			boolean filterIsPersonName = parameters.getParameterBooleanValue("filterIsPersonName");
			for (DocumentLocationToken token : tokens) {
				location = token.getLocation();
				if (location.getPopulation()<minPopulation) {continue;}
				if (filterHasLowerCaseForm || filterIsPersonName) {
					boolean skip = false;
					for (Confidence confidence : token.getConfidences()) {
						if ((filterHasLowerCaseForm && confidence.getType()==Confidence.Type.HasLowerCaseForm) ||
								filterIsPersonName && confidence.getType()==Confidence.Type.IsPersonName) {
							skip = true;
							break;
						}
					}
					if (skip) {continue;}
				}
				
				// we have an override for this location
				if (overrides.containsKey(location.getId())) {
					String altId = overrides.get(location.getId());
					if (altId.isEmpty()) {continue;} // empty location mapping, so just ignore this token
					if (locationIdToLocationsMap.containsKey(altId)) {
						location = locationIdToLocationsMap.get(altId);
						if (location!=null) {
							token.setLocation(location);
						} else {
							continue; // this shouldn't happen, so continue
						}
					} else {
						continue; // this shouldn't happen, so continue
					}
					
				}
				id = location.getId();
				
				// we have defined sourceId and targetId but no match
				if (connectionIds.isEmpty()==false && connectionIds.contains(id)==false) {
					continue;
				}

				if (locationIdsToFormsMap.containsKey(id)==false) {
					locationIdsToFormsMap.put(id, new ArrayList<String>());
					locationIdToLocationsMap.put(id, location.clone());
				}
				locationIdsToFormsMap.get(id).add(token.getTerm());
				if (locationIdsToTokens.containsKey(id)) {
					locationIdsToTokens.get(id).add(token);
					occurrencesTotal++;
				}
				
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
			
			//
			if (parameters.getParameterBooleanValue("suppressConnectionOccurrences")==false) {
				Map<Integer, List<DocumentLocationToken[]>> docToTokenConnections = new HashMap<Integer, List<DocumentLocationToken[]>>();
				for (DocumentLocationToken[] connectionOccurrence : connectionOccurrences) {
					docIndex = connectionOccurrence[0].getDocIndex();
					if (docToTokenConnections.containsKey(docIndex)==false) {
						docToTokenConnections.put(docIndex, new ArrayList<DocumentLocationToken[]>());
					}
					docToTokenConnections.get(docIndex).add(connectionOccurrence);
				}
				Set<Integer> positions = new HashSet<Integer>();
				Stripper stripper = new Stripper(Stripper.TYPE.ALL);
				int context = parameters.getParameterIntValue("context", 2);
				for (Map.Entry<Integer, List<DocumentLocationToken[]>> entry : docToTokenConnections.entrySet()) {
					positions.clear();
					for (DocumentLocationToken[] documentLocationToken : entry.getValue()) {
						positions.add(documentLocationToken[0].getPosition());
						positions.add(documentLocationToken[1].getPosition());
					}
					docIndex = entry.getKey();
					IndexedDocument doc = corpusMapper.getCorpus().getDocument(docIndex);
					List<Kwic> kwics = LuceneHelper.getKwicsFromPositions(corpusMapper, doc, TokenType.lexical, positions, context);
					Map<Integer, Kwic> positionToKwic = new HashMap<Integer, Kwic>();
					kwics.forEach(kwic -> positionToKwic.put(kwic.getPosition(), kwic));
					for (DocumentLocationToken[] documentLocationToken : entry.getValue()) {
						Kwic kwic1 = positionToKwic.get(documentLocationToken[0].getPosition());
						Kwic kwic2 = positionToKwic.get(documentLocationToken[1].getPosition());
						this.connectionOccurrences.add(new DocumentKwicLocationToken[]{
							new DocumentKwicLocationToken(
									documentLocationToken[0], stripper.strip(kwic1.getLeft()), stripper.strip(kwic1.getRight())
							),
							new DocumentKwicLocationToken(
									documentLocationToken[1], stripper.strip(kwic2.getLeft()), stripper.strip(kwic2.getRight())
							)
						});
					}
					
				}
			}
			
			if (locationIdsToTokens.isEmpty()==false) {
				Map<Integer, List<DocumentLocationToken>> docToTokens = new HashMap<Integer, List<DocumentLocationToken>>();
				for (List<DocumentLocationToken> tokensList : locationIdsToTokens.values()) {
					for (DocumentLocationToken token : tokensList) {
						docIndex = token.getDocIndex();
						if (docToTokens.containsKey(docIndex)==false) {
							docToTokens.put(docIndex, new ArrayList<DocumentLocationToken>());
						}
						if (docToTokens.size()<=limit) {
							docToTokens.get(docIndex).add(token);
						}
					}
				}
				Set<Integer> positions = new HashSet<Integer>();
				int context = parameters.getParameterIntValue("context", 2);
				Stripper stripper = new Stripper(Stripper.TYPE.ALL);
				for (Map.Entry<Integer, List<DocumentLocationToken>> entry : docToTokens.entrySet()) {
					positions.clear();
					for (DocumentLocationToken documentLocationToken : entry.getValue()) {
						positions.add(documentLocationToken.getPosition());
					}
					docIndex = entry.getKey();
					IndexedDocument doc = corpusMapper.getCorpus().getDocument(docIndex);
					List<Kwic> kwics = LuceneHelper.getKwicsFromPositions(corpusMapper, doc, TokenType.lexical, positions, context);
					Map<Integer, Kwic> positionToKwic = new HashMap<Integer, Kwic>();
					kwics.forEach(kwic -> positionToKwic.put(kwic.getPosition(), kwic));
					for (DocumentLocationToken documentLocationToken : entry.getValue()) {
						Kwic kwic = positionToKwic.get(documentLocationToken.getPosition());
						this.occurrences.add(new DocumentKwicLocationToken(
								documentLocationToken, stripper.strip(kwic.getLeft()), stripper.strip(kwic.getRight())
						));
					}
					
				}

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
			return Dreamscape.class.isAssignableFrom(type);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			
			Dreamscape dreamscape = (Dreamscape) source;
			
			if (dreamscape.progress!=null) {
		        writer.startNode("progress");
				context.convertAnother(dreamscape.progress);
				writer.endNode();
			}
			
			
	        writer.startNode("locations");
	        
	        ToolSerializer.startNode(writer, "total", Integer.class);
	        writer.setValue(String.valueOf(dreamscape.locations.size()));
	        ToolSerializer.endNode(writer);
	        
	        writer.startNode("locations");
			for (CorpusLocation location : dreamscape.locations) {
				writer.startNode(location.getId());
		        
		        writer.startNode("id");
		        writer.setValue(location.getId());
		        writer.endNode();
		        
		        ToolSerializer.startNode(writer, "rawFreq", Integer.class);
		        writer.setValue(String.valueOf(location.getRawFreq()));
		        ToolSerializer.endNode(writer);
		        
		        writer.startNode("label");
		        writer.setValue(location.getLocation().getName());
		        writer.endNode();
		        
		        ToolSerializer.startNode(writer, "forms", Map.class);
		        context.convertAnother(location.getForms());
		        ToolSerializer.endNode(writer);
		        
		        ToolSerializer.startNode(writer, "lat", Float.class);
		        writer.setValue(String.valueOf(location.getLocation().getLat()));
		        ToolSerializer.endNode(writer);
		        
		        ToolSerializer.startNode(writer, "lng", Float.class);
		        writer.setValue(String.valueOf(location.getLocation().getLng()));
		        ToolSerializer.endNode(writer);
		        
		        ToolSerializer.startNode(writer, "population", Float.class);
		        writer.setValue(String.valueOf(location.getLocation().getPopulation()));
		        ToolSerializer.endNode(writer);
		        
		        writer.endNode();
			}
			writer.endNode();
			
			writer.endNode();

			

	        writer.startNode("occurrences");
	        
	        ToolSerializer.startNode(writer, "total", Integer.class);
	        writer.setValue(String.valueOf(dreamscape.occurrences.size()));
	        ToolSerializer.endNode(writer);
	        
	        ToolSerializer.startNode(writer, "occurrences", Map.class);
			for (DocumentKwicLocationToken token : dreamscape.occurrences) {
		        writer.startNode("occurrence");
		        
		        writer.startNode("term");
		        writer.setValue(token.getTerm());
		        writer.endNode();
		        
		        ToolSerializer.startNode(writer, "docIndex", Integer.class);
		        writer.setValue(String.valueOf(token.getDocIndex()));			        
		        ToolSerializer.endNode(writer);
		        
		        ToolSerializer.startNode(writer, "position", Integer.class);
		        writer.setValue(String.valueOf(token.getPosition()));	       
		        ToolSerializer.endNode(writer);
		        
		        writer.startNode("location");
		        writer.setValue(token.getLocation().getId());
		        writer.endNode();
		        
		        ToolSerializer.startNode(writer, "confidence", Integer.class);
		        writer.setValue(String.valueOf(token.getConfidence()));	       
		        ToolSerializer.endNode(writer);
		        
		        writer.startNode("confidences");
		        for (Confidence confidence : token.getConfidences()) {
		        	writer.startNode(confidence.name());
			        
			        ToolSerializer.startNode(writer, "value", Float.class);
			        writer.setValue(String.valueOf(confidence.getValue()));	       
			        ToolSerializer.endNode(writer);
			        
			        ToolSerializer.startNode(writer, "weight", Float.class);
			        writer.setValue(String.valueOf(confidence.getWeight()));	       
			        ToolSerializer.endNode(writer);
			        
		        	writer.endNode();
		        }
		        writer.endNode();
		        
		        writer.startNode("left");
		        writer.setValue(token.getLeft());
		        writer.endNode();
		        
		        writer.startNode("right");
		        writer.setValue(token.getRight());
		        writer.endNode();
		        
		        writer.endNode();
			}
			ToolSerializer.endNode(writer);
			
			writer.endNode();
			
			
			
			writer.startNode("connectionOccurrences");
			
			ToolSerializer.startNode(writer, "total", Integer.class);
	        writer.setValue(String.valueOf(dreamscape.total));
	        ToolSerializer.endNode(writer);
	        
	        ToolSerializer.startNode(writer, "connectionOccurrences", Map.class);
			for (DocumentKwicLocationToken[] tokens : dreamscape.connectionOccurrences) {
		        writer.startNode("connectionOccurrence");
		        
		        int i = 0;
		        for (DocumentKwicLocationToken token : tokens) {
			        writer.startNode(i++==0 ? "source" : "target");
			        
			        writer.startNode("term");
			        writer.setValue(token.getTerm());
			        writer.endNode();
			        
			        ToolSerializer.startNode(writer, "docIndex", Integer.class);
			        writer.setValue(String.valueOf(token.getDocIndex()));			        
			        ToolSerializer.endNode(writer);
			        
			        ToolSerializer.startNode(writer, "position", Integer.class);
			        writer.setValue(String.valueOf(token.getPosition()));	       
			        ToolSerializer.endNode(writer);
			        
			        writer.startNode("location");
			        writer.setValue(token.getLocation().getId());
			        writer.endNode();
			        
			        ToolSerializer.startNode(writer, "confidence", Integer.class);
			        writer.setValue(String.valueOf(token.getConfidence()));	       
			        ToolSerializer.endNode(writer);
			        
			        writer.startNode("confidences");
			        for (Confidence confidence : token.getConfidences()) {
			        	writer.startNode(confidence.name());
				        
				        ToolSerializer.startNode(writer, "value", Float.class);
				        writer.setValue(String.valueOf(confidence.getValue()));	       
				        ToolSerializer.endNode(writer);
				        
				        ToolSerializer.startNode(writer, "weight", Float.class);
				        writer.setValue(String.valueOf(confidence.getWeight()));	       
				        ToolSerializer.endNode(writer);
				        
			        	writer.endNode();
			        }
			        writer.endNode();
			        
			        writer.startNode("left");
			        writer.setValue(token.getLeft());
			        writer.endNode();
			        
			        writer.startNode("right");
			        writer.setValue(token.getRight());
			        writer.endNode();
			        
			        writer.endNode();
		        }
		        
				writer.endNode();
			}
			ToolSerializer.endNode(writer);
			
			writer.endNode();
			
			
			
	        writer.startNode("connections");
	        
	        ToolSerializer.startNode(writer, "total", Integer.class);
	        writer.setValue(String.valueOf(dreamscape.connections.size()));
	        ToolSerializer.endNode(writer);
	        
	        ToolSerializer.startNode(writer, "connections", Map.class);
			for (CorpusLocationConnection connection : dreamscape.connections) {
		        writer.startNode("connection");
		        
				Location[] locations = connection.getLocations();
				
				writer.startNode("source");
		        writer.setValue(locations[0].getId());
		        writer.endNode();
		        
				writer.startNode("target");
		        writer.setValue(locations[1].getId());
		        writer.endNode();
		        
		        ToolSerializer.startNode(writer, "rawFreq", Integer.class);
		        writer.setValue(String.valueOf(connection.getRawFreq()));
		        ToolSerializer.endNode(writer);
		        
		        writer.endNode();
			}
			ToolSerializer.endNode(writer);
			
			writer.endNode();


		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}

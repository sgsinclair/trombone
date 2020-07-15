package org.voyanttools.trombone.nlp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttributeImpl;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.analysis.LexicalAnalyzer;
import org.voyanttools.trombone.model.Confidence;
import org.voyanttools.trombone.model.Confidence.Type;
import org.voyanttools.trombone.model.CorpusTermMinimalsDB;
import org.voyanttools.trombone.model.DocumentLocationToken;
import org.voyanttools.trombone.model.DocumentNgram;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.Kwic;
import org.voyanttools.trombone.model.Location;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.corpus.DocumentContexts;
import org.voyanttools.trombone.tool.corpus.DocumentNgrams;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.GeonamesIterator;

public class GeonamesAnnotator {
	
	private Storage storage;
	private FlexibleParameters parameters;
	
	public GeonamesAnnotator(Storage storage, FlexibleParameters parameters) {
		this.storage = storage;
		this.parameters = parameters;
	}
	
	public List<DocumentLocationToken> getDocumentLocationTokens(CorpusMapper corpusMapper, FlexibleParameters parameters) throws IOException {		CorpusTermMinimalsDB corpusTermMinimalsDB = CorpusTermMinimalsDB.getInstance(corpusMapper, TokenType.lexical); // quick lookup
		IndexSearcher searcher = corpusMapper.getSearcher();
		Analyzer analyzer = new LexicalAnalyzer();
		Location reusableLocation = new Location();
		Map<String, Location> geonameIdToLocation = new HashMap<String, Location>();
		Map<String, Set<String>> formsToGeonameIds = new HashMap<String, Set<String>>();		
		Map<String, String> locationStringToForm = new HashMap<String, String>();
		List<String> words = new ArrayList<String>();
		Set<String> forms = new HashSet<String>();
		Set<String> seenLocations = new HashSet<String>();
		
		Keywords names = new Keywords();
		names.load(corpusMapper.getStorage(), new String[]{"stop.en.common-names.txt"});
		
		// first we go through the corpus to look for location candidates
		for (String lang : corpusMapper.getCorpus().getLanguageCodes()) {
			GeonamesIterator iterator = new GeonamesIterator(lang);
			Keywords stopwords = Keywords.getStopListForLangCode(storage, lang);
			while (iterator.hasNext()) {
				reusableLocation = iterator.next(reusableLocation);
				if (reusableLocation!=null) {
					forms.clear();
					for (String locationString : reusableLocation.getPlaces()) {
						
						// try to skip if we've already seen this form
						if (locationStringToForm.containsKey(locationString)) {
							String form = locationStringToForm.get(locationString);
							formsToGeonameIds.get(form).add(reusableLocation.getId());
		        				if (geonameIdToLocation.containsKey(reusableLocation.getId())==false) {
		        					geonameIdToLocation.put(reusableLocation.getId(), reusableLocation.clone());
		        				}							
							continue;
						} else if (seenLocations.contains(locationString)) {
							continue;
						}
						seenLocations.add(locationString);

						// for every location form, we'll analyze the full form (all words) and
						// make sure that they're all located in our corpus
						words.clear();
						TokenStream tokenStream = analyzer.tokenStream("lexical", locationString+"<!-- lang="+lang+" -->");
						CharTermAttribute term = tokenStream.addAttribute(CharTermAttribute.class);
						tokenStream.reset();
						String word;
						while (tokenStream.incrementToken()) {
							word = term.toString();
							if (corpusTermMinimalsDB.exists(word)) {
								words.add(word);				
							} else {
								words.clear();
								break;
							}
				        }
				        tokenStream.end();
				        tokenStream.close();
				        
				        // if we have words we'll see if it's a single word or a phrase to search
				        if (words.isEmpty()==false) {
				        		String form = null;
				        		if (words.size()==1) {  // add one word if it's not a stopword
				        			if (stopwords.isKeyword(words.get(0))==false) {
					        			form = words.get(0);
				        			}
				        		}
				        		else { // check to see if phrase is in the corpus
				        			 PhraseQuery.Builder builder = new PhraseQuery.Builder();
				        			 for (String w : words) {
				        				 builder.add(new Term(TokenType.lexical.name(), w));
				        			 }
				        			 Query query = builder.build();
				        			TopDocs topDocs = searcher.search(query, 1);
				        			if (topDocs.totalHits.value>0) {
				        				form = StringUtils.join(words, ' ');
				        			}   			
				        		}
				        		if (form!=null) {
				        			locationStringToForm.put(locationString, form);
				        			if (formsToGeonameIds.containsKey(form)==false) {
				        				formsToGeonameIds.put(form, new HashSet<String>());
				        				formsToGeonameIds.get(form).add(reusableLocation.getId());
				        				if (geonameIdToLocation.containsKey(reusableLocation.getId())==false) {
				        					geonameIdToLocation.put(reusableLocation.getId(), reusableLocation.clone());
				        				}
				        			}
				        		}
				        }

					}
				}
			}
			iterator.close();			
		}
		analyzer.close();
		corpusTermMinimalsDB.close();
		List<DocumentLocationToken> documentLocationTokens = new ArrayList<DocumentLocationToken>();
		if (formsToGeonameIds.isEmpty()) {return documentLocationTokens;}

		List<Location> locationsForAveraging = new ArrayList<Location>();
		for (Map.Entry<String, Set<String>> entry : formsToGeonameIds.entrySet()) {
			locationsForAveraging.add(entry.getValue().stream()
				.map(id -> geonameIdToLocation.get(id))
				// this sorting is used for maxPopulation, if it changes, need to make sure population/maxPopulation is not > 1
				.sorted((l1, l2) -> Integer.compare(l2.getPopulation(), l1.getPopulation()))
				.findFirst().get());
		}
		
		String[] preferredCoordinates = parameters.getParameterValue("preferredCoordinates","").split(",");
		
		// determine the very simple mean latitude and longitude (this doesn't take into consideration the shape of the globe)
		double latMean = preferredCoordinates.length==2 ? Double.valueOf(preferredCoordinates[0]) : locationsForAveraging.stream()
			.mapToDouble(Location::getLat)
			.average().getAsDouble();
		// determine the very simple mean latitude and longitude (this doesn't take into consideration the shape of the globe)
		double lngMean = preferredCoordinates.length==2 ? Double.valueOf(preferredCoordinates[1]) : locationsForAveraging.stream()
			.mapToDouble(Location::getLng)
			.average().getAsDouble();
		double maxDistance = locationsForAveraging.stream()
				.mapToDouble(l -> Math.hypot(latMean-l.getLat(), lngMean-l.getLng()))
				.max().getAsDouble();
		
		// get max population (should be highest already since that's how we sorted locationsForAveraging)
		int maxPopulation = locationsForAveraging.stream()
				.mapToInt(Location::getPopulation)
				.max().getAsInt();
		
		String[] queries = formsToGeonameIds.keySet().stream()
				.map(q -> q.indexOf(' ')>-1 ? "\""+q+"\"" : q)
				.sorted((q1, q2) -> Integer.compare(q2.length(), q1.length()))
				.toArray(String[]::new);
		
		FlexibleParameters params = new FlexibleParameters();
		params.setParameter("sort", "DOCINDEXASC");
		params.setParameter("context", 1);		
		DocumentContexts documentContexts = new DocumentContexts(storage, params);
		documentContexts.runQueries(corpusMapper, new Keywords(), queries);
		int pos = -1;
		Pattern punctuationNonWord = Pattern.compile("[.!?][^\\p{L}]*$/");
		Set<Location> locations = new HashSet<Location>();
		int docIndex = -1;
		String docString = "";
		Pattern word = Pattern.compile("\\p{L}+");
		boolean hasLowerCase;
		for (Kwic kwic : documentContexts.getContexts()) {
			if (kwic.getPosition()>=pos) {
				Matcher matcher = word.matcher(kwic.getMiddle());
				hasLowerCase = false;
				while (matcher.find()) {
					if (Character.isUpperCase(matcher.group().charAt(0))==false) {
						hasLowerCase = true;
						break;
					}
				}
				if (hasLowerCase) {
					continue;
				}
				String term = kwic.getTerm();
				pos = kwic.getPosition()+term.split(" ").length;
				if (kwic.getDocIndex()>docIndex) { // update string if we have new doc
					docIndex = kwic.getDocIndex();
					docString = corpusMapper.getCorpus().getDocument(docIndex).getDocumentString();
				}
//				if (Character.isUpperCase(kwic.getMiddle().charAt(0))==false) {continue;}
				List<Confidence> confidences = new ArrayList<Confidence>();
				assert formsToGeonameIds.containsKey(term) : "Can't find term: "+term;
				locations.clear();
				for (String geonameId : formsToGeonameIds.get(term)) {
					assert geonameIdToLocation.containsKey(geonameId) : "Can't find geoname: "+geonameId;
					locations.add(geonameIdToLocation.get(geonameId));
				}
				assert locations.isEmpty()==false;
				
				Location location = locations.stream()
					.sorted((l1, l2) -> Double.compare(Math.hypot(latMean-l1.getLat(), lngMean-l1.getLng()), Math.hypot(latMean-l2.getLat(), lngMean-l2.getLng())))
//					.sorted((l1, l2) -> Integer.compare(l2.getPopulation(),  l1.getPopulation()))
					.findFirst().get();
				double distance = Math.hypot(latMean-location.getLat(), lngMean-location.getLng());
				confidences.add(new Confidence(Type.GeoDistance, (float) (distance/maxDistance)));
				confidences.add(new Confidence(Type.Population, (float) location.getPopulation()/maxPopulation));
				confidences.add(new Confidence(Type.InitialUppercase));
				confidences.add(new Confidence(Type.GeonamesLookup));
				if (names.isKeyword(term)) {
					confidences.add(new Confidence(Type.IsPersonName));
				}
				if (punctuationNonWord.matcher(kwic.getLeft()).find()) {
					confidences.add(new Confidence(Confidence.Type.PrecededByPunctuation));
				}
				if (docString.contains(term.toLowerCase())) {
					confidences.add(new Confidence(Confidence.Type.HasLowerCaseForm));
				}
				if (term.indexOf(' ')>-1) {
					confidences.add(new Confidence(Confidence.Type.IsMultiTerm));
				}
				DocumentLocationToken docLocationToken = new DocumentLocationToken(kwic.getDocIndex(), kwic.getMiddle(), kwic.getPosition(), confidences.toArray(new Confidence[0]), location.clone());
				documentLocationTokens.add(docLocationToken);
			}
		}
		
		// now we go through individual documents to look for our word forms
		
		return documentLocationTokens;
	}
	
	
	
}

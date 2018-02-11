/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.vectorhighlight.FieldTermStack.TermInfo;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.analysis.LexicalAnalyzer;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusTermMinimalsDB;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.Storage.Location;
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
@XStreamAlias("geonames")
@XStreamConverter(Geonames.GeonamesConverter.class)
public class Geonames extends AbstractContextTerms {
	
	private int minPopulation;
	
	List<Map.Entry<City, Integer>> citiesCountList = new ArrayList<Map.Entry<City, Integer>>();
	List<Map.Entry<String, AtomicInteger>> connectionsCount = new ArrayList<Map.Entry<String, AtomicInteger>>();
	List<ConnectionOccurrence> connectionOccurrences = new ArrayList<ConnectionOccurrence>();
	
	/**
	 * @param storage
	 * @param parameters
	 */
	public Geonames(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		minPopulation = parameters.getParameterIntValue("minPopulation", 0);
	}

	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {

		boolean acceptAllDocs = queries==null || queries.length==0;
		HashSet<Integer> docIndices = new HashSet<Integer>();
		if (!acceptAllDocs) {
			FlexibleParameters params = new FlexibleParameters();
			params.setParameter("includeDocIds", "true");
			DocumentsFinder finder = new DocumentsFinder(storage, params);
			finder.runQueries(corpusMapper, stopwords, queries);
			for (String[] ids : finder.queryDocumentidsMap.values()) {
				for (String id : ids) {
					if (id!=null) {
						docIndices.add(corpusMapper.getCorpus().getDocumentPosition(id));
					}
				}
			}
		}
		
		// build a set of valid cities
		Map<String, City> cities = getAllCorpusCities(corpusMapper).stream()
				.filter(city -> city.population>minPopulation)
				.collect(Collectors.toMap(City::getId, c->c));

		CityOccurrenceIterator<CityOccurrence> cityOccurrenceIterator = getCityOccurrenceIterator(corpusMapper);
		CityOccurrence cityOccurrence;
		CityOccurrence previousCityOccurrence = null;
		Map<String, AtomicInteger> citiesIdCount = new HashMap<String, AtomicInteger>();
		Map<String, AtomicInteger> connectionsCount = new HashMap<String, AtomicInteger>();
		int counter = 0;
		while(cityOccurrenceIterator.hasNext()) {
			cityOccurrence = cityOccurrenceIterator.next();
			if (cityOccurrence!=null && (acceptAllDocs || docIndices.contains(cityOccurrence.docIndex)) && cities.containsKey(cityOccurrence.id) && counter>=start && connectionOccurrences.size()<limit) {
				if (citiesIdCount.containsKey(cityOccurrence.id)==false) {
					citiesIdCount.put(cityOccurrence.id, new AtomicInteger(1));
				} else {
					citiesIdCount.get(cityOccurrence.id).incrementAndGet();
				}
				if (previousCityOccurrence!=null && previousCityOccurrence.docIndex==cityOccurrence.docIndex && previousCityOccurrence.id!=cityOccurrence.id) {
					String cid = StringUtils.joinWith("-", previousCityOccurrence.id, cityOccurrence.id);
					if (connectionsCount.containsKey(cid)==false) {connectionsCount.put(cid, new AtomicInteger(1));}
					else {connectionsCount.get(cid).incrementAndGet();}
					connectionOccurrences.add(new ConnectionOccurrence(previousCityOccurrence, cityOccurrence));
				}
				previousCityOccurrence = cityOccurrence;
			}
			counter++;
			total++;
		}
		
		if (parameters.getParameterBooleanValue("suppressCities")!=true) {
			Map<City, Integer> citiesCount = new HashMap<City, Integer>();
//			Collection<City> cities = getAllCorpusCities(corpusMapper);
			for (City city : cities.values()) {
				if (citiesIdCount.containsKey(city.id)) {
					citiesCount.put(city, citiesIdCount.get(city.id).get());
				}
			}
			citiesCountList = citiesCount.entrySet().stream()
					.sorted((c1, c2) -> c1.getValue()==c2.getValue() ? c1.getKey().label.compareTo(c2.getKey().label): Integer.compare(c2.getValue(), c1.getValue()))
					.collect(Collectors.toList());			
		}
		
		if (parameters.getParameterBooleanValue("suppressConnectionCounts")!=true) {
			this.connectionsCount = connectionsCount.entrySet().stream()
					.sorted((c1, c2) -> Integer.compare(c2.getValue().get(), c1.getValue().get()))
					.collect(Collectors.toList());
		}
		
	}

	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		runQueries(corpusMapper, stopwords, new String[0]);
	}
	
	private CityOccurrenceIterator<CityOccurrence> getCityOccurrenceIterator(CorpusMapper corpusMapper) throws IOException {
		String id = corpusMapper.getCorpus().getId()+"-geonamescityoccurrences-"+getVersion();
		if (storage.isStored(id, Location.cache)) {
			Reader reader = storage.getStoreReader(id, Location.cache);
			return new StoredCityOccurrenceIterator(reader); 
		}
		Collection<CityOccurrence> cityOccurrences = getAllCityOccurrences(corpusMapper);
		
		// store for next time
		Writer writer = storage.getStoreWriter(id, Location.cache);
		for (CityOccurrence cityOccurrence : cityOccurrences) {
			writer.write(cityOccurrence.toString()+"\n");
		}
		writer.close();
		
		return new IterableCityOccurrenceIterator(cityOccurrences);
		
	}
	
	private Collection<CityOccurrence> getAllCityOccurrences(CorpusMapper corpusMapper) throws IOException {
		Collection<City> cities = getAllCorpusCities(corpusMapper);
		
		// organize by form while checking population
		Map<String, City> citiesByForm = new HashMap<String, City>();
		for (City city : cities) {
			for (String form : city.forms) {
				if (citiesByForm.containsKey(form)==false || citiesByForm.get(form).population<city.population) {
					citiesByForm.put(form, city);
				}
			}
		}
				
		// sort forms by length to find longer terms first
		String[] queries = citiesByForm.keySet().stream()
				.map(s -> s.indexOf(' ')>-1 ? '"'+s+'"' : s)
				.sorted((s1, s2) -> Integer.compare(s2.length(), s1.length()))
				.toArray(String[]::new);
		
		int[] totalTokens = corpusMapper.getCorpus().getLastTokenPositions(TokenType.lexical);
		Map<Integer, List<DocumentSpansData>> documentSpansDataMap = this.getDocumentSpansData(corpusMapper, queries);
		Pattern quotePattern = Pattern.compile("\"");
		List<CityOccurrence> cityOccurrences = new ArrayList<CityOccurrence>();
		for (Map.Entry<Integer, List<DocumentSpansData>> dsd : documentSpansDataMap.entrySet()) {
			Set<Integer> positions = new HashSet<Integer>();
			List<DocumentSpansData> dsdList = dsd.getValue();
			// make sure we sort by longest so that New York comes before York
			Collections.sort(dsdList, (sd1, sd2) -> Integer.compare(sd2.queryString.length(), sd1.queryString.length()));
			int luceneDoc = dsd.getKey();
			int corpusDocIndex = corpusMapper.getDocumentPositionFromLuceneId(luceneDoc);
			String document = corpusMapper.getCorpus().getDocument(corpusDocIndex).getDocumentString();
			int lastToken = totalTokens[corpusDocIndex];
			Map<Integer, TermInfo> termsOfInterest = getTermsOfInterest(corpusMapper.getLeafReader(), luceneDoc, lastToken, dsd.getValue(), false);
			for (DocumentSpansData dsdItem : dsdList) {
				String form = quotePattern.matcher(dsdItem.queryString).replaceAll("");
				City city = citiesByForm.get(form);
				if (city==null) {
					continue;
				}				
				for(int[] sd : dsdItem.spansData) {
					boolean hasOverlap = false;
					for (int i=sd[0]; i<sd[1]; i++) {
						if (positions.contains(i) || Character.isLowerCase(document.charAt(termsOfInterest.get(i).getStartOffset()))) {
							hasOverlap=true;
							break;
						}
					}
					if (!hasOverlap) { // check all positions before filling
						for (int i=sd[0]; i<sd[1]; i++) {
							positions.add(i);
						}
						List<String> words = IntStream.range(sd[0], sd[1]).mapToObj(i -> termsOfInterest.get(i).getText()).collect(Collectors.toList());
						StringUtils.join(words, ' ');
						cityOccurrences.add(new CityOccurrence(corpusDocIndex, sd[0], city.id, StringUtils.join(words, ' ')));
					}
				}
			}
		}
		
		// order by id and position
		Collections.sort(cityOccurrences);
		return cityOccurrences;

	}
	
	private Collection<City> getAllCorpusCities(CorpusMapper corpusMapper) throws IOException {
		
		Corpus corpus = corpusMapper.getCorpus();
		
		// try to load cached collection
		String id = corpus.getId()+"-geonamesallpossiblecorpuscities-"+getVersion();
		if (storage.isStored(id, Location.cache)) {
			try {
				return (Collection<City>) storage.retrieve(id, Location.cache);
			} catch (ClassNotFoundException e) {
				// fail silently but proceed to create a new cache
			}
		}
		
		Keywords stop = new Keywords();
		stop.load(storage, new String[]{"stop.en.people.txt","stop.en.taporware.txt"});

		
		Map<String, City> citiesById = new HashMap<String, City>();
		CorpusTermMinimalsDB corpusTermMinimalsDB = CorpusTermMinimalsDB.getInstance(corpusMapper, TokenType.lexical); // quick lookup
		Pattern tabPattern = Pattern.compile("\t");
		Set<String> cityPhrases = new HashSet<String>();
		IndexSearcher searcher = corpusMapper.getSearcher();
		for (String lang : corpus.getLanguageCodes()) {
			Analyzer analyzer = new LexicalAnalyzer();
			try(InputStream is = getClass().getResourceAsStream("/org/voyanttools/trombone/geonames/"+lang+".txt")) {
				for (String line : IOUtils.readLines(is)) {
					cityPhrases.clear();
					String[] parts = tabPattern.split(line);
					Set<String> forms = new HashSet<String>();
					// 5=city,6=admin,7=country
					String[] cityParts = StringUtils.split(parts[5], '+');
					for (String c : cityParts) {
						// make sure length of three and with letters
						if (c.length()>2 && c.chars().anyMatch(i -> Character.isLetter(i))) {
							forms.add(c);
						}
					}
					if (forms.isEmpty()) {
						continue;
					}
					for (int i=6; i<8; i++) {
						String[] admins = StringUtils.split(parts[i], '+');
						for (String c : cityParts) {
							for (String a : admins) {
								forms.add(c+" "+a);
							}
						}
						// for admin and country send city + admin or country
					}
					for (String form : forms) {
						if (stop.isKeyword(form)==false) {
							List<String> words = getRecognizedTerms(searcher, analyzer, corpusTermMinimalsDB, form, lang);
							if (words!=null) {cityPhrases.addAll(words);}
						}
					}
					if (cityPhrases.isEmpty()==false) {
						if (citiesById.containsKey(parts[0])) {
							citiesById.get(parts[0]).forms.addAll(cityPhrases);
						} else {
							citiesById.put(parts[0], new City(parts[0], parts[1], parts[2], parts[3], Integer.parseInt(parts[4]), cityPhrases));
						}
					}
				}
			} catch (IOException e) {
				// fail quietly
			}
		}
		
		// try to cull cities by population
		Map<String, City> citiesByLabel = new HashMap<String, City>();
		for (City city : citiesById.values()) {
			for (String form : city.forms) {
				if (citiesByLabel.containsKey(form)) {
					if (city.population>citiesByLabel.get(form).population) {
						citiesByLabel.put(form, city);
					}
				} else {
					citiesByLabel.put(form, city);
				}
			}
		}
		
		Collection<City> cities = citiesByLabel.values().stream().collect(Collectors.toSet());
		storage.store(cities, id, Location.cache);
		return cities;
		
	}
	
	private List<String> getRecognizedTerms(IndexSearcher searcher, Analyzer analyzer, CorpusTermMinimalsDB corpusTermMinimalsDB, String text, String lang) throws IOException {
		List<String> phrases = new ArrayList<String>();
		List<String> words = new ArrayList<String>();
		for (String t : StringUtils.split(text, '+')) {
			if (t.trim().length()==0) {continue;}
			TokenStream tokenStream = analyzer.tokenStream("lexical", t+"<!-- lang="+lang+" -->");
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
	        if (words.isEmpty()==false) {
	        		if (words.size()==1) {phrases.add(words.get(0));} // add one word
	        		else { // check to see if phrase is in the corpus
	        			 PhraseQuery.Builder builder = new PhraseQuery.Builder();
	        			 for (String w : words) {
	        				 builder.add(new Term(TokenType.lexical.name(), w));
	        			 }
	        			 Query query = builder.build();
	        			TopDocs topDocs = searcher.search(query, 1);
	        			if (topDocs.totalHits>0) {
	        				phrases.add(StringUtils.join(words, ' '));
	        			}   			
	        		}
	        }
		}
		
		return phrases.isEmpty() ? null : phrases;
	}

	private static class City implements Serializable {
		
		private String id;
		private String label;
		private String latitude;
		private String longitude;
		private int population;
		private Set<String> forms;
		private City(String id, String label, String latitude, String longitude, int population, Set<String> forms) {
			this.id = id;
			this.label = label;
			this.latitude = latitude;
			this.longitude = longitude;
			this.population = population;
			this.forms = new HashSet<String>(forms);
		}
		
		private String getId() {
			return id;
		}
		public String toString() {
			return label+" ("+id+": "+population+") "+StringUtils.join(forms, ", ");
		}
	}
	
	private class CityOccurrence implements Comparable<CityOccurrence> {
		private int docIndex;
		private int position;
		private String id;
		private String form;
		private CityOccurrence(int docIndex, int position, String id, String form) {
			this.docIndex = docIndex;
			this.position = position;
			this.id = id;
			this.form = form;
		}
		@Override
		public int compareTo(CityOccurrence o) {
			return docIndex==o.docIndex ? Integer.compare(position, o.position) : Integer.compare(docIndex, o.docIndex);
		}
		
		public String toString() {
			return StringUtils.joinWith("\t", docIndex, position, id, form);
		}
	}
	
	public interface CityOccurrenceIterator<CityOccurrence> extends Iterator<CityOccurrence>, AutoCloseable {
	}
	
	private class StoredCityOccurrenceIterator implements CityOccurrenceIterator<CityOccurrence> {
		
		private BufferedReader reader;
		private StoredCityOccurrenceIterator(Reader reader) {
			this.reader = new BufferedReader(reader);
		}

		@Override
		public boolean hasNext() {
			try {
				if (reader!=null) {
					if (reader.ready()) {return true;}
					reader.close(); reader=null;
				}
			} catch (IOException e) {
			}
			return false;
		}

		@Override
		public CityOccurrence next() {
			String line;
			try {
				line = reader.readLine();
			} catch (IOException e) {
				return null;
			}
			if (line!=null) {
				String[] parts = StringUtils.split(line, "\t");
				if (parts.length==4) {
					return new CityOccurrence(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), parts[2], parts[3]);
				}
				return null;
			}
			if (reader!=null) {try {
				reader.close();
			} catch (IOException e) {
				
			}}
			return null;
		}

		@Override
		public void close() throws Exception {
			if (reader!=null) {reader.close();}
		}
		
	}
	
	private class IterableCityOccurrenceIterator implements CityOccurrenceIterator<CityOccurrence> {
		
		private Iterator<CityOccurrence> iterator;
		private IterableCityOccurrenceIterator(Iterable<CityOccurrence> iterable) {
			iterator = iterable.iterator();
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public CityOccurrence next() {
			return iterator.next();
		}

		@Override
		public void close() throws Exception {
			// ignore
		}
	}
	
	private class ConnectionOccurrence {
		private int docIndex;
		private String sourceId;
		private int sourcePosition;
		private String sourceForm;
		private String targetId;
		private int targetPosition;
		private String targetForm;
		ConnectionOccurrence(CityOccurrence left, CityOccurrence right) {
			if (left.docIndex!=right.docIndex) {throw new IllegalArgumentException("Occurrences must belong to the same document.");}
			docIndex = left.docIndex;
			sourceId = left.id;
			targetId = right.id;
			sourcePosition = left.position;
			targetPosition = right.position;
			sourceForm = left.form;
			targetForm = right.form;
		}
	}
	public static class GeonamesConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return Geonames.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			Geonames geonames = (Geonames) source;
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "cities", String.class);
			for (Map.Entry<City, Integer> cityCount : geonames.citiesCountList) {
				
				City city = cityCount.getKey();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, city.id, String.class); // not written in JSON

		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
				writer.setValue(String.valueOf(cityCount.getValue()));
				writer.endNode();

		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "label", String.class);
				writer.setValue(String.valueOf(city.label));
				writer.endNode();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "latitude", Float.class);
				writer.setValue(String.valueOf(city.latitude));
				writer.endNode();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "longitude", Float.class);
				writer.setValue(String.valueOf(city.longitude));
				writer.endNode();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "population", Integer.class);
				writer.setValue(String.valueOf(city.population));
				writer.endNode();
				
				writer.endNode();
			}
			writer.endNode();
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "connectionCounts", String.class);
			for (Map.Entry<String, AtomicInteger> connectionCount : geonames.connectionsCount) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, connectionCount.getKey(), Integer.class);
				writer.setValue(String.valueOf(String.valueOf(connectionCount.getValue().get())));
				writer.endNode();
			}
			writer.endNode();

	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "connections", String.class);
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "total", Integer.class);
			writer.setValue(String.valueOf(String.valueOf(geonames.total)));
			writer.endNode();

	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "connections", Map.class);
			for (ConnectionOccurrence occurrence : geonames.connectionOccurrences) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "connection", String.class);
		        
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "docIndex", Integer.class);
		        writer.setValue(String.valueOf(occurrence.docIndex));
		        writer.endNode();				
		        
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "source", String.class);
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "id", Integer.class);
		        writer.setValue(occurrence.sourceId);
		        writer.endNode();				
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "position", Integer.class);
		        writer.setValue(String.valueOf(occurrence.sourcePosition));
		        writer.endNode();				
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "form", String.class);
		        writer.setValue(String.valueOf(occurrence.sourceForm));
		        writer.endNode();				
				writer.endNode();
				
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "target", String.class);
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "id", Integer.class);
		        writer.setValue(occurrence.targetId);
		        writer.endNode();				
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "position", Integer.class);
		        writer.setValue(String.valueOf(occurrence.targetPosition));
		        writer.endNode();	
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "form", String.class);
		        writer.setValue(String.valueOf(occurrence.targetForm));
		        writer.endNode();				
				writer.endNode();
				
				writer.endNode();
			}
			writer.endNode();
			writer.endNode();
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader, com.thoughtworks.xstream.converters.UnmarshallingContext)
		 */
		@Override
		public Object unmarshal(HierarchicalStreamReader arg0,
				UnmarshallingContext arg1) {
			// TODO Auto-generated method stub
			return null;
		}

	}
}

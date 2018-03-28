package org.voyanttools.trombone.nlp;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.DocumentEntity;
import org.voyanttools.trombone.model.DocumentToken;
import org.voyanttools.trombone.model.EntityType;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.tool.corpus.DocumentTokens;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.NumberUtils;
import org.voyanttools.trombone.util.Stripper;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinder;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;
import postaggersalanguage.five.POSTaggersALanguage;

public class OpenNlpAnnotator implements NlpAnnotator {

	private POSTaggersALanguage posTaggerAnnotator = null;
	private TokenNameFinderModel locationNameFinderModel = null;
	private String lang;
	
	public OpenNlpAnnotator(String lang) throws IOException {
		this.lang = lang;
	}
	
	public PosLemmas getPosLemmas(String text, String lang) throws IOException {
		ensurePOSTaggerLoaded();
		return posTaggerAnnotator.getLemmatized(text);	
	}
	
	private synchronized void ensurePOSTaggerLoaded() throws IOException {
		if (posTaggerAnnotator==null) {posTaggerAnnotator=new POSTaggersALanguage(lang);}
	}
	
	public String getLang() {
		return lang;
	}

	@Override
	public List<DocumentEntity> getEntities(CorpusMapper corpusMapper, IndexedDocument indexedDocument,
			Collection<EntityType> types, FlexibleParameters parameters) throws IOException {
		if (types!=null && types.size()==1 && types.contains(EntityType.location)) {
			return getLocations(corpusMapper, indexedDocument, parameters);
		}
		return null;
	}
	
	private List<DocumentEntity> getLocations(CorpusMapper corpusMapper, IndexedDocument indexedDocument, FlexibleParameters parameters) throws IOException {
		FlexibleParameters params = new FlexibleParameters();
		params.setParameter("noOthers", "true");		
		DocumentTokens documentTokens = new DocumentTokens(corpusMapper.getStorage(), new FlexibleParameters());
		documentTokens.run(corpusMapper);
		List<DocumentToken> tokens = documentTokens.getDocumentTokens();
		String[] strings = tokens.stream()
			.map(DocumentToken::getTerm)
			.toArray(String[]::new);
		Span[] spans = getLocations(strings);
		Map<String, List<Span>> stringSpans = new HashMap<String, List<Span>>();
		for (Span span : spans) {
			String term = IntStream.range(span.getStart(), span.getEnd())
				.mapToObj(i -> tokens.get(i).getTerm())
				.collect(Collectors.joining(" "));
			if (stringSpans.containsKey(term)==false) {
				stringSpans.put(term, new ArrayList<Span>());
			}
			stringSpans.get(term).add(span);
		}
		int corpusDocumentIndex = corpusMapper.getCorpus().getDocumentPosition(indexedDocument.getId());
		List<DocumentEntity> documentEntities = new ArrayList<DocumentEntity>();
		for (Map.Entry<String, List<Span>> stringSpansEntry : stringSpans.entrySet()) {
			String term = stringSpansEntry.getKey();
			List<Span> spansList = stringSpansEntry.getValue();
			int[] positions = spansList.stream().
					mapToInt(span -> tokens.get(span.getStart()).getPosition()).toArray();
			float[] probabilities = NumberUtils.getFloats(spansList.stream()
					.mapToDouble(span -> span.getProb())
					.toArray());
			Span span = spansList.get(0);
			DocumentEntity documentEntity = new DocumentEntity(corpusDocumentIndex, term, term, EntityType.location, spansList.size(), positions, probabilities);
			documentEntities.add(documentEntity);
		}
		return documentEntities;
	}
	
	private Span[] getLocations(String string) throws IOException {
		Stripper stripper = new Stripper(Stripper.TYPE.ALL);
		string = stripper.strip(string, true);
		Matcher matcher = Pattern.compile("\\p{L}+(['-]\\p{L})?").matcher(string);
		List<String> strings = new ArrayList<String>();
		while (matcher.find()) {
			strings.add(matcher.group());
		}
		return getLocations(strings.toArray(new String[0]));
		
	}
			
	private Span[] getLocations(String[] tokens) throws IOException {
		ensureLocationNameFinderModelLoaded();
		TokenNameFinder finder = new NameFinderME(locationNameFinderModel);
		return finder.find(tokens);
	}

	private synchronized void ensureLocationNameFinderModelLoaded() throws IOException {
		if (locationNameFinderModel==null) {
		      InputStream inputStreamNameFinder = getClass().getResourceAsStream("/org/voyanttools/trombone/nlp/opennlp/"+lang+"-ner-location.bin"); 
		      locationNameFinderModel = new TokenNameFinderModel(inputStreamNameFinder); 
		      inputStreamNameFinder.close();
		}
		
	}
	
	public static void main(String[] args) throws IOException {
		String string = " <p>The city of New York is this is, a London, Ontario, Montreal test</p> ";
		OpenNlpAnnotator annotator = new OpenNlpAnnotator("en");
		Span[] spans = annotator.getLocations(string);
		for (Span span : spans) {
			System.out.println(span);
		}
	}
	
}

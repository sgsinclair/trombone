/**
 * 
 */
package org.voyanttools.trombone.nlp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Confidence;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentLocationToken;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.Location;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;
import org.voyanttools.trombone.tool.corpus.CorpusManager;
import org.voyanttools.trombone.tool.progress.Progress;
import org.voyanttools.trombone.tool.progress.Progress.Status;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.GeonamesIterator;
import org.voyanttools.trombone.util.Stripper;

import ca.crim.nlp.voyant.VoyantPacteClient;

/**
 * @author sgs
 *
 */
public class CrimGeonameAnnotator {
	
	private VoyantPacteClient voyantPacteClient;
	private String lang;

	/**
	 * @throws IOException 
	 * 
	 */
	public CrimGeonameAnnotator(File configFile, String lang) throws IOException {
		voyantPacteClient = new VoyantPacteClient(configFile);
		this.lang = lang;
	}
	
	public List<DocumentLocationToken> getDocumentLocationTokens(CorpusMapper corpusMapper, FlexibleParameters parameters, Progress progress) throws IOException {
		
		progress.update(.1f, Status.RUNNING, "crimZip", "Transferring data to annotation server.");
		File zipFile = getZipFile(corpusMapper);
		Map<String, String> fileToJsonMap = voyantPacteClient.getNERAnnotations(zipFile.toString(), progress);

		progress.update(.5f, Status.RUNNING, "crimProcess", "Processing annotation results.");
		// go through all files to collect relevant geoname IDs and entey maps
		Map<String, List<Map<String, String>>> docIdToEntryMaps = new HashMap<String, List<Map<String, String>>>();
		Set<String> geonameIds = new HashSet<String>();
		for (String jsonString : fileToJsonMap.values()) {
			List<Map<String, String>> entries = parse(jsonString);
			String docId = null;
			for (Map<String, String> entry : entries) {
				String id = entry.get("docId");
				if (docId!=null && docId.equals(id)==false) {
					throw new IllegalStateException("Different document: "+id);
				}
				geonameIds.add(entry.get("geoname"));
				docId = id;
			}
			if (docId!=null && entries.isEmpty()==false) {
				docIdToEntryMaps.put(docId, entries);
			}
		}
		zipFile.delete();
		
		// load locations we need
		progress.update(.8f, Status.RUNNING, "crimProcess", "Processing annotation results.");
		GeonamesIterator iterator = new GeonamesIterator(lang);
		Location reusableLocation = new Location();
		Map<String, Location> locationIdToLocation = new HashMap<String, Location>();
		while (iterator.hasNext()) {
			reusableLocation = iterator.next(reusableLocation);
			if (reusableLocation!=null && geonameIds.contains(reusableLocation.getId())) {
				locationIdToLocation.put(reusableLocation.getId(), reusableLocation.clone());
			}
		}
		iterator.close();

		// go through for every document and create tokens
		List<DocumentLocationToken> tokens = new ArrayList<DocumentLocationToken>();
		for (Map.Entry<String, List<Map<String, String>>> docIdToListEntry : docIdToEntryMaps.entrySet()) {
			String docId = docIdToListEntry.getKey();
			int docIndex = corpusMapper.getCorpus().getDocumentPosition(docId);
			Set<Integer> startOffsets = new HashSet<Integer>();
			docIdToListEntry.getValue().forEach(m -> startOffsets.add(Integer.valueOf(m.get("start"))));
			Map<Integer, Integer> startOffsetToPositionMap = getStartOffsetToPositionMap(corpusMapper, docId, startOffsets);
			for (Map<String, String> map : docIdToListEntry.getValue()) {
				String id = map.get("geoname");
				if (locationIdToLocation.containsKey(id)) {
					int position = startOffsetToPositionMap.get(Integer.valueOf(map.get("start")));
					float confidence = Float.valueOf(map.get("confidence"));
					Confidence[] confidences = new Confidence[] {new Confidence(Confidence.Type.Pacte, confidence)};
					tokens.add(new DocumentLocationToken(docIndex, map.get("term"), position, confidences, locationIdToLocation.get(id)));
				}
			}
		}
		return tokens;
	}
	
	private Map<Integer, Integer> getStartOffsetToPositionMap(CorpusMapper corpusMapper, String docId,
			Set<Integer> startOffsets) throws IOException {
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		int luceneDocId = corpusMapper.getLuceneIdFromDocumentId(docId);
		IndexReader reader = corpusMapper.getIndexReader();
		Terms terms = reader.getTermVector(luceneDocId, TokenType.lexical.name());
		TermsEnum termsEnum = null;
		if (terms!=null) {
			termsEnum = terms.iterator();
			if (termsEnum!=null) {
				BytesRef bytesRef = termsEnum.next();
				while (bytesRef!=null) {
					PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.OFFSETS);
					postingsEnum.nextDoc();
					int freq = postingsEnum.freq();
					for (int i=0; i<freq; i++) {
						int position = postingsEnum.nextPosition();
						int offset = postingsEnum.startOffset();
						if (startOffsets.contains(offset)) {
							map.put(offset, position);
						}
					}
					bytesRef = termsEnum.next();
				}
			}
		}
		return map;
	}

	private List<Map<String, String>> parse(String annotationsString) {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		JSONObject annotations = null;
		try {
			annotations = new JSONObject(annotationsString);
		} catch (JSONException e) {
			return null;
		}
		JSONArray data = annotations.optJSONArray("data");
		if (data!=null) {
			for (int i=0, len=data.length(); i<len; i++) {
				JSONObject annotation = data.getJSONObject(i);
				String uri = annotation.optString("uri");
				if (uri==null || uri.equals("null")) {continue;}
				Map<String, String> map = new HashMap<String, String>();
				String geoname = uri.substring(uri.lastIndexOf('/')+1);
				map.put("geoname", geoname);
				map.put("docId", annotation.getString("_documentID"));
				map.put("term", annotation.getString("text"));
				map.put("confidence", String.valueOf(annotation.getFloat("confidence")));
				JSONArray offsets = annotation.getJSONArray("offsets");
				for (int j=0, jlen=offsets.length(); j<jlen; j++) {
					JSONObject vals = offsets.getJSONObject(j);
					map.put("start", String.valueOf(vals.getInt("begin")));
					map.put("end", String.valueOf(vals.getInt("end")));
					list.add(map);
				}
			}
		}
		return list;
	}
	
	private File getZipFile(CorpusMapper corpusMapper) throws IOException {
		File file = File.createTempFile(UUID.randomUUID().toString(), ".zip");
		
		Stripper stripper = new Stripper(Stripper.TYPE.ALL);
		String string;
		String stripped;
		ZipOutputStream zos = null;
		try {
			FileOutputStream fos = new FileOutputStream(file);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			zos = new ZipOutputStream(bos);
			for (IndexedDocument indexedDocument : corpusMapper.getCorpus()) {
				string = indexedDocument.getDocumentString();
				stripped = stripper.strip(string, true);
				ZipEntry entry = new ZipEntry(indexedDocument.getId()+".txt"); 
				zos.putNextEntry(entry);
				zos.write(stripped.getBytes());
				zos.closeEntry();
			}
			zos.close();
		} finally {
			if (zos!=null) zos.close();
		}	
		return file;
	}
	
	public static void main(String[] args) throws IOException {
		
		CrimGeonameAnnotator annotator = new CrimGeonameAnnotator(new File(args[0]), "en");
		
		/*
		File file = new File("/Users/sgs/Downloads/pacte/out/annotations_00002.txt");
		String string = FileUtils.getStringFromFile(file);
		
		annotator.parse(string);
		System.out.println(annotator.parse(string));
		*/
		

		Storage storage =  new MemoryStorage();
		String text1 = "Most of London, Ontario and Montreal and most of London, England and Montreal and London and Montreal.";
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"string="+text1,"includeCities=true"});
		Corpus corpus = CorpusManager.getCorpus(storage, parameters);
		CorpusMapper corpusMapper = new CorpusMapper(storage, corpus);
		List<DocumentLocationToken> tokens = annotator.getDocumentLocationTokens(corpusMapper, new FlexibleParameters(), Progress.DUMMY);
		for (DocumentLocationToken token : tokens) {
			System.out.println(token);
		}
	}
	
	

}

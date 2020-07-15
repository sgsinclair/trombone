package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.Kwic;
import org.voyanttools.trombone.model.TermInfo;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.FlexibleQueue;
import org.voyanttools.trombone.util.Stripper;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("documentContexts")
public class DocumentContexts extends AbstractContextTerms implements ConsumptiveTool {

	private List<Kwic> contexts = new ArrayList<Kwic>();
	
	@XStreamOmitField
	private Kwic.Sort contextsSort;
	
	@XStreamOmitField
	private Comparator<Kwic> comparator;
	
	@XStreamOmitField
	private Kwic.OverlapStrategy overlapStrategy;

	@XStreamOmitField
	private int perDocLimit;
	
	private boolean accurateTotalNotNeeded;
	
	public DocumentContexts(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		contextsSort = Kwic.Sort.getForgivingly(parameters);
		comparator = Kwic.getComparator(contextsSort);
		overlapStrategy = Kwic.OverlapStrategy.valueOfForgivingly(parameters.getParameterValue("overlapStrategy", ""));
		perDocLimit = parameters.getParameterIntValue("perDocLimit", limit);
		accurateTotalNotNeeded = parameters.getParameterBooleanValue("accurateTotalNotNeeded");
	}
	
	public float getVersion() {
		return super.getVersion()+4;
	}

	private List<Kwic> getKwics(CorpusMapper corpusMapper, Map<Integer, List<DocumentSpansData>> documentSpansDataMap) throws IOException {
		
		int[] totalTokens = corpusMapper.getCorpus().getLastTokenPositions(tokenType);
		FlexibleQueue<Kwic> queue = new FlexibleQueue<Kwic>(comparator, limit == Integer.MAX_VALUE ? limit : start+limit);
		int position = parameters.getParameterIntValue("position", -1);
		for (Map.Entry<Integer, List<DocumentSpansData>> dsd : documentSpansDataMap.entrySet()) {
			int luceneDoc = dsd.getKey();
			int corpusDocIndex = corpusMapper.getDocumentPositionFromLuceneId(luceneDoc);
			int lastToken = totalTokens[corpusDocIndex];
			FlexibleQueue<Kwic> q = getKwics(corpusMapper, dsd.getKey(), corpusDocIndex, lastToken, dsd.getValue());
			int offeredForThisDocument = 0;
			for (Kwic k : q.getUnorderedList()) {
				if (k!=null){
					if (position>-1 && k.getPosition()!=position) {continue;}
					queue.offer(k);
					offeredForThisDocument++;
					// we have enough for this document (and we already have an accurate total count if needed)
					if (perDocLimit<limit && offeredForThisDocument>=perDocLimit) {
						break;
					}
				}
			}
		}
		
		return queue.getOrderedList(start);
	}
	
	
	private FlexibleQueue<Kwic> getKwics(CorpusMapper corpusMapper, int luceneDoc, int corpusDocumentIndex,
			int lastToken, List<DocumentSpansData> documentSpansData) throws IOException {

		int position = parameters.getParameterIntValue("position", -1);
		
		Map<Integer, TermInfo> termsOfInterest = getTermsOfInterest(corpusMapper.getIndexReader(), luceneDoc, lastToken, documentSpansData, overlapStrategy==Kwic.OverlapStrategy.merge);
		
		Stripper stripper = new Stripper(parameters.getParameterValue("stripTags"));

		// build kwics
		FlexibleQueue<Kwic> queue = new FlexibleQueue<Kwic>(comparator, start+limit);
		String document = corpusMapper.getCorpus().getDocument(corpusDocumentIndex).getDocumentString();
		//String document = LeafReader.document(luceneDoc).get(tokenType.name());
		
		// we start by creating a list of all positions in the document, as well as map to help us retrieve the span for each one
		List<int[]> datas = new ArrayList<int[]>();
		Map<Integer, String> queriesMap = new HashMap<Integer, String>();
		
		for (DocumentSpansData dsd : documentSpansData) {
			for (int[] dsddata : dsd.spansData) {
				if (position>-1 && dsddata[0]!=position) {continue;}
				datas.add(dsddata);
				queriesMap.put(dsddata[0], dsd.queryString);
			}
		}
		
		Collections.sort(datas, new Comparator<int[]>() {
		    @Override
		    public int compare(int[] o1, int[] o2) {
		        return o1[0] - o2[0];
		    }
		});
		
		// now we can go through and consider each position, filtering as needed

		int previousrightend = -1;
		
		int offeredForThisDocument = 0;
		
		for (int i=0, len=datas.size(); i<len; i++) {
			int[] data = datas.get(i);

			int keywordstart = data[0];
			int keywordend = data[1];
			
			String middle = StringUtils.substring(document, termsOfInterest.get(keywordstart).getStartOffset(), termsOfInterest.get(keywordend-1).getEndOffset());
			
			String[] parts = new String[keywordend-keywordstart];
			for (int k=0; k<keywordend-keywordstart; k++) {
				parts[k] = termsOfInterest.get(keywordstart+k).getText();
			}
			String analyzedMiddle = StringUtils.join(parts, " ");
			
			
			int leftstart = keywordstart - context;
			
			// check to see if we need to shift left based on previous kwic
			if (overlapStrategy==Kwic.OverlapStrategy.merge && leftstart<previousrightend) {
				leftstart = previousrightend < keywordstart ? previousrightend : keywordstart;
			}
			
			if (leftstart<0) {leftstart=0;}
			
			// make a simple check to see if we're overlapping with the previous kwic
			if (overlapStrategy==Kwic.OverlapStrategy.first && leftstart < previousrightend) {continue;}
			
			// make a simple check to see if we're overlapping with the previous kwic
			if (overlapStrategy==Kwic.OverlapStrategy.merge) {
				if (keywordstart < previousrightend) {continue;} // we have to drop one
				if (leftstart <= previousrightend) {leftstart=previousrightend+1;}
			}
			
			int rightend = keywordend-1 + context;
			if (rightend>lastToken) {rightend=lastToken;}
			
			
			if (overlapStrategy==Kwic.OverlapStrategy.merge) { // see if we need to collapse with next one
				if (i+1<len) { // make sure there's a next one
					int[] nextData = datas.get(i+1);
					if (nextData[0]-(context*2)<=rightend) {
						int span = nextData[1]-keywordstart; // this is the span between the end of this keyword and the start of the next
						int margin = (int) Math.floor(((context*2)-span)/2);
						if (margin>context) {margin=context;}
						if (leftstart<keywordstart-margin) {
							leftstart=keywordstart-margin;
							if (leftstart<=previousrightend) {leftstart = previousrightend+1;}
						}
						if (rightend<nextData[1]-1+margin) {
							rightend=nextData[1]-1+margin > lastToken ? lastToken : nextData[1]-1+margin;
						}
						i++; // make sure to increment to skip next one
					}
				}
			}
			
			
			String left = leftstart < keywordstart ? StringUtils.substring(document, termsOfInterest.get(leftstart).getStartOffset(), termsOfInterest.get(keywordstart).getStartOffset()) : "";
			
			String right = rightend > keywordend-1 ? StringUtils.substring(document, termsOfInterest.get(keywordend-1).getEndOffset(), termsOfInterest.get(rightend).getEndOffset()) : "";
			
			total++;
			
			queue.offer(new Kwic(corpusDocumentIndex, stripper.strip(queriesMap.get(keywordstart)), stripper.strip(analyzedMiddle), keywordstart, stripper.strip(left), stripper.strip(middle), stripper.strip(right)));
		
			offeredForThisDocument++;
			
			// we have enough for this document and we don't need an accurate total
			if (perDocLimit<limit && offeredForThisDocument>=perDocLimit && accurateTotalNotNeeded) {
				break;
			}
			
			previousrightend = rightend;			
		}
		

		return queue;

	}
	
	public List<Kwic> getContexts() {
		return contexts;
	}
	
	@Override
	public void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		Map<Integer, List<DocumentSpansData>> documentSpansDataMap = getDocumentSpansData(corpusMapper, queries);
		this.contexts = getKwics(corpusMapper, documentSpansDataMap);
	}

	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		runQueries(corpusMapper, stopwords, new String[0]); // doesn't make much sense without queries
	}

}

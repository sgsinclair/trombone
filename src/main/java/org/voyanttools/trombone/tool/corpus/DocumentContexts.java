package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.vectorhighlight.FieldTermStack.TermInfo;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.Kwic;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.FlexibleQueue;
import org.voyanttools.trombone.util.Stripper;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("documentContexts")
public class DocumentContexts extends AbstractContextTerms {

	private List<Kwic> contexts = new ArrayList<Kwic>();
	
	@XStreamOmitField
	private Kwic.Sort contextsSort;
	
	@XStreamOmitField
	private Comparator<Kwic> comparator;

	public DocumentContexts(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		contextsSort = Kwic.Sort.valueOfForgivingly(parameters.getParameterValue("sortBy", ""));
		comparator = Kwic.getComparator(contextsSort);
	}
	
	public int getVersion() {
		return super.getVersion()+1;
	}

	private List<Kwic> getKwics(CorpusMapper corpusMapper, Map<Integer, Collection<DocumentSpansData>> documentSpansDataMap) throws IOException {
		
		int[] totalTokens = corpusMapper.getCorpus().getLastTokenPositions(tokenType);
		FlexibleQueue<Kwic> queue = new FlexibleQueue(comparator, limit);
		for (Map.Entry<Integer, Collection<DocumentSpansData>> dsd : documentSpansDataMap.entrySet()) {
			int luceneDoc = dsd.getKey();
			int corpusDocIndex = corpusMapper.getDocumentPositionFromLuceneId(luceneDoc);
			int lastToken = totalTokens[corpusDocIndex];
			FlexibleQueue<Kwic> q = getKwics(corpusMapper, dsd.getKey(), corpusDocIndex, lastToken, dsd.getValue());
			for (Kwic k : q.getUnorderedList()) {
				if (k!=null){
					queue.offer(k);
				}
			}
		}
		
		return queue.getOrderedList();
	}
	
	
	private FlexibleQueue<Kwic> getKwics(CorpusMapper corpusMapper, int luceneDoc, int corpusDocumentIndex,
			int lastToken, Collection<DocumentSpansData> documentSpansData) throws IOException {

		Map<Integer, TermInfo> termsOfInterest = getTermsOfInterest(corpusMapper.getAtomicReader(), luceneDoc, lastToken, documentSpansData, false);
		
		Stripper stripper = new Stripper(parameters.getParameterValue("stripTags"));

		// build kwics
		FlexibleQueue<Kwic> queue = new FlexibleQueue<Kwic>(comparator, limit);
		String document = corpusMapper.getCorpus().getDocument(corpusDocumentIndex).getDocumentString();
		//String document = atomicReader.document(luceneDoc).get(tokenType.name());
		for (DocumentSpansData dsd : documentSpansData) {
			for (int[] data : dsd.spansData) {
				int keywordstart = data[0];
				int keywordend = data[1];
				
				String middle = StringUtils.substring(document, termsOfInterest.get(keywordstart).getStartOffset(), termsOfInterest.get(keywordend-1).getEndOffset());
				
				String[] parts = new String[keywordend-keywordstart];
				for (int i=0; i<keywordend-keywordstart; i++) {
					parts[i] = termsOfInterest.get(keywordstart+i).getText();
				}
				String analyzedMiddle = StringUtils.join(parts, " ");
				
				
				int leftstart = keywordstart - context;
				if (leftstart<0) {leftstart=0;}
				String left = StringUtils.substring(document, termsOfInterest.get(leftstart).getStartOffset(), termsOfInterest.get(keywordstart).getStartOffset());

				int rightend = keywordend + context;
				if (rightend>lastToken) {rightend=lastToken;}
				
				String right = StringUtils.substring(document, termsOfInterest.get(keywordend-1).getEndOffset()+1, termsOfInterest.get(rightend).getEndOffset());
				queue.offer(new Kwic(corpusDocumentIndex, stripper.strip(dsd.queryString), stripper.strip(analyzedMiddle), keywordstart, stripper.strip(left), stripper.strip(middle), stripper.strip(right)));
			}
		}
		
		return queue;
		
	}

	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		Map<Integer, Collection<DocumentSpansData>> documentSpansDataMap = getDocumentSpansData(corpusMapper, queries);
		this.contexts = getKwics(corpusMapper, documentSpansDataMap);
	}

	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		runQueries(corpusMapper, stopwords, new String[0]); // doesn't make much sense without queries
	}




}

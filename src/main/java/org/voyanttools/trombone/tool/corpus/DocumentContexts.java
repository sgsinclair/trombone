package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.vectorhighlight.FieldTermStack.TermInfo;
import org.voyanttools.trombone.lucene.StoredToLuceneDocumentsMapper;
import org.voyanttools.trombone.model.Corpus;
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

	public List<Kwic> getKwics(AtomicReader atomicReader, StoredToLuceneDocumentsMapper corpusMapper, Corpus corpus) throws IOException {
		
		Map<Integer, Collection<DocumentSpansData>> documentSpansDataMap = getDocumentSpansData(atomicReader, corpusMapper, queries);
		return getKwics(atomicReader, corpusMapper, corpus, documentSpansDataMap);

	}
	
	private List<Kwic> getKwics(AtomicReader atomicReader, StoredToLuceneDocumentsMapper corpusMapper, Corpus corpus, Map<Integer, Collection<DocumentSpansData>> documentSpansDataMap) throws IOException {
		
		int[] totalTokens = corpus.getLastTokenPositions(tokenType);
		FlexibleQueue<Kwic> queue = new FlexibleQueue(comparator, limit);
		for (Map.Entry<Integer, Collection<DocumentSpansData>> dsd : documentSpansDataMap.entrySet()) {
			int luceneDoc = dsd.getKey();
			int corpusDocIndex = corpusMapper.getDocumentPositionFromLuceneDocumentIndex(luceneDoc);
			int lastToken = totalTokens[corpusDocIndex];
			FlexibleQueue<Kwic> q = getKwics(atomicReader, dsd.getKey(), corpusDocIndex, lastToken, dsd.getValue());
			for (Kwic k : q.getUnorderedList()) {
				queue.offer(k);
			}
		}
		
		return queue.getOrderedList();
	}
	
	
	private FlexibleQueue<Kwic> getKwics(AtomicReader atomicReader, int luceneDoc, int corpusDocumentIndex,
			int lastToken, Collection<DocumentSpansData> documentSpansData) throws IOException {

		Map<Integer, TermInfo> termsOfInterest = getTermsOfInterest(atomicReader, luceneDoc, lastToken, documentSpansData, false);
		
		Stripper stripper = new Stripper(parameters.getParameterValue("stripTags"));

		// build kwics
		FlexibleQueue<Kwic> queue = new FlexibleQueue<Kwic>(comparator, limit);
		String document = atomicReader.document(luceneDoc).get(tokenType.name());
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
				String left = StringUtils.substring(document, termsOfInterest.get(leftstart).getStartOffset(), termsOfInterest.get(keywordstart).getStartOffset()-1);

				int rightend = keywordend + context;
				if (rightend>lastToken) {rightend=lastToken;}
				
				String right = StringUtils.substring(document, termsOfInterest.get(keywordend-1).getEndOffset()+1, termsOfInterest.get(rightend).getEndOffset());
				
				queue.offer(new Kwic(corpusDocumentIndex, stripper.strip(dsd.queryString), stripper.strip(analyzedMiddle), keywordstart, stripper.strip(left), stripper.strip(middle), stripper.strip(right)));
			}
		}
		
		return queue;
		
	}

	@Override
	protected void runQueries(Corpus corpus, String[] queries) throws IOException {
		this.queries = queries; // FIXME: this should be set by superclass

		AtomicReader reader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getDirectoryReader());
		StoredToLuceneDocumentsMapper corpusMapper = getStoredToLuceneDocumentsMapper(new IndexSearcher(reader), corpus);
		this.contexts = getKwics(reader, corpusMapper, corpus);
	}

	@Override
	protected void runAllTerms(Corpus corpus) throws IOException {
		AtomicReader reader = SlowCompositeReaderWrapper.wrap(storage.getLuceneManager().getDirectoryReader());
		StoredToLuceneDocumentsMapper corpusMapper = getStoredToLuceneDocumentsMapper(new IndexSearcher(reader), corpus);
		this.contexts = getKwics(reader, corpusMapper, corpus);
	}




}

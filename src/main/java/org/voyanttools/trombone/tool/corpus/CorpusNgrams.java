/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.CorpusNgram;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.DocumentNgram;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.FlexibleQueue;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
@XStreamAlias("corpusNgrams")
public class CorpusNgrams extends AbstractTerms {
	
	private List<CorpusNgram> ngrams = new ArrayList<CorpusNgram>();
	
	@XStreamOmitField
	private Comparator<CorpusNgram> comparator;

	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusNgrams(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		CorpusNgram.Sort sort = CorpusNgram.Sort.getForgivingly(parameters);
		comparator = CorpusNgram.getComparator(sort);
	}
	
	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		FlexibleParameters localParameters = parameters.clone();
		localParameters.setParameter("limit", Integer.MAX_VALUE); // we need all ngrams for documents in order to determine corpus collocates
		localParameters.setParameter("start", 0);
		DocumentNgrams documentNgrams = new DocumentNgrams(storage, localParameters);
		List<DocumentNgram> docNgrams = documentNgrams.getNgrams(corpusMapper, stopwords, queries);
		addFromDocumentNgrams(docNgrams, corpusMapper.getCorpus().size());
	}
	
	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		FlexibleParameters localParameters = parameters.clone();
		localParameters.setParameter("limit", Integer.MAX_VALUE); // we need all ngrams for documents in order to determine corpus collocates
		localParameters.setParameter("start", 0);
		DocumentNgrams documentNgrams = new DocumentNgrams(storage, localParameters);
		List<DocumentNgram> docNgrams = documentNgrams.getNgrams(corpusMapper, stopwords);
		addFromDocumentNgrams(docNgrams, corpusMapper.getCorpus().size());
	}
	
	private void addFromDocumentNgrams(List<DocumentNgram> docNgrams, int docs) {
		String currentTerm = "";
		List<DocumentNgram> currentList = new ArrayList<DocumentNgram>();
		FlexibleQueue<CorpusNgram> queue = new FlexibleQueue<CorpusNgram>(comparator, start+limit);
		for (DocumentNgram docNgram : docNgrams) {
			if (docNgram.getTerm().equals(currentTerm)==false) {
				if (currentList.isEmpty()==false) {
					queue.offer(getCorpusNgram(currentList, docs));
					total++;
					currentList.clear();
				}
				currentTerm = docNgram.getTerm();
			}
			currentList.add(docNgram);
		}
		if (currentList.isEmpty()==false) {
			queue.offer(getCorpusNgram(currentList, docs));
			total++;
		}
		this.ngrams.addAll(queue.getOrderedList(start));
	}

	private CorpusNgram getCorpusNgram(List<DocumentNgram> currentList, int docs) {
		int[] rawFreqs = new int[docs];
		for (DocumentNgram docNgram : currentList) {
			rawFreqs[docNgram.getCorpusDocumentIndex()]+=docNgram.getPositions().size();
		}
		DocumentNgram docNgram = currentList.get(0);
		return new CorpusNgram(docNgram.getTerm(), docNgram.getLength(), rawFreqs);
	}
}

/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusNgram;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.DocumentNgram;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.FlexibleQueue;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
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
@XStreamAlias("corpusNgrams")
@XStreamConverter(CorpusNgrams.CorpusNgramsConverter.class)
public class CorpusNgrams extends AbstractTerms implements ConsumptiveTool {
	
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
	public float getVersion() {
		return super.getVersion()+2;
	}

	
	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		DocumentNgrams documentNgrams = getDocumentNgrams(corpusMapper.getCorpus());
		List<DocumentNgram> docNgrams = documentNgrams.getNgrams(corpusMapper, stopwords, queries);
		addFromDocumentNgrams(docNgrams, corpusMapper.getCorpus().size());
	}
	
	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		DocumentNgrams documentNgrams = getDocumentNgrams(corpusMapper.getCorpus());
		List<DocumentNgram> docNgrams = documentNgrams.getNgrams(corpusMapper, stopwords);
		addFromDocumentNgrams(docNgrams, corpusMapper.getCorpus().size());
	}
	
	private DocumentNgrams getDocumentNgrams(Corpus corpus) throws IOException {
		FlexibleParameters localParameters = parameters.clone();
		localParameters.setParameter("limit", Integer.MAX_VALUE); // we need all ngrams for documents in order to determine corpus collocates
		localParameters.setParameter("start", 0);
		List<String> ids = this.getCorpusStoredDocumentIdsFromParameters(corpus);
		if (ids.size()<corpus.size()) {
			localParameters.setParameter("docId", ids.toArray(new String[0]));
		}
		return new DocumentNgrams(storage, localParameters);
	}
	
	private void addFromDocumentNgrams(List<DocumentNgram> docNgrams, int docs) {
		String currentTerm = "";
		List<DocumentNgram> currentList = new ArrayList<DocumentNgram>();
		FlexibleQueue<CorpusNgram> queue = new FlexibleQueue<CorpusNgram>(comparator, start+limit);
		
		// resort the docNgrams in term order so that we can merge same terms easier
		Collections.sort(docNgrams, DocumentNgram.getComparator(DocumentNgram.Sort.TERMASC));
		
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
	
	public static class CorpusNgramsConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return CorpusNgrams.class.isAssignableFrom(type);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			CorpusNgrams corpusNgrams = (CorpusNgrams) source;
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "total", Integer.class);
			writer.setValue(String.valueOf(corpusNgrams.total));
			writer.endNode();
			
			FlexibleParameters parameters = corpusNgrams.getParameters();
			boolean withDistributions = parameters.getParameterBooleanValue("withDistributions");
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "ngrams", Map.class);
	        for (CorpusNgram corpusNgram : corpusNgrams.ngrams) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "term", String.class); // not written in JSON
		        
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "term", String.class);
				writer.setValue(corpusNgram.getTerm());
				writer.endNode();

		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
				writer.setValue(String.valueOf(corpusNgram.getRawFreq()));
				writer.endNode();

		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "length", Integer.class);
				writer.setValue(String.valueOf(corpusNgram.getLength()));
				writer.endNode();

	        	if (withDistributions) {
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "distributions", List.class);
			        context.convertAnother(corpusNgram.getDistributions());
			        writer.endNode();
	        	}
	        	
	        	writer.endNode();
	        }
	        writer.endNode();

			
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			return null;
		}
		
	}
}

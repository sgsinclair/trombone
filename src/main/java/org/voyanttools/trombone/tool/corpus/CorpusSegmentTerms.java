/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.apache.commons.collections4.ListUtils;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.DocumentToken;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

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
@XStreamAlias("corpusSegmentTerms")
@XStreamConverter(CorpusSegmentTerms.CorpusSegmentTermsConverter.class)
public class CorpusSegmentTerms extends AbstractCorpusTool {

	/* package visibility primarily for testing */
	@XStreamOmitField
	List<DocumentToken[]> segmentMarkers = new ArrayList<DocumentToken[]>();

	/* package visibility primarily for testing */
	@XStreamOmitField
	List<Map.Entry<String, double[]>> sortedSegmentTerms = null;

	@XStreamOmitField
	private int total;
	
	@XStreamOmitField
	private TokenType tokenType;
	
	
	public CorpusSegmentTerms(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		tokenType = TokenType.getTokenTypeForgivingly(parameters.getParameterValue("tokenType", "lexical"));
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.corpus.AbstractCorpusTool#run(org.voyanttools.trombone.lucene.CorpusMapper)
	 */
	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		int segments = parameters.getParameterIntValue("segments", corpusMapper.getCorpus().size());
		int corpusTokensCount = corpusMapper.getCorpus().getTokensCount(TokenType.lexical);
		int[] documentTokensCounts = corpusMapper.getCorpus().getTokensCounts(TokenType.lexical);
		FlexibleParameters params = parameters.clone(); // should include stopwords as needed
		params.setParameter("noOthers", "true"); // only keep worda
		params.removeParameter("start"); // make sure to start at the beginning
		params.setParameter("limit", Integer.MAX_VALUE); // make sure to grab all tokens for this document
		List<DocumentToken> documentTokens;	
		List<Map<String, Double>> termsToFreqsMapList = new ArrayList<Map<String, Double>>();
		for (int i=0, len=documentTokensCounts.length; i<len; i++) {
			params.setParameter("docIndex", i); // limit to current document
			DocumentTokens tokens = new DocumentTokens(storage, params);
			tokens.run(corpusMapper);
			documentTokens = tokens.getDocumentTokens();
			int docUnits = (int) (((float) documentTokensCounts[i])*segments)/corpusTokensCount;
			if (docUnits<1) {docUnits=1;}
			int size = (int) ((float) documentTokens.size())/docUnits;
			List<List<DocumentToken>> tokensUnits = ListUtils.partition(documentTokens, size+1);
			for (List<DocumentToken> tokensUnit : tokensUnits) {
				System.out.println(documentTokens.size()+" "+tokensUnit.size()+" "+size);
				Map<String, AtomicInteger> unitFreqs = new HashMap<String, AtomicInteger>();
				for (DocumentToken documentToken : tokensUnit) {
					String term = documentToken.getTerm();
					if (tokenType==TokenType.lexical) {term = term.toLowerCase();}
					if (unitFreqs.containsKey(term)) {unitFreqs.get(term).incrementAndGet();}
					else {unitFreqs.put(term, new AtomicInteger(1));}
				}
				Map<String, Double> relativeMap = new HashMap<String, Double>();
				int tokensUnitSize = tokensUnit.size();
				for (Map.Entry<String, AtomicInteger> unitFreqsEntry : unitFreqs.entrySet()) {
					relativeMap.put(unitFreqsEntry.getKey(), (double) ((double) unitFreqsEntry.getValue().get()/tokensUnitSize));
				}
				termsToFreqsMapList.add(relativeMap);
				segmentMarkers.add(new DocumentToken[]{tokensUnit.get(0),tokensUnit.get(tokensUnit.size()-1)});
			}
			
		}
		
		// build a list of all terms
		Set<String> termsSet = new HashSet<String>();
		for (Map<String, Double> map : termsToFreqsMapList) {
			termsSet.addAll(map.keySet());
		}
		total = termsSet.size(); // make sure to set total before subsetting
		
		// build map of freqs
		Map<String, double[]> termFreqs = new HashMap<String, double[]>();
		for (String term : termsSet) {
			double[] freqs = new double[termsToFreqsMapList.size()];
			for (int i=0, len=termsToFreqsMapList.size(); i<len; i++) {
				Map<String, Double> freqsMap = termsToFreqsMapList.get(i);
				freqs[i] = freqsMap.containsKey(term) ?  freqsMap.get(term) : 0;
			}
			termFreqs.put(term, freqs);
		}
		sortedSegmentTerms = termFreqs.entrySet().stream()
			.sorted((m1, m2) -> Double.compare(Arrays.stream(m2.getValue()).sum(), Arrays.stream(m1.getValue()).sum()))
			.limit(parameters.getParameterIntValue("limit", Integer.MAX_VALUE))
			.collect(Collectors.toList());
		
	}
	
	public static class CorpusSegmentTermsConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return CorpusSegmentTerms.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			CorpusSegmentTerms corpusSegmentTerms = (CorpusSegmentTerms) source;
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "total", Integer.class);
			writer.setValue(String.valueOf(corpusSegmentTerms.total));
			writer.endNode();
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "segments", Map.class);
	        for (DocumentToken[] documentTokens : corpusSegmentTerms.segmentMarkers) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "segment", String.class); // not written in JSON
		        
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "start-position", Integer.class);
				writer.setValue(String.valueOf(documentTokens[0].getPosition()));
				writer.endNode();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "start-docIndex", Integer.class);
				writer.setValue(String.valueOf(documentTokens[0].getDocIndex()));
				writer.endNode();

		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "end-position", Integer.class);
				writer.setValue(String.valueOf(documentTokens[1].getPosition()));
				writer.endNode();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "end-docIndex", Integer.class);
				writer.setValue(String.valueOf(documentTokens[1].getDocIndex()));
				writer.endNode();

				writer.endNode();
	        }
	        writer.endNode();

	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "terms", Map.class);
			for (Map.Entry<String, double[]> corpusSegmentTerm : corpusSegmentTerms.sortedSegmentTerms) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "term", String.class); // not written in JSON
		        
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "term", String.class);
				writer.setValue(corpusSegmentTerm.getKey());
				writer.endNode();
				
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreqs", List.class);
		        context.convertAnother(corpusSegmentTerm.getValue());
		        writer.endNode();


				writer.endNode();
			}
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

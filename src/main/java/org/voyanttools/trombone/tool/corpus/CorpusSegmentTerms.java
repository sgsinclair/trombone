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

	@XStreamOmitField
	private List<DocumentToken[]> segmentMarkers = new ArrayList<DocumentToken[]>();
	@XStreamOmitField
	private int segments;
	@XStreamOmitField
	private List<Map.Entry<String, int[]>> sortedSegmentTerms = null;
	@XStreamOmitField
	private int total;
	@XStreamOmitField
	private TokenType tokenType;
	
	
	public CorpusSegmentTerms(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		segments = parameters.getParameterIntValue("segments", 10);
		tokenType = TokenType.getTokenTypeForgivingly(parameters.getParameterValue("tokenType", "lexical"));
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.corpus.AbstractCorpusTool#run(org.voyanttools.trombone.lucene.CorpusMapper)
	 */
	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		int tokensCount = corpusMapper.getCorpus().getTokensCount(TokenType.lexical);
		int[] tokensCounts = corpusMapper.getCorpus().getTokensCounts(TokenType.lexical);
		int tokensPerSegment = Math.round(tokensCount/segments);
		FlexibleParameters params = parameters.clone();
		params.setParameter("limit", tokensPerSegment);
		params.setParameter("noOthers", "true");
		List<DocumentToken> documentTokens;
		List<Map<String, AtomicInteger>> termsToFreqsMapList = new ArrayList<Map<String, AtomicInteger>>();
		int tokensConsumed = 0;
		int start = 0;
		for (int i=0; i<segments; i++) {
			Map<String, AtomicInteger> freqs = new HashMap<String, AtomicInteger>();
			int c = 0;
			for (int j=0, len=tokensCounts.length; j<len; j++) {
				if (tokensConsumed<c+tokensCounts[j]) {
					params.setParameter("skipToDocId", corpusMapper.getDocumentIdFromDocumentPosition(j));
					start = tokensConsumed-c;
					break;
				}
				c+=tokensCounts[j];
			}
			params.setParameter("start", start);
			DocumentTokens tokens = new DocumentTokens(storage, params);
			tokens.run(corpusMapper);
			documentTokens = tokens.getDocumentTokens();
			tokensConsumed+=documentTokens.size();
			for (DocumentToken documentToken : documentTokens) {
				String term = documentToken.getTerm();
				if (tokenType==TokenType.lexical) {
					term = term.toLowerCase();
				}
				if (freqs.containsKey(term)) {freqs.get(term).incrementAndGet();}
				else {freqs.put(term, new AtomicInteger(1));}
			}
			termsToFreqsMapList.add(freqs);
			segmentMarkers.add(new DocumentToken[]{documentTokens.get(0),documentTokens.get(documentTokens.size()-1)});
		}
		
		// build a list of all terms
		Set<String> termsSet = new HashSet<String>();
		for (Map<String, AtomicInteger> map : termsToFreqsMapList) {
			termsSet.addAll(map.keySet());
		}
		total = termsSet.size(); // make sure to set total before subsetting
		
		// build map of freqs
		Map<String, int[]> termFreqs = new HashMap<String, int[]>();
		for (String term : termsSet) {
			int[] freqs = new int[termsToFreqsMapList.size()];
			for (int i=0, len=termsToFreqsMapList.size(); i<len; i++) {
				Map<String, AtomicInteger> freqsMap = termsToFreqsMapList.get(i);
				freqs[i] = freqsMap.containsKey(term) ?  freqsMap.get(term).get() : 0;
			}
			termFreqs.put(term, freqs);
		}
		sortedSegmentTerms = termFreqs.entrySet().stream()
			.sorted((m1, m2) -> Integer.compare(Arrays.stream(m2.getValue()).sum(), Arrays.stream(m1.getValue()).sum()))
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
			for (Map.Entry<String, int[]> corpusSegmentTerm : corpusSegmentTerms.sortedSegmentTerms) {
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

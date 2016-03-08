package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.DocumentTerm;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.RawCAType;
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

@XStreamAlias("documentSimilarity")
@XStreamConverter(DocumentSimilarity.DocSimConverter.class)
public class DocumentSimilarity extends CA {

	@XStreamOmitField
	private List<String> ids = new ArrayList<String>();
	
	@XStreamOmitField
	private List<Integer> indexes = new ArrayList<Integer>();
	
	enum ComparisonType {
		RELATIVEFREQ, TFIDF
	}
	
	private ComparisonType comparisonType = ComparisonType.RELATIVEFREQ;
	
	public DocumentSimilarity(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		
		String compType = parameters.getParameterValue("comparisonType", "");
		if (compType.toUpperCase().equals("TFIDF")) {
			comparisonType = ComparisonType.TFIDF;
		} else {
			comparisonType = ComparisonType.RELATIVEFREQ;
		}
	}

	@Override
	protected void runAnalysis(CorpusMapper corpusMapper) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		
		ids = this.getCorpusStoredDocumentIdsFromParameters(corpus);
		for (String id : ids) {
			int index = corpus.getDocumentPosition(id);
			indexes.add(index);
		}
		
		this.maxOutputDataItemCount = ids.size();
		
		double[][] freqMatrix = buildFrequencyMatrix(corpusMapper, 0);
		doCA(freqMatrix);
		
		int dimensions = Math.min(ids.size(), this.dimensions);
        if (ids.size() == 3) dimensions = 2;
		
		int i = 0, j;
		double[] v;
		for (Integer docIndex : indexes) {
			IndexedDocument doc = corpus.getDocument(docIndex);
	    	
	    	v = new double[dimensions];
	    	for (j = 0; j < dimensions; j++) {
		    	v[j] = this.rowProjections[i][j+1];
	    	}
	    	
	    	this.caTypes.add(new RawCAType(doc.getMetadata().getTitle(), doc.getMetadata().getTokensCount(TokenType.lexical), 0.0, v, RawCAType.PART, corpus.getDocumentPosition(doc.getId()) ));
	    	i++;
	    }
	}

	@Override
	protected double[][] buildFrequencyMatrix(CorpusMapper corpusMapper, int type) throws IOException {
		List<CorpusTerm> corpusTerms = this.getCorpusTypes(corpusMapper);
		FlexibleParameters params = new FlexibleParameters();
		for (CorpusTerm ct : corpusTerms) {
			params.addParameter("query", ct.getTerm());
		}
		
		params.addParameter("docId", ids.toArray(new String[0]));
		params.addParameter("stopList", this.parameters.getParameterValue("stopList"));
		
		DocumentTerms docTerms = new DocumentTerms(storage, params);
		docTerms.run(corpusMapper);
				
		List<DocumentTerm> docTermsList = docTerms.getDocumentTerms();
		docTermsList.sort(DocumentTerm.getComparator(DocumentTerm.Sort.TERMASC));
		
		Map<String, List<Float>> tempMap = new HashMap<String, List<Float>>();
		
		for (DocumentTerm dt : docTermsList) {
			String term = dt.getTerm();
			tempMap.putIfAbsent(term, new ArrayList<Float>(Collections.nCopies(this.maxOutputDataItemCount, 0f)));
			int docIndexIndex = indexes.indexOf(dt.getDocIndex());
			if (docIndexIndex > -1) {
				if (comparisonType == ComparisonType.RELATIVEFREQ) {
					tempMap.get(term).set(docIndexIndex, dt.getRelativeFrequency());
				} else if (comparisonType == ComparisonType.TFIDF) {
					tempMap.get(term).set(docIndexIndex, dt.getTfIdf());
				}
			}
		}
		
		Set<Map.Entry<String, List<Float>>> entrySet = tempMap.entrySet();
		int numTerms = entrySet.size();
		double[][] freqMatrix = new double[this.maxOutputDataItemCount][numTerms];
		
		int termIndex = 0;
		for (Map.Entry<String, List<Float>> entry : entrySet) {
			List<Float> docValues = entry.getValue();
			
//			System.out.println(entry.getKey()+": "+StringUtils.join(docValues, ","));
			
			for (int docIndex = 0; docIndex < docValues.size(); docIndex++) {
				freqMatrix[docIndex][termIndex] = docValues.get(docIndex);
			}
			termIndex++;
		}
		
		return freqMatrix;
	}
	
	public static class DocSimConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return DocumentSimilarity.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer,
				MarshallingContext context) {
			
			
			DocumentSimilarity docSim = (DocumentSimilarity) source;
			
//			writer.startNode("total");
//			writer.setValue(String.valueOf(correspondenceAnalysis.total));
//			writer.endNode();
//	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "tokens", List.class);
//	        for (DocumentToken documentToken :  correspondenceAnalysis.getDocumentTokens()) {
//		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "token", String.class);
//		        
//		        context.convertAnother(documentToken);
//		        
//		        writer.endNode();
//	        }
//	        writer.endNode();
	        
			final List<RawCAType> caTypes = docSim.caTypes;
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "totalDocs", Integer.class);
			writer.setValue(String.valueOf(docSim.maxOutputDataItemCount));
			writer.endNode();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "dimensions", List.class);
	        context.convertAnother(docSim.dimensionPercentages);
	        writer.endNode();
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "tokens", Map.class);
			for (RawCAType caType : caTypes) {
				writer.startNode("token");
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "term", String.class);
				writer.setValue(caType.getType());
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "category", String.class);
				writer.setValue(String.valueOf(caType.getCategory()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "docIndex", Integer.class);
				writer.setValue(String.valueOf(caType.getDocIndex()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
				writer.setValue(String.valueOf(caType.getRawFreq()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "relativeFreq", Float.class);
				writer.setValue(String.valueOf(caType.getRelativeFreq()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "cluster", Integer.class);
				writer.setValue(String.valueOf(caType.getCluster()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "clusterCenter", Boolean.class);
				writer.setValue(String.valueOf(caType.isClusterCenter()));
				writer.endNode();
				
				double[] vectorDouble = caType.getVector();
				float[] vectorFloat = new float[vectorDouble.length];
				for (int i = 0, size = vectorDouble.length; i < size; i++) 
					vectorFloat[i] = (float) vectorDouble[i];
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "vector", vectorFloat.getClass());
	            context.convertAnother(vectorFloat);
	            writer.endNode();
				
	        	writer.endNode();
			}
			writer.endNode();
	        

		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader, com.thoughtworks.xstream.converters.UnmarshallingContext)
		 */
		@Override
		public Object unmarshal(HierarchicalStreamReader reader,
				UnmarshallingContext context) {
			return null;
		}

	}
	
	
}

package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.RawCAType;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@XStreamAlias("documentSimilarity")
@XStreamConverter(DocumentSimilarity.DocSimConverter.class)
public class DocumentSimilarity extends CA {
	
	public DocumentSimilarity(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	protected void runAnalysis(CorpusMapper corpusMapper) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		
		double[][] freqMatrix = buildFrequencyMatrix(corpusMapper, MatrixType.DOCUMENT, 3);
		doCA(freqMatrix);
		
		List<String> ids = this.getCorpusStoredDocumentIdsFromParameters(corpus);
		int dimensions = Math.min(ids.size(), this.dimensions);
        if (ids.size() == 3) dimensions = 2;
		
		int i = 0, j;
		double[] v;
		for (String	docId : ids) {
			IndexedDocument doc = corpus.getDocument(docId);
	    	
	    	v = new double[dimensions];
	    	for (j = 0; j < dimensions; j++) {
		    	v[j] = this.rowProjections[i][j+1];
	    	}
	    	
	    	this.caTypes.add(new RawCAType(doc.getMetadata().getTitle(), doc.getMetadata().getTokensCount(TokenType.lexical), 0.0, v, RawCAType.DOC, corpus.getDocumentPosition(docId) ));
	    	i++;
	    }
		
		if (clusters > 0) {
			AnalysisTool.clusterPoints(this.caTypes, clusters);
		}
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
	        
			final List<RawCAType> caTypes = docSim.caTypes;
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "totalDocs", Integer.class);
			writer.setValue(String.valueOf(caTypes.size()));
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

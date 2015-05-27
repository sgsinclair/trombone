package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.DocumentTerm;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.RawCAType;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.algorithms.pca.CorrespondenceAnalysis;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@XStreamAlias("correspondenceAnalysis")
@XStreamConverter(CA.CAConverter.class)
public class CA extends AnalysisTool {

	private List<RawCAType> caTypes;
	
	private double[][] rowProjections;
	private double[][] columnProjections;
	private double[] dimensionPercentages;
	
	private String target;
	private int clusters;
	private String docId;
	private int bins;
	private int dimensions;
	
	
	public CA(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);

		target = parameters.getParameterValue("target");
		clusters = parameters.getParameterIntValue("clusters");
		docId = parameters.getParameterValue("docId");
		bins = parameters.getParameterIntValue("bins", 10);
		dimensions = parameters.getParameterIntValue("dimensions", 2);
		
		this.caTypes = new ArrayList<RawCAType>();
	}
	
	private void doCA(double[][] freqMatrix) {
		CorrespondenceAnalysis ca = new CorrespondenceAnalysis(freqMatrix);
		ca.doAnalysis();
		this.rowProjections = ca.getRowProjections();
		this.columnProjections = ca.getColumnProjections();
		this.dimensionPercentages = ca.getDimensionPercentages();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void runAnalysis(CorpusMapper corpusMapper) throws IOException {
		int i, j;
		double[] v;
		
		Corpus corpus = corpusMapper.getCorpus();
		int numDocs = corpus.size();
		
		double[] targetVector = null;
		List<String> initialTypes = new ArrayList<String>();
//		List<String> initialTypes = new ArrayList<String>(Arrays.asList(this.properties.getParameterValues("type")));
//		if (target != null) this.properties.setParameter("type", "");
		
		if (numDocs > 2 && docId == null) { // FIXME CA needs at least 3 columns to function properly
			// CORPUS
			double[][] freqMatrix = this.buildFrequencyMatrix(corpusMapper, CORPUS);
			final List<CorpusTerm> corpusTerms = (List<CorpusTerm>) this.getTypesList();
			
			doCA(freqMatrix);
			
			int dimensions = Math.min(numDocs, this.dimensions);
            if (numDocs == 3) dimensions = 2; // make sure there's no ArrayOutOfBoundsException 
            
			for (i = 0; i < this.maxOutputDataItemCount; i++) {
		    	final CorpusTerm term = corpusTerms.get(i);
		    	
		    	v = new double[dimensions];
		    	for (j = 0; j < dimensions; j++) {
			    	v[j] = this.rowProjections[i][j+1];
		    	}
		    	
		    	if (term.getTerm().equals(target)) targetVector = v;
			    
		    	int rawFreq = term.getRawFreq();
		    	double relFreq = (double) rawFreq / corpus.getTokensCount(TokenType.lexical);
		    	
		    	this.caTypes.add(new RawCAType(term.getTerm(), rawFreq, relFreq, v, RawCAType.WORD));
		    }
			
			if (target != null) {
				this.doFilter(targetVector, initialTypes);
			}
			
			for (i = 0; i < numDocs; i++) {
		    	IndexedDocument doc = corpus.getDocument(i);
		    	
		    	v = new double[dimensions];
		    	for (j = 0; j < dimensions; j++) {
			    	v[j] = this.columnProjections[i][j+1];
		    	}
		    	
		    	if (doc.getMetadata().getTitle().equals(target)) targetVector = v;
			    
		    	this.caTypes.add(new RawCAType(doc.getMetadata().getTitle(), doc.getMetadata().getTokensCount(TokenType.lexical), 0.0, v, RawCAType.PART));
		    }
			
			if (clusters > 0) {
				AnalysisTool.clusterPoints(this.caTypes, clusters);
			}
			
		} else {
			// DOCUMENT
			double[][] freqMatrix = this.buildFrequencyMatrix(corpusMapper, DOCUMENT);
			final List<DocumentTerm> docTypes = (List<DocumentTerm>) this.getTypesList();
			
			doCA(freqMatrix);
			
			for (i = 0; i < this.maxOutputDataItemCount; i++) {
				DocumentTerm docTerm = docTypes.get(i);
		    	
		    	v = new double[dimensions];
		    	for (j = 0; j < dimensions; j++) {
			    	v[j] = this.rowProjections[i][j+1];
		    	}
		    	
		    	if (docTerm.getTerm().equals(target)) targetVector = v;
		    	
		    	this.caTypes.add(new RawCAType(docTerm.getTerm(), docTerm.getRawFrequency(), docTerm.getRelativeFrequency(), v, RawCAType.WORD));
		    }
			
			if (target != null) {
				this.doFilter(targetVector, initialTypes);
			}
			
			IndexedDocument doc;
			if (docId != null) {
				doc = corpus.getDocument(docId);
			} else {
				doc = corpus.getDocument(0);
			}
			
			for (i = 0; i < bins; i++) {
				String docTitle = doc.getMetadata().getTitle() + " " + i;
				int tokensPerBin = doc.getMetadata().getTokensCount(TokenType.lexical) / bins;
				
		    	v = new double[dimensions];
		    	for (j = 0; j < dimensions; j++) {
			    	v[j] = this.columnProjections[i][j+1];
		    	}
		    	
		    	if (doc.getMetadata().getTitle().equals(target)) targetVector = v;
			    
		    	this.caTypes.add(new RawCAType(docTitle, tokensPerBin, 0.0, v, RawCAType.PART));
		    }
			
			if (clusters > 0) {
				AnalysisTool.clusterPoints(this.caTypes, clusters);
			}
		}
	}
	
	private void doFilter(double[] targetVector, List<String> initialTypes) {
		double[][] minMax = AnalysisTool.getMinMax(this.rowProjections);
		double distance = AnalysisTool.getDistance(minMax[0], minMax[1]) / 50;
		AnalysisTool.filterTypesByTarget(this.caTypes, targetVector, distance, initialTypes);
		this.maxOutputDataItemCount = this.caTypes.size();
	}
	
	private static String getCategoryName(int category) {
		if (category == RawCAType.PART) return "part";
		else return "word";
	}
	
	public static class CAConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return CA.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer,
				MarshallingContext context) {
			
			
			CA ca = (CA) source;
			
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
	        
			final List<RawCAType> caTypes = ca.caTypes;
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "totalTerms", Integer.class);
			writer.setValue(String.valueOf(ca.maxOutputDataItemCount));
			writer.endNode();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "dimensions", List.class);
	        context.convertAnother(ca.dimensionPercentages);
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

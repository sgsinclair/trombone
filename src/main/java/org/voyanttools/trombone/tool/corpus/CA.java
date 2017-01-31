package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.RawCATerm;
import org.voyanttools.trombone.model.RawPCATerm;
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

	protected List<RawCATerm> caTerms;
	
	protected double[][] rowProjections;
	protected double[][] columnProjections;
	protected double[] dimensionPercentages;
	
	public CA(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);

		this.caTerms = new ArrayList<RawCATerm>();
	}
	
	protected void doCA(double[][] freqMatrix) {
		CorrespondenceAnalysis ca = new CorrespondenceAnalysis(freqMatrix);
		ca.doAnalysis();
		this.rowProjections = ca.getRowProjections();
		this.columnProjections = ca.getColumnProjections();
		this.dimensionPercentages = ca.getDimensionPercentages();
	}

	@Override
	protected void runAnalysis(CorpusMapper corpusMapper) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		List<String> ids = this.getCorpusStoredDocumentIdsFromParameters(corpus);
		int numDocs = ids.size();
		
		double[] targetVector = null;
		List<String> initialTerms = new ArrayList<String>(Arrays.asList(this.parameters.getParameterValues("term")));
			
		double[][] freqMatrix = buildFrequencyMatrix(corpusMapper, MatrixType.TERM, 3);
		doCA(freqMatrix);
        
		int dimensions;
		if (divisionType == DivisionType.DOCS) dimensions = Math.min(numDocs, this.dimensions);
		else dimensions = Math.min(bins, this.dimensions);
		if (numDocs == 3) dimensions = 2; // make sure there's no ArrayOutOfBoundsException
		
		int i, j;
		double[] v;
        List<RawPCATerm> terms = this.getAnalysisTerms();
        for (i = 0; i < terms.size(); i++) {
        	RawPCATerm term = terms.get(i);
	    	
	    	v = new double[dimensions];
	    	for (j = 0; j < dimensions; j++) {
		    	v[j] = this.rowProjections[i][j+1];
	    	}
	    	
	    	if (term.getTerm().equals(target)) targetVector = v;
	    	
	    	this.caTerms.add(new RawCATerm(term.getTerm(), term.getRawFrequency(), term.getRelativeFrequency(), v, RawCATerm.TERM, -1));
	    }

		if (divisionType == DivisionType.DOCS) {
			for (i = 0; i < numDocs; i++) {
		    	IndexedDocument doc = corpus.getDocument(i);
		    	
		    	v = new double[dimensions];
		    	for (j = 0; j < dimensions; j++) {
			    	v[j] = this.columnProjections[i][j+1];
		    	}
		    	
		    	if (doc.getMetadata().getTitle().equals(target)) targetVector = v;
			    
		    	this.caTerms.add(new RawCATerm(doc.getMetadata().getTitle(), doc.getMetadata().getTokensCount(TokenType.lexical), 0.0, v, RawCATerm.DOC, corpus.getDocumentPosition(doc.getId())));
		    }
			
		} else {
			int tokensPerBin = corpus.getTokensCount(TokenType.lexical) / bins;
			for (i = 0; i < bins; i++) {
				String binTitle = "Corpus " + i;
				
		    	v = new double[dimensions];
		    	for (j = 0; j < dimensions; j++) {
			    	v[j] = this.columnProjections[i][j+1];
		    	}
		    	
		    	if (binTitle.equals(target)) targetVector = v;
			    
		    	this.caTerms.add(new RawCATerm(binTitle, tokensPerBin, 0.0, v, RawCATerm.BIN, i));
		    }
		}
		
		if (target != null) {
			this.doFilter(targetVector, initialTerms);
		}
		
		if (clusters > 0) {
			AnalysisTool.clusterPoints(this.caTerms, clusters);
		}
	}
	
	private void doFilter(double[] targetVector, List<String> initialTerms) {
		double[][] minMax = AnalysisTool.getMinMax(this.rowProjections);
		double distance = AnalysisTool.getDistance(minMax[0], minMax[1]) / 50;
		AnalysisTool.filterTermsByTarget(this.caTerms, targetVector, distance, initialTerms);
//		this.maxOutputDataItemCount = this.caTypes.size();
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
	        
			final List<RawCATerm> caTerms = ca.caTerms;
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "totalTerms", Integer.class);
			writer.setValue(String.valueOf(caTerms.size()));
			writer.endNode();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "dimensions", List.class);
	        context.convertAnother(ca.dimensionPercentages);
	        writer.endNode();
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "tokens", Map.class);
			for (RawCATerm caTerm : caTerms) {
				writer.startNode("token");
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "term", String.class);
				writer.setValue(caTerm.getTerm());
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "category", String.class);
				writer.setValue(String.valueOf(caTerm.getCategory()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "docIndex", Integer.class);
				writer.setValue(String.valueOf(caTerm.getDocIndex()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
				writer.setValue(String.valueOf(caTerm.getRawFrequency()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "relativeFreq", Float.class);
				writer.setValue(String.valueOf(caTerm.getRelativeFrequency()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "cluster", Integer.class);
				writer.setValue(String.valueOf(caTerm.getCluster()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "clusterCenter", Boolean.class);
				writer.setValue(String.valueOf(caTerm.isClusterCenter()));
				writer.endNode();
				
				double[] vectorDouble = caTerm.getVector();
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

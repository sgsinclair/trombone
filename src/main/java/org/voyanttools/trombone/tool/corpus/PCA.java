package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.RawPCAType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.algorithms.pca.PrincipalComponentsAnalysis;
import org.voyanttools.trombone.tool.algorithms.pca.PrincipalComponentsAnalysis.PrincipleComponent;
import org.voyanttools.trombone.util.FlexibleParameters;

import Jama.Matrix;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@XStreamAlias("pcaAnalysis")
@XStreamConverter(PCA.PCAConverter.class)
public class PCA extends AnalysisTool {

	private List<RawPCAType> pcaTypes;
	private List<PrincipleComponent> principalComponents;
	
	public PCA(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		
		this.pcaTypes = new ArrayList<RawPCAType>();
		this.principalComponents = new ArrayList<PrincipleComponent>();
	}
	
	private double[][] doPCA(double[][] freqMatrix) {
		//Matrix originalData = new Matrix(freqMatrix);
	    //originalData.print(8, 4);
		
	    PrincipalComponentsAnalysis pca = new PrincipalComponentsAnalysis(freqMatrix);
	    
	    double[][] matrixAdjusted = PrincipalComponentsAnalysis.getMeanAdjusted(freqMatrix, pca.getMeans());
	    Matrix adjustedInput = new Matrix(matrixAdjusted);
	    
	    SortedSet<PrincipleComponent> principalComponents = pca.getPrincipleComponents();
	    Iterator<PrincipleComponent> it = principalComponents.iterator();
	    while (it.hasNext()) {
	    	PrincipleComponent pc = it.next();
	    	this.principalComponents.add(pc);
	    }
	    
	    Matrix features = PrincipalComponentsAnalysis.getDominantComponentsMatrix(pca.getDominantComponents(dimensions));

	    Matrix featuresXpose = features.transpose();

	    Matrix xformedData = featuresXpose.times(adjustedInput.transpose());

	    double[][] result = xformedData.transpose().getArray();
	    return result;
	}

	@Override
	protected void runAnalysis(CorpusMapper corpusMapper) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		
		int i;
		int numDocs = corpus.size();
		
		double[] targetVector = null;
		List<String> initialTerms = new ArrayList<String>(Arrays.asList(this.parameters.getParameterValues("term")));
//		if (target != null) this.properties.setParameter("type", "");
		
		double[][] freqMatrix = buildFrequencyMatrix(corpusMapper, MatrixType.TERM, 2);
		double[][] result = this.doPCA(freqMatrix);
		
		List<CorpusTerm> terms = this.getTermsList();
		for (i = 0; i < terms.size(); i++) {
			CorpusTerm term = terms.get(i);
			
			if (term.getTerm().equals(target)) targetVector = result[i];
			
			this.pcaTypes.add(new RawPCAType(term.getTerm(), term.getRawFrequency(), term.getRelativeFrequency(), result[i]));
		}
		
		if (clusters > 0) {
			AnalysisTool.clusterPoints(this.pcaTypes, clusters);
		}
		
		if (target != null) {
			double[][] minMax = AnalysisTool.getMinMax(result);
			double distance = AnalysisTool.getDistance(minMax[0], minMax[1]) / 50;
			AnalysisTool.filterTypesByTarget(this.pcaTypes, targetVector, distance, initialTerms);
//			this.maxOutputDataItemCount = this.pcaTypes.size();
		}
	}
	
	public static class PCAConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return PCA.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer,
				MarshallingContext context) {
			
			PCA pca = (PCA) source;
	        
			final List<RawPCAType> pcaTypes = pca.pcaTypes;
			
			final List<PrincipleComponent> principalComponents = pca.principalComponents;
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "totalTerms", Integer.class);
			writer.setValue(String.valueOf(pcaTypes.size()));
			writer.endNode();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "principalComponents", Map.Entry.class);
			for (PrincipleComponent pc : principalComponents) {
				writer.startNode("principalComponent");
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "eigenValue", Double.class);
				writer.setValue(String.valueOf(pc.eigenValue));
				writer.endNode();
				
				float[] vectorFloat = new float[pc.eigenVector.length];
				for (int i = 0, size = pc.eigenVector.length; i < size; i++)  {
					vectorFloat[i] = (float) pc.eigenVector[i];
				}
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "eigenVectors", vectorFloat.getClass());
				context.convertAnother(vectorFloat);
				writer.endNode();
				
				writer.endNode();
			}
			writer.endNode();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "tokens", Map.class);
			for (RawPCAType pcaType : pcaTypes) {
				writer.startNode("token");
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "term", String.class);
				writer.setValue(pcaType.getType());
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
				writer.setValue(String.valueOf(pcaType.getRawFreq()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "relativeFreq", Float.class);
				writer.setValue(String.valueOf(pcaType.getRelativeFreq()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "cluster", Integer.class);
				writer.setValue(String.valueOf(pcaType.getCluster()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "clusterCenter", Boolean.class);
				writer.setValue(String.valueOf(pcaType.isClusterCenter()));
				writer.endNode();
				
				double[] vectorDouble = pcaType.getVector();
				float[] vectorFloat = new float[vectorDouble.length];
				for (int i = 0, size = vectorDouble.length; i < size; i++) {
					vectorFloat[i] = (float) vectorDouble[i];
				}
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
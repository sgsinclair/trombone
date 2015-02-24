package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.DocumentTerm;
import org.voyanttools.trombone.model.RawPCAType;
import org.voyanttools.trombone.model.TokenType;
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
	
	private String target;
	private int clusters;
	private String docId;
	private int bins;
	private int dimensions;
	
	public PCA(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		
		target = parameters.getParameterValue("target");
		clusters = parameters.getParameterIntValue("clusters");
		docId = parameters.getParameterValue("docId");
		bins = parameters.getParameterIntValue("bins", 10);
		dimensions = parameters.getParameterIntValue("dimensions", 2);
		
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

	@SuppressWarnings("unchecked")
	@Override
	protected void runAnalysis(CorpusMapper corpusMapper) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		
		int i;
		int numDocs = corpus.size();
		
		double[] targetVector = null;
		List<String> initialTypes = new ArrayList<String>();
//		List<String> initialTypes = new ArrayList<String>(Arrays.asList(this.properties.getParameterValues("type")));
//		if (target != null) this.properties.setParameter("type", "");
		
		double[][] result = null;
		
		if (numDocs > 1 && docId == null) {
			double[][] freqMatrix = this.buildFrequencyMatrix(corpusMapper, CORPUS);
			List<CorpusTerm> corpusTerms = (List<CorpusTerm>) this.getTypesList();
			
			result = this.doPCA(freqMatrix);
			
		    for (i = 0; i < result.length; i++) {
		    	final CorpusTerm corpusTerm = corpusTerms.get(i);
		    	
		    	if (corpusTerm.getTerm().equals(target)) targetVector = result[i];
			    
		    	int rawFreq = corpusTerm.getRawFreq();
		    	double relFreq = (double) rawFreq / corpus.getTokensCount(TokenType.lexical);
		    	
		    	this.pcaTypes.add(new RawPCAType(corpusTerm.getTerm(), rawFreq, relFreq, result[i]));
		    }
		} else {
			double[][] freqMatrix = this.buildFrequencyMatrix(corpusMapper, DOCUMENT);
			final List<DocumentTerm> docTerms = (List<DocumentTerm>) this.getTypesList();
			
			result = this.doPCA(freqMatrix);
			
		    for (i = 0; i < result.length; i++) {
		    	DocumentTerm docTerm = docTerms.get(i);
		    	
		    	if (docTerm.getTerm().equals(target)) targetVector = result[i];
		    	
		    	this.pcaTypes.add(new RawPCAType(docTerm.getTerm(), docTerm.getRawFrequency(), docTerm.getRelativeFrequency(), result[i]));
		    }
		}
		
		if (clusters > 0) {
			AnalysisTool.clusterPoints(this.pcaTypes, clusters);
		}
		
		if (target != null) {
			double[][] minMax = AnalysisTool.getMinMax(result);
			double distance = AnalysisTool.getDistance(minMax[0], minMax[1]) / 50;
			AnalysisTool.filterTypesByTarget(this.pcaTypes, targetVector, distance, initialTypes);
			this.maxOutputDataItemCount = this.pcaTypes.size();
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
			
			writer.addAttribute("totalTypes", String.valueOf(pca.maxOutputDataItemCount));
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "principalComponents", Map.Entry.class);
			for (PrincipleComponent pc : principalComponents) {
				writer.startNode("principalComponent");
				writer.addAttribute("eigenValue", String.valueOf(pc.eigenValue));
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
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "tokens", Map.Entry.class);
			for (RawPCAType pcaType : pcaTypes) {
				writer.startNode("token");
				writer.addAttribute("term", pcaType.getType());
				writer.addAttribute("rawFreq", String.valueOf(pcaType.getRawFreq()));
				writer.addAttribute("relativeFreq", String.valueOf(pcaType.getRelativeFreq()));
				writer.addAttribute("cluster", String.valueOf(pcaType.getCluster()));
				writer.addAttribute("clusterCenter", String.valueOf(pcaType.isClusterCenter()));
				
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
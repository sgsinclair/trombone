package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.RawPCATerm;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.jujutsu.tsne.barneshut.BHTSne;
import com.jujutsu.tsne.barneshut.BarnesHutTSne;
import com.jujutsu.tsne.barneshut.TSneConfiguration;
import com.jujutsu.utils.TSneUtils;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@XStreamAlias("tsneAnalysis")
@XStreamConverter(TSNE.TSNEConverter.class)
public class TSNE extends AnalysisTool {

	private List<RawPCATerm> pcaTerms;
	
	/**
	 * The parameter theta specifies how coarse the Barnes-Hut approximation is:
	 * setting theta to 0 runs the original O(N2) t-SNE algorithm,
	 * whereas using higher values runs the O(N log N) with increasingly better constant.
	 * The value of theta should be between 0 and 1, and its default value is 0.5
	 */
	private final double defaultTheta = 0.5;
	
	/**
	 * The performance of t-SNE is fairly robust under different settings of the perplexity.
	 * The most appropriate value depends on the density of your data.
	 * Loosely speaking, one could say that a larger / denser dataset requires a larger perplexity.
	 * Typical values for the perplexity range between 5 and 50.
	 * 
	 * Perplexity is a measure for information that is defined as 2 to the power of the Shannon entropy.
	 * The perplexity of a fair die with k sides is equal to k.
	 * In t-SNE, the perplexity may be viewed as a knob that sets the number of effective nearest neighbors
	 */
	private final double defaultPerplexity = 25;
	
	/**
	 * Use PCA to reduce input dimensions, if necessary
	 */
	private boolean use_pca = false;
	
	public TSNE(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		this.pcaTerms = new ArrayList<RawPCATerm>();
	}

	private double[][] doTSNE(double[][] freqMatrix) {
		
		int iterations = Math.min(5000, Math.max(parameters.getParameterIntValue("iterations", 2000), 10));
		int initial_dims = 2;
		
		int rows = freqMatrix.length;
		
		float maxPerplexity = (rows-2)/3f; // more than this and tsne will fail
		float perplexity = parameters.getParameterFloatValue("perplexity");
		if (perplexity <= 0) {
			perplexity = maxPerplexity;
		} else if (perplexity*3 > rows-1) {
			perplexity = maxPerplexity;
		}
		
		float theta = Math.min(1f, Math.max(parameters.getParameterFloatValue("theta", (float)this.defaultTheta), 0f));
		
	    BarnesHutTSne tsne = new BHTSne();
	    
	    TSneConfiguration config = TSneUtils.buildConfig(freqMatrix, dimensions, initial_dims, perplexity, iterations, use_pca, theta, true, true);
	    
	    double[][] result = tsne.tsne(config);   
		
		return result;
	}
	
	@Override
	protected void runAnalysis(CorpusMapper corpusMapper) throws IOException {
		double[][] freqMatrix = buildFrequencyMatrix(corpusMapper, MatrixType.TERM, 2);
		if (freqMatrix.length >= 5) {
			double[][] result = this.doTSNE(freqMatrix);
			
			double[] targetVector = null;
			List<RawPCATerm> terms = this.getAnalysisTerms();
			for (int i = 0; i < terms.size(); i++) {
				RawPCATerm term = terms.get(i);
				this.pcaTerms.add(new RawPCATerm(term.getTerm(), term.getRawFrequency(), term.getRelativeFrequency(), result[i]));
			}
			
			if (clusters > 0) {
				AnalysisTool.clusterPoints(this.pcaTerms, clusters);
			}
		}
	}
	
	public static class TSNEConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return TSNE.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer,
				MarshallingContext context) {
			
			TSNE tsne = (TSNE) source;
	        
			final List<RawPCATerm> pcaTerms = tsne.pcaTerms;
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "totalTerms", Integer.class);
			writer.setValue(String.valueOf(pcaTerms.size()));
			writer.endNode();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "tokens", Map.class);
			for (RawPCATerm pcaTerm : pcaTerms) {
				writer.startNode("token");
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "term", String.class);
				writer.setValue(pcaTerm.getTerm());
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
				writer.setValue(String.valueOf(pcaTerm.getRawFrequency()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "relativeFreq", Float.class);
				writer.setValue(String.valueOf(pcaTerm.getRelativeFrequency()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "cluster", Integer.class);
				writer.setValue(String.valueOf(pcaTerm.getCluster()));
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "clusterCenter", Boolean.class);
				writer.setValue(String.valueOf(pcaTerm.isClusterCenter()));
				writer.endNode();
				
				double[] vectorDouble = pcaTerm.getVector();
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

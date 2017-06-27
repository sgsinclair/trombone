package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.List;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.RawCATerm;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.AnalysisUtils;
import org.voyanttools.trombone.tool.analysis.TSNEAnalysis;
import org.voyanttools.trombone.tool.corpus.CorpusAnalysisTool.MatrixType;
import org.voyanttools.trombone.util.FlexibleParameters;

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
public class TSNE extends CorpusAnalysisTool {
	
	public TSNE(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	private double[][] doTSNE(double[][] freqMatrix) {
		
		TSNEAnalysis tsner = new TSNEAnalysis(freqMatrix);
		tsner.setIterations(parameters.getParameterIntValue("iterations"));
		tsner.setPerplexity(parameters.getParameterFloatValue("perplexity"));
		tsner.setTheta(parameters.getParameterFloatValue("theta"));
		tsner.setDimensions(parameters.getParameterIntValue("dimensions", 2));
		
		tsner.runAnalysis();
		
		return tsner.getResult();
	}
	
	@Override
	public double[][] getInput() throws IOException {
		return buildFrequencyMatrix(MatrixType.TERM, 2);
	}
	
	@Override
	public double[][] runAnalysis(double[][] freqMatrix) throws IOException {
		if (freqMatrix.length >= 5) {
			double[][] result = doTSNE(freqMatrix);
			
			for (int i = 0; i < getAnalysisTerms().size(); i++) {
				RawCATerm term = getAnalysisTerms().get(i);
				term.setVector(result[i]);
				if (term.getTerm().equals(getTarget())) setTargetVector(result[i]);
			}
			
			return result;
		} else {
			return new double[][]{{}};
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
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			
			TSNE tsne = (TSNE) source;
	        
			final List<RawCATerm> caTerms = tsne.getAnalysisTerms();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "totalTerms", Integer.class);
			writer.setValue(String.valueOf(caTerms.size()));
			writer.endNode();
			
			AnalysisUtils.outputTerms(caTerms, false, writer, context);
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

package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.RawCATerm;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.AnalysisUtils;
import org.voyanttools.trombone.tool.analysis.PrincipalComponentsAnalysis;
import org.voyanttools.trombone.tool.analysis.PrincipalComponentsAnalysis.PrincipleComponent;
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

@XStreamAlias("pcaAnalysis")
@XStreamConverter(PCA.PCAConverter.class)
public class PCA extends CorpusAnalysisTool {
	
	private PrincipalComponentsAnalysis pca;
	
	public PCA(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}
	
	@Override
	public double[][] getInput() throws IOException {
		return buildFrequencyMatrix(MatrixType.TERM, 2);
	}

	@Override
	public double[][] runAnalysis(double[][] freqMatrix) throws IOException {
		pca = new PrincipalComponentsAnalysis(freqMatrix);
	    pca.runAnalysis();
		double[][] result =  pca.getResult(getDimensions());
		
		int i;
		for (i = 0; i < getAnalysisTerms().size(); i++) {
			RawCATerm term = getAnalysisTerms().get(i);
			term.setVector(result[i]);
			if (term.getTerm().equals(getTarget())) setTargetVector(result[i]);
		}
		
		return result;
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
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			
			PCA pca = (PCA) source;
	        
			final List<RawCATerm> pcaTerms = pca.getAnalysisTerms();
			
			final SortedSet<PrincipleComponent> principalComponents = pca.pca.getPrincipleComponents();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "totalTerms", Integer.class);
			writer.setValue(String.valueOf(pcaTerms.size()));
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
			
			AnalysisUtils.outputTerms(pcaTerms, false, writer, context);
	        

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
package org.voyanttools.trombone.tool.table;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.voyanttools.trombone.model.RawCATerm;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.AnalysisUtils;
import org.voyanttools.trombone.tool.analysis.PrincipalComponentsAnalysis;
import org.voyanttools.trombone.tool.analysis.PrincipalComponentsAnalysis.PrincipleComponent;
import org.voyanttools.trombone.tool.util.ToolSerializer;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@XStreamAlias("pcaAnalysis")
@XStreamConverter(PCA.PCAConverter.class)
public class PCA extends TableAnalysisTool {

	private PrincipalComponentsAnalysis pca;
	
	public PCA(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
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
			
			ToolSerializer.startNode(writer, "totalTerms", Integer.class);
			writer.setValue(String.valueOf(pcaTerms.size()));
			ToolSerializer.endNode(writer);
			
			ToolSerializer.startNode(writer, "principalComponents", Map.Entry.class);
			for (PrincipleComponent pc : principalComponents) {
				context.convertAnother(pc);
			}
			ToolSerializer.endNode(writer);
			
			AnalysisUtils.outputTerms(pcaTerms, false, writer, context);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader, com.thoughtworks.xstream.converters.UnmarshallingContext)
		 */
		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			return null;
		}
	}

}

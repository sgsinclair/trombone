package org.voyanttools.trombone.tool.table;

import java.io.IOException;
import java.util.List;

import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.RawCATerm;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.AnalysisUtils;
import org.voyanttools.trombone.tool.analysis.CorrespondenceAnalysis;
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
public class CA extends TableAnalysisTool {

	protected CorrespondenceAnalysis ca;
	
	private Keywords whitelist;
	
	public CA(Storage storage, FlexibleParameters parameters) throws IOException {
		super(storage, parameters);
		
		whitelist = new Keywords();
		whitelist.load(storage, parameters.getParameterValues("whitelist", new String[0]));
	}
	
	protected void doCA(double[][] freqMatrix) {
		ca = new CorrespondenceAnalysis(freqMatrix);
		ca.runAnalysis();
	}

	@Override
	protected double[][] runAnalysis() throws IOException {
		double[][] freqMatrix = AnalysisUtils.getMatrixFromParameters(parameters, analysisTerms);

		doCA(freqMatrix);
		
		double[][] rowProjections = ca.getRowProjections();
		int i, j;
		double[] v;
        for (i = 0; i < analysisTerms.size(); i++) {
        	RawCATerm term = analysisTerms.get(i);
        	if (whitelist.isEmpty()==false && whitelist.isKeyword(term.getTerm())==false) {continue;}
	    	
	    	v = new double[dimensions];
	    	for (j = 0; j < dimensions; j++) {
		    	v[j] = rowProjections[i][j+1];
	    	}
	    	
	    	if (term.getTerm().equals(target)) targetVector = v;
	    	
	    	term.setVector(v);
	    }
		
		return rowProjections;
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
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			
			CA ca = (CA) source;
	        
			final List<RawCATerm> caTerms = ca.analysisTerms;
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "totalTerms", Integer.class);
			writer.setValue(String.valueOf(caTerms.size()));
			writer.endNode();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "dimensions", List.class);
	        context.convertAnother(ca.ca.getDimensionPercentages());
	        writer.endNode();
			
	        AnalysisUtils.outputTerms(caTerms, true, writer, context);

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

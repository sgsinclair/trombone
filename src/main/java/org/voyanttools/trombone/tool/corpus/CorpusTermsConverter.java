/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.DocumentTerm;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author sgs
 *
 */
public class CorpusTermsConverter implements Converter {

	/* (non-Javadoc)
	 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
	 */
	@Override
	public boolean canConvert(Class type) {
		return CorpusTerms.class.isAssignableFrom(type);
	}

	/* (non-Javadoc)
	 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
	 */
	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		CorpusTerms corpusTerms = (CorpusTerms) source;
		
        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "total", Integer.class);
		writer.setValue(String.valueOf(corpusTerms.getTotal()));
		writer.endNode();
		
		FlexibleParameters parameters = corpusTerms.getParameters();
		String freqsMode = parameters.getParameterValue("withDistributions");
		
		boolean withRawDistributions = freqsMode != null && freqsMode.equals("raw");
		boolean withRelativeDistributions = freqsMode != null && !withRawDistributions && (freqsMode.equals("relative") || parameters.getParameterBooleanValue("withDistributions"));		
		int bins = parameters.getParameterIntValue("distributionBins");
		
		
        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "terms", Map.class);
		for (CorpusTerm corpusTerm : corpusTerms) {
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "terms", String.class); // not written in JSON
	        
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "term", String.class);
			writer.setValue(corpusTerm.getTerm());
			writer.endNode();
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
			writer.setValue(String.valueOf(corpusTerm.getRawFrequency()));
			writer.endNode();

			if (withRawDistributions) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "distributions", List.class);
		        context.convertAnother(corpusTerm.getRawDistributions(bins));
		        writer.endNode();
			}
			
			if (withRelativeDistributions) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "distributions", List.class);
		        context.convertAnother(corpusTerm.getRelativeDistributions(bins));
		        writer.endNode();
			}
			
			writer.endNode();
		}
		writer.endNode();
	}

	/* (non-Javadoc)
	 * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader, com.thoughtworks.xstream.converters.UnmarshallingContext)
	 */
	@Override
	public Object unmarshal(HierarchicalStreamReader arg0,
			UnmarshallingContext arg1) {
		// TODO Auto-generated method stub
		return null;
	}

}

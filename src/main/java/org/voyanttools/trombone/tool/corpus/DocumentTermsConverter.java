/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
public class DocumentTermsConverter implements Converter {


	/* (non-Javadoc)
	 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
	 */
	@Override
	public boolean canConvert(Class type) {
		return DocumentTerms.class.isAssignableFrom(type);
	}

	/* (non-Javadoc)
	 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
	 */
	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		DocumentTerms documentTerms = (DocumentTerms) source;
		
        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "total", Integer.class);
		writer.setValue(String.valueOf(documentTerms.getTotal()));
		writer.endNode();
		
		FlexibleParameters parameters = documentTerms.getParameters();
		int bins = parameters.getParameterIntValue("distributionBins", 10);
		boolean withRawDistributions = parameters.getParameterBooleanValue("withDistributions");
		
        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "terms", Map.class);
		for (DocumentTerm documentTerm : documentTerms) {
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "terms", String.class); // not written in JSON
	        
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "term", String.class);
			writer.setValue(documentTerm.getTerm());
			writer.endNode();
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
			writer.setValue(String.valueOf(documentTerm.getRawFrequency()));
			writer.endNode();

	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "relativeFreq", Float.class);
			writer.setValue(String.valueOf(documentTerm.getRelativeFrequency()));
			writer.endNode();
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "totalTermsCount", Integer.class);
			writer.setValue(String.valueOf(documentTerm.getTotalTermsCount()));
			writer.endNode();
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "docIndex", Integer.class);
			writer.setValue(String.valueOf(documentTerm.getDocIndex()));
			writer.endNode();
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "docId", String.class);
			writer.setValue(documentTerm.getDocId());
			writer.endNode();

			if (withRawDistributions) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "distributions", List.class);
		        context.convertAnother(documentTerm.getDistributions(bins));
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

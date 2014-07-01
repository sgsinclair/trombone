/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.DocumentTerm;
import org.voyanttools.trombone.model.DocumentToken;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * @author sgs
 *
 */
public class DocumentTokensConverter implements Converter {

	/* (non-Javadoc)
	 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
	 */
	@Override
	public boolean canConvert(Class type) {
		return DocumentTokens.class.isAssignableFrom(type);
	}

	/* (non-Javadoc)
	 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
	 */
	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		
		
		DocumentTokens documentTokens = (DocumentTokens) source;
		
        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "documents", List.class);
        for (Map.Entry<String, List<DocumentToken>> entry : documentTokens.getDocumentTokens().entrySet()) {
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "document", String.class); // not written in JSON

	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "id", String.class); // not written in JSON
	        writer.setValue(entry.getKey());
	        writer.endNode();
	        
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "tokens", List.class);
	        for (DocumentToken documentToken : entry.getValue()) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "token", String.class);
		        
		        context.convertAnother(documentToken);
		        
		        writer.endNode();
	        }
	        writer.endNode();
	        
	        
	        
	        writer.endNode();
        	
        }
        writer.endNode();
		
		/*
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
		writer.endNode();		// TODO Auto-generated method stub
		*/

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

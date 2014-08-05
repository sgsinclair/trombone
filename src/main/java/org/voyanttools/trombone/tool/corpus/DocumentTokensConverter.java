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
		
        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "tokens", List.class);
        for (DocumentToken documentToken :  documentTokens.getDocumentTokens()) {
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "token", String.class);
	        
	        context.convertAnother(documentToken);
	        
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

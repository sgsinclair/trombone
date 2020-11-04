package org.voyanttools.trombone.model;

import java.io.IOException;
import java.util.List;

import org.voyanttools.trombone.tool.util.ToolSerializer;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class DocumentConverter implements Converter {

	public DocumentConverter() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean canConvert(Class type) {
		return DocumentContainer.class.isAssignableFrom(type);
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		
		final DocumentContainer doc = (DocumentContainer) source;

		DocumentMetadata metadata; 
		try {
			metadata = doc.getMetadata();
		} catch (IOException e) {
			throw new RuntimeException("Unable to get document metadata during serialization: "+doc);
		}
		
		
		FlexibleParameters params = metadata.getFlexibleParameters();
		
		writer.startNode("id");
		writer.setValue(doc.getId());
		writer.endNode();
		for (String key : params.getKeys()) {
			String[] values = params.getParameterValues(key);
			if (values.length>0) {
				if (values.length==1) {
					writer.startNode(key);
					writer.setValue(values[0]);
					writer.endNode();
				}
				else {
					ToolSerializer.startNode(writer, key, List.class);
					context.convertAnother(values);
					ToolSerializer.endNode(writer);
				}
			}
		}

	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		return null;
	}

}

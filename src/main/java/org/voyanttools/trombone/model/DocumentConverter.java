package org.voyanttools.trombone.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
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
	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		
		final DocumentContainer doc = (DocumentContainer) source;

		FlexibleParameters params;
		try {
			params = doc.getMetadata().getFlexibleParameters();
		} catch (IOException e) {
			throw new RuntimeException("Unable to get document metadata during serialization: "+doc);
		}
		
		ExtendedHierarchicalStreamWriterHelper.startNode(writer, "id", String.class);
		writer.setValue(doc.getId());
		writer.endNode();
		for (String key : params.getKeys()) {
			String[] values = params.getParameterValues(key);
			if (values.length>0) {
				if (values.length==1) {
					ExtendedHierarchicalStreamWriterHelper.startNode(writer, key, String.class);
					writer.setValue(values[0]);
					writer.endNode();
				}
				else {
					ExtendedHierarchicalStreamWriterHelper.startNode(writer, key, List.class);
					context.convertAnother(values);
					writer.endNode();
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

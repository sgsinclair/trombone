package org.voyanttools.trombone.model;

import java.util.Properties;


import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class MetadataConverter implements Converter {

	public MetadataConverter() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean canConvert(Class type) {
		return DocumentMetadata.class == type || CorpusMetadata.class == type;
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		
		Properties properties = ((CorpusMetadata) source).getProperties();
		for (String key : properties.stringPropertyNames()) {
			System.err.println(key);
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, key, String.class);
	        context.convertAnother(properties.getProperty(key));
	        writer.endNode();
		}

	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		return null;
	}

}

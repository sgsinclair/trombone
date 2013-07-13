package org.voyanttools.trombone.tool.converter;

import java.util.List;

import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentContainer;
import org.voyanttools.trombone.tool.CorpusSummary;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class CorpusSummaryConverter implements Converter {

	@Override
	public boolean canConvert(Class type) {
		return CorpusSummary.class == type;
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		final Corpus corpus = ((CorpusSummary) source).getCorpus();
		ExtendedHierarchicalStreamWriterHelper.startNode(writer, "id", String.class);
		writer.setValue(corpus.getId());
		writer.endNode();
//		PropertiesWrapper metadata = corpus.getCorpusMetadata();
//		context.convertAnother(metadata);
		ExtendedHierarchicalStreamWriterHelper.startNode(writer, "documents", List.class);
        for (DocumentContainer doc : corpus) {
        	context.convertAnother(doc);
        }
        writer.endNode();
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		// TODO Auto-generated method stub
		return null;
	}

}

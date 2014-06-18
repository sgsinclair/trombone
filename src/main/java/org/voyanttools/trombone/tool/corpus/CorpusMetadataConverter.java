package org.voyanttools.trombone.tool.corpus;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusMetadata;
import org.voyanttools.trombone.model.DocumentContainer;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.tool.CorpusSummary;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class CorpusMetadataConverter implements Converter {

	@Override
	public boolean canConvert(Class type) {
		return CorpusMetadata.class == type;
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		final CorpusMetadata corpusMetadata = ((CorpusMetadata) source);
		
		
//		writer.startNode("id");
		ExtendedHierarchicalStreamWriterHelper.startNode(writer, "id", String.class);
		writer.setValue(corpusMetadata.getId());
		writer.endNode();
		
		ExtendedHierarchicalStreamWriterHelper.startNode(writer, "documentsCount", Integer.class);
		writer.setValue(String.valueOf(corpusMetadata.getDocumentIds().size()));
		writer.endNode();
		
		ExtendedHierarchicalStreamWriterHelper.startNode(writer, "createdTime", Integer.class);
		writer.setValue(String.valueOf(String.valueOf(corpusMetadata.getCreatedTime())));
		writer.endNode();
		
		ExtendedHierarchicalStreamWriterHelper.startNode(writer, "createdDate", String.class);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(corpusMetadata.getCreatedTime());
		writer.setValue(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(calendar.getTime()));
		writer.endNode();

		ExtendedHierarchicalStreamWriterHelper.startNode(writer, "lexicalTokensCount", Integer.class);
		writer.setValue(String.valueOf(corpusMetadata.getTokensCount(TokenType.lexical)));
		writer.endNode();

		ExtendedHierarchicalStreamWriterHelper.startNode(writer, "lexicalTypesCount", Integer.class);
		writer.setValue(String.valueOf(corpusMetadata.getTypesCount(TokenType.lexical)));
		writer.endNode();

		//		PropertiesWrapper metadata = corpus.getCorpusMetadata();
//		context.convertAnother(metadata);
//		ExtendedHierarchicalStreamWriterHelper.startNode(writer, "documents", List.class);
//        for (DocumentContainer doc : corpus) {
//        	context.convertAnother(doc);
//        }
//        writer.endNode();
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		// TODO Auto-generated method stub
		return null;
	}

}

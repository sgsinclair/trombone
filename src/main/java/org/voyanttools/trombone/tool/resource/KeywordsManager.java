/**
 * 
 */
package org.voyanttools.trombone.tool.resource;

import java.io.IOException;

import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.corpus.CorpusManager;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author sgs
 *
 */
@XStreamAlias("keywords")
@XStreamConverter(KeywordsManager.KeywordsManagerConverter.class)
public class KeywordsManager extends AbstractTool {

	Keywords keywords;

	public KeywordsManager(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		keywords = new Keywords();
	}

	@Override
	public void run() throws IOException {
		if (parameters.containsKey("stopList")) {
			if (parameters.containsKey("corpus") && !parameters.getParameterValue("corpus").trim().isEmpty()) {
				Corpus corpus = CorpusManager.getCorpus(storage, parameters);
				keywords = this.getStopwords(corpus);
			}
			else {
				keywords.load(storage, parameters.getParameterValues("stopList"));
			}
		}
	}
	
	@Override
	public int getVersion() {
		return super.getVersion()+2;
	}

	public static class KeywordsManagerConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return KeywordsManager.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			KeywordsManager keywordsManager = (KeywordsManager) source;
			context.convertAnother(keywordsManager.keywords);
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader,
				UnmarshallingContext context) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
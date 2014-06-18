/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.Properties;

import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * @author sgs
 *
 */
@XStreamAlias("corpus")
public class CorpusMetadata extends AbstractTool {

	@XStreamConverter(CorpusMetadataConverter.class)
	org.voyanttools.trombone.model.CorpusMetadata metadata = null;
	
	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusMetadata(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.utils.RunnableTool#run()
	 */
	@Override
	public void run() throws IOException {
		metadata = CorpusManager.getCorpus(storage, parameters).getCorpusMetadata();
	}

}

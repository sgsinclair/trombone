/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.io.Serializable;

import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;

/**
 * @author sgs
 *
 */
@XStreamAlias("corpus")
public class CorpusMetadata extends AbstractCorpusTool  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1953143503582284656L;
	
	@XStreamConverter(org.voyanttools.trombone.model.CorpusMetadata.CorpusMetadataConverter.class)
	org.voyanttools.trombone.model.CorpusMetadata metadata = null;
	
	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusMetadata(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	protected void run(Corpus corpus) throws IOException {
		metadata = corpus.getCorpusMetadata();
	}

}

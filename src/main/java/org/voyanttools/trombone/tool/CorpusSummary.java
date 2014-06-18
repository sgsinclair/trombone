package org.voyanttools.trombone.tool;

import java.io.IOException;

import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.corpus.CorpusMetadataConverter;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;

@XStreamConverter(CorpusMetadataConverter.class)
@XStreamAlias("corpusSummary")
public class CorpusSummary extends AbstractTool {
	
	private Corpus corpus;

	public CorpusSummary(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run() throws IOException {
		corpus = storage.getCorpusStorage().getCorpus(parameters.getParameterValue("corpus"));
		for (IndexedDocument doc : corpus) {
			doc.getMetadata(); // make sure metadata is loaded
		}
	}
	
	public Corpus getCorpus() {
		return corpus;
	}
}


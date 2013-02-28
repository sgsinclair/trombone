package org.voyanttools.trombone.tool;

import java.io.IOException;

import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

public class CorpusSummary extends AbstractTool {
	
	private Corpus corpus;

	public CorpusSummary(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() throws IOException {
		corpus = storage.getCorpusStorage().getCorpus(parameters.getParameterValue("corpus"));
		for (IndexedDocument doc : corpus) {
			doc.getMetadata(); // make sure metadata is loaded
		}
	}

}

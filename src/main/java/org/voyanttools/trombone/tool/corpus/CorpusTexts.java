package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.Stripper;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("texts")
public class CorpusTexts extends AbstractCorpusTool {

	private List<String> texts;
	
	public CorpusTexts(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		texts = new ArrayList<String>();
	}

	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		Stripper stripper = new Stripper(Stripper.TYPE.ALL);
		boolean noMarkup = parameters.getParameterBooleanValue("noMarkup");
		boolean compactSpace = parameters.getParameterBooleanValue("compactSpace");
		int limit = parameters.getParameterIntValue("limit", Integer.MAX_VALUE);
		String format = parameters.getParameterValue("format", "text").toLowerCase();
		for (String id : this.getCorpusStoredDocumentIdsFromParameters(corpusMapper.getCorpus())) {
			String string = corpus.getDocument(id).getDocumentString();
			if (format.equals("text") || noMarkup) {
				string = stripper.strip(string);
			}
			if (compactSpace) {
				string = string.replaceAll("  +", " ");
				string = string.replaceAll("(\r\n|\r|\n)\\s+", "\n");
				string = string.trim();
			}
			if (string.length()>=limit) {
				string = string.substring(0, limit);
			}
			texts.add(string);
		}

	}

}

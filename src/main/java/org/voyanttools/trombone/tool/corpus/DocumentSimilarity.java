package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.RawCATerm;
import org.voyanttools.trombone.model.RawCATerm.CategoryType;
import org.voyanttools.trombone.model.RawPCATerm;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.AnalysisUtils;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@XStreamAlias("documentSimilarity")
public class DocumentSimilarity extends CA {
	
	public DocumentSimilarity(Storage storage, FlexibleParameters parameters) throws IOException {
		super(storage, parameters);
	}

	@Override
	protected double[][] runAnalysis(CorpusMapper corpusMapper) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		
		double[][] freqMatrix = buildFrequencyMatrix(corpusMapper, MatrixType.DOCUMENT, 3);
		doCA(freqMatrix);
		
		analysisTerms.removeAll(analysisTerms); // don't need terms for docsim
		
		List<String> ids = this.getCorpusStoredDocumentIdsFromParameters(corpus);
		int dimensions = Math.min(ids.size(), this.dimensions);
        if (ids.size() == 3) dimensions = 2;
		
        double[][] rowProjections = ca.getRowProjections();
		int i = 0, j;
		for (String	docId : ids) {
			IndexedDocument doc = corpus.getDocument(docId);
	    	
	    	double[] v = new double[dimensions];
	    	for (j = 0; j < dimensions; j++) {
		    	v[j] = rowProjections[i][j+1];
	    	}
	    	
	    	analysisTerms.add(new RawCATerm(doc.getMetadata().getTitle(), doc.getMetadata().getTokensCount(TokenType.lexical), 0.0, v, CategoryType.DOCUMENT, corpus.getDocumentPosition(docId) ));
	    	i++;
	    }
		
		return rowProjections;
	}
}

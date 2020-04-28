package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.RawCATerm;
import org.voyanttools.trombone.model.RawCATerm.CategoryType;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.AnalysisUtils;
import org.voyanttools.trombone.tool.analysis.CorrespondenceAnalysis;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@XStreamAlias("correspondenceAnalysis")
@XStreamConverter(CA.CAConverter.class)
public class CA extends CorpusAnalysisTool {
	
	protected CorrespondenceAnalysis ca;
	
	private Keywords whitelist;
	
	public CA(Storage storage, FlexibleParameters parameters) throws IOException {
		super(storage, parameters);

		whitelist = new Keywords();
		whitelist.load(storage, parameters.getParameterValues("whitelist", new String[0]));
	}

	@Override
	public double[][] getInput() throws IOException {
		return buildFrequencyMatrix(MatrixType.TERM, 2);
	}
	
	@Override
	public double[][] runAnalysis(double[][] freqMatrix) throws IOException {
		ca = new CorrespondenceAnalysis(freqMatrix);
		ca.runAnalysis();
        
		Corpus corpus = corpusMapper.getCorpus();
		List<String> ids = this.getCorpusStoredDocumentIdsFromParameters(corpus);
		int numDocs = ids.size();
		
		int dimensions;
		if (divisionType == DivisionType.DOCS) {
			dimensions = Math.min(numDocs-1, getDimensions());
			if (dimensions < 1) {
				dimensions = 1; // should not ever be here
			}
		} else {
			dimensions = Math.min(getBins(), getDimensions());
		}
		
		double[][] rowProjections = ca.getRowProjections();
		int i, j;
		double[] v;
        for (i = 0; i < getAnalysisTerms().size(); i++) {
        	RawCATerm term = getAnalysisTerms().get(i);
        	if (whitelist.isEmpty()==false && whitelist.isKeyword(term.getTerm())==false) {continue;}
	    	
	    	v = new double[dimensions];
	    	for (j = 0; j < dimensions; j++) {
		    	v[j] = rowProjections[i][j+1];
	    	}
	    	
	    	if (term.getTerm().equals(getTarget())) setTargetVector(v);
	    	
	    	term.setVector(v);
	    }

        double[][] columnProjections = ca.getColumnProjections();
		if (divisionType == DivisionType.DOCS) {
			for (i = 0; i < numDocs; i++) {
		    	IndexedDocument doc = corpus.getDocument(i);
		    	
		    	v = new double[dimensions];
		    	for (j = 0; j < dimensions; j++) {
			    	v[j] = columnProjections[i][j+1];
		    	}
		    	
		    	if (doc.getMetadata().getTitle().equals(getTarget())) setTargetVector(v);
			    
		    	getAnalysisTerms().add(new RawCATerm(doc.getMetadata().getTitle(), doc.getMetadata().getTokensCount(TokenType.lexical), 0.0, v, CategoryType.DOCUMENT, corpus.getDocumentPosition(doc.getId())));
		    }
			
		} else {
			int tokensPerBin = corpus.getTokensCount(TokenType.lexical) / getBins();
			for (i = 0; i < getBins(); i++) {
				String binTitle = "Corpus " + i;
				
		    	v = new double[dimensions];
		    	for (j = 0; j < dimensions; j++) {
			    	v[j] = columnProjections[i][j+1];
		    	}
		    	
		    	if (binTitle.equals(getTarget())) setTargetVector(v);
			    
		    	getAnalysisTerms().add(new RawCATerm(binTitle, tokensPerBin, 0.0, v, CategoryType.BIN, i));
		    }
        }
		
		return rowProjections;
	}
	
	
	public static class CAConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return CA.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			
			CA ca = (CA) source;
	        
			final List<RawCATerm> caTerms = ca.getAnalysisTerms();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "totalTerms", Integer.class);
			writer.setValue(String.valueOf(caTerms.size()));
			writer.endNode();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "dimensions", List.class);
	        context.convertAnother(ca.ca.getDimensionPercentages());
	        writer.endNode();
			
	        AnalysisUtils.outputTerms(caTerms, true, writer, context);

		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader, com.thoughtworks.xstream.converters.UnmarshallingContext)
		 */
		@Override
		public Object unmarshal(HierarchicalStreamReader reader,
				UnmarshallingContext context) {
			return null;
		}

	}

}

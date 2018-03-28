package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.DocumentTerm;
import org.voyanttools.trombone.model.DocumentTermsCorrelation;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.FlexibleQueue;
import org.voyanttools.trombone.util.NumberUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@XStreamAlias("termCorrelations")
@XStreamConverter(DocumentTermCorrelations.DocumentTermCorrelationsConverter.class)
public class DocumentTermCorrelations extends AbstractTerms {

	@XStreamOmitField
	private int distributionBins;
	
	private List<DocumentTermsCorrelation> correlations;
	
	public DocumentTermCorrelations(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		distributionBins = parameters.getParameterIntValue("bins", 10);
		correlations = new ArrayList<DocumentTermsCorrelation>();
	}

	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords, String[] queries) throws IOException {
		List<String> ids = this.getCorpusStoredDocumentIdsFromParameters(corpusMapper.getCorpus());
		DocumentTerms documentTermsTool = getDocumentTermsTool(null);
		documentTermsTool.runQueries(corpusMapper, stopwords, queries);
		List<DocumentTerm> outerList = documentTermsTool.getDocumentTerms();
		populate(documentTermsTool.getDocumentTerms(), documentTermsTool.getDocumentTerms(), true);
		for (String id : ids) {
			documentTermsTool = getDocumentTermsTool(id);
			documentTermsTool.runAllTerms(corpusMapper, stopwords);
			populate(outerList, documentTermsTool.getDocumentTerms(), true);
		}
	}

	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords) throws IOException {
		List<String> ids = this.getCorpusStoredDocumentIdsFromParameters(corpusMapper.getCorpus());
		for (String id : ids) {
			DocumentTerms documentTermsTool = getDocumentTermsTool(id);
			documentTermsTool.runAllTerms(corpusMapper, stopwords);
			populate(documentTermsTool.getDocumentTerms(), documentTermsTool.getDocumentTerms(), true);
		}
	}
	
	private DocumentTerms getDocumentTermsTool(String id) {
		FlexibleParameters params = new FlexibleParameters();
		params.setParameter("withDistributions", "relative");
		params.setParameter("minRawFreq", 2);
		if (id!=null) {params.setParameter("docId", id);}
		return new DocumentTerms(storage, params);
	}
	
	private void populate(List<DocumentTerm> outerList, List<DocumentTerm> innerList, boolean half) {
		PearsonsCorrelation correlationer = new PearsonsCorrelation();
//		SpearmansCorrelation spearmansCorrelation = new SpearmansCorrelation();
		Comparator<DocumentTermsCorrelation> comparator = DocumentTermsCorrelation.getComparator(DocumentTermsCorrelation.Sort.getForgivingly(parameters));
		FlexibleQueue<DocumentTermsCorrelation> queue = new FlexibleQueue<DocumentTermsCorrelation>(comparator, start+limit);
		for (DocumentTerm outer : outerList) {
			for (DocumentTerm inner : innerList) {
				if (outer.getDocId().equals(inner.getDocId())==false) {continue;} // different docs, maybe from querying
				if (outer.equals(inner)) {continue;} // same word
				if (!half || (half && outer.getTerm().compareTo(inner.getTerm())>0)) {
					double correlation = correlationer.correlation(
							NumberUtils.getDoubles(outer.getRelativeDistributions(distributionBins)),
							NumberUtils.getDoubles(inner.getRelativeDistributions(distributionBins)));
					total++;
					queue.offer(new DocumentTermsCorrelation(inner, outer, (float) correlation));
				}
			}
		}
		correlations.addAll(queue.getOrderedList(start));
	}

	public static class DocumentTermCorrelationsConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return DocumentTermCorrelations.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			DocumentTermCorrelations documentTermCorrelations = (DocumentTermCorrelations) source;
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "total", Integer.class);
			writer.setValue(String.valueOf(documentTermCorrelations.getTotal()));
			writer.endNode();
			
			FlexibleParameters parameters = documentTermCorrelations.getParameters();
			boolean termsOnly = parameters.getParameterBooleanValue("termsOnly");
			
			
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "correlations", Map.class);
			for (DocumentTermsCorrelation documentTermCorrelation : documentTermCorrelations.correlations) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "correlation", String.class); // not written in JSON
		        
		        int i = 0;
		        for (DocumentTerm documentTerm : documentTermCorrelation.getDocumentTerms()) {
			        ExtendedHierarchicalStreamWriterHelper.startNode(writer, i++==0 ? "source" : "target", String.class);
			        if (termsOnly) {
						writer.setValue(documentTerm.getTerm());
			        } else {
			        	
				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "term", String.class);
						writer.setValue(documentTerm.getTerm());
						writer.endNode();
						
				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
						writer.setValue(String.valueOf(documentTerm.getRawFrequency()));
						writer.endNode();

				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "relativeFreq", Float.class);
						writer.setValue(String.valueOf(documentTerm.getRelativeFrequency()));
						writer.endNode();
						
				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "zscore", Float.class);
						writer.setValue(String.valueOf(documentTerm.getZscore()));
						writer.endNode();
						
				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "zscoreRatio", Float.class);
						writer.setValue(String.valueOf(documentTerm.getZscoreRatio()));
						writer.endNode();
						
				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "tfidf", Float.class);
						writer.setValue(String.valueOf(documentTerm.getTfIdf()));
						writer.endNode();
						
				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "totalTermsCount", Integer.class);
						writer.setValue(String.valueOf(documentTerm.getTotalTermsCount()));
						writer.endNode();
						
				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "docIndex", Integer.class);
						writer.setValue(String.valueOf(documentTerm.getDocIndex()));
						writer.endNode();
						
				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "docId", String.class);
						writer.setValue(documentTerm.getDocId());
						writer.endNode();

				        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "distributions", List.class);
				        float[] distributions = documentTerm.getRelativeDistributions(documentTermCorrelations.distributionBins).clone();
				        // clone to avoid empty on subsequent instances 
				        context.convertAnother(distributions.clone());
				        writer.endNode();
			        }
					writer.endNode();
		        }
		        
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "correlation", Integer.class);
				writer.setValue(String.valueOf(documentTermCorrelation.getCorrelation()));
				writer.endNode();
				
				writer.endNode();
			}
			writer.endNode();
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader, com.thoughtworks.xstream.converters.UnmarshallingContext)
		 */
		@Override
		public Object unmarshal(HierarchicalStreamReader arg0,
				UnmarshallingContext arg1) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	public Object getDocumentTerms() {
		// TODO Auto-generated method stub
		return null;
	}
}

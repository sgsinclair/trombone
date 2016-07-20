/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.search.DocumentFilter;
import org.voyanttools.trombone.lucene.search.FieldPrefixAwareSimpleQueryParser;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author sgs
 *
 */
@XStreamAlias("corpusFacets")
@XStreamConverter(CorpusFacets.CorpusFacetsConverter.class)
public class CorpusFacets extends AbstractTerms {
	
	private List<LabelAndValueAndDim> facetResults = new ArrayList<LabelAndValueAndDim>();

	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusFacets(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		// TODO Auto-generated constructor stub
	}
	
	public int getVersion() {
		return super.getVersion()+2;
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.corpus.AbstractTerms#runQueries(org.voyanttools.trombone.lucene.CorpusMapper, org.voyanttools.trombone.model.Keywords, java.lang.String[])
	 */
	@Override
	protected void runQueries(CorpusMapper corpusMapper, Keywords stopwords,
			String[] queries) throws IOException {
		
		String defaultPrefix = parameters.getParameterValue("facet", "");
		String defaultNonFacetedPrefix = defaultPrefix.replace("facet.", "");
		FieldPrefixAwareSimpleQueryParser parser = new FieldPrefixAwareSimpleQueryParser(corpusMapper.getLeafReader(), storage.getLuceneManager().getAnalyzer(), defaultPrefix);
		FieldPrefixAwareSimpleQueryParser nonFacetedParser = new FieldPrefixAwareSimpleQueryParser(corpusMapper.getLeafReader(), storage.getLuceneManager().getAnalyzer(), defaultNonFacetedPrefix);
		
		SortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(corpusMapper.getLeafReader());
		
		String[] queryStrings = getQueries(queries);
		
		for (String queryString : queryStrings)  {
			Query query = parser.parse(queryString);
		    String dim;
		    Query baseQuery;
			if (query instanceof PrefixQuery && ((PrefixQuery) query).getPrefix().text().isEmpty()) {
				dim = ((PrefixQuery) query).getField();
				baseQuery = new MatchAllDocsQuery();
			}
			else {
				// reparse to use unfaceted data
				baseQuery = nonFacetedParser.parse(queryString);
				if (baseQuery instanceof PrefixQuery) {
					dim = "facet."+((PrefixQuery) baseQuery).getField();
				}
				if (baseQuery instanceof TermQuery) {
					dim = "facet."+((TermQuery) baseQuery).getTerm().field();
				}
				else {
					dim = defaultPrefix;
				}
			}
		    FacetsCollector fc = new FacetsCollector();
		    FacetsCollector.search(corpusMapper.getSearcher(), baseQuery, corpusMapper.getSearcher().getIndexReader().maxDoc(), fc);
		    Facets facets = new SortedSetDocValuesFacetCounts(state, fc);
		    FacetResult result = facets.getTopChildren(1000000, dim);
		    if (result!=null){
		    	// if we have an additional query, we need to check if multiple values are present in case some don't match
		    	if (!(baseQuery instanceof MatchAllDocsQuery) && result.labelValues.length>1) {
		    		if (baseQuery instanceof BooleanQuery) {
		    			result = checkQuery(baseQuery, result);
		    		}
		    		if (result.labelValues.length>1 || queryStrings.length>1) {
		    			addResult(new FacetResult(result.dim.replace("facet.", ""), result.path, result.value, new LabelAndValue[]{new LabelAndValue(queryString, result.labelValues.length)}, result.childCount));
		    		}
		    	} 
//		    	else {
		    		addResult(result);
//		    	}
//				facetResults.add(result);
		    }
		}
		Collections.sort(facetResults);
	}
	
	private void addResult(FacetResult result) {
		for (LabelAndValue labelAndValue : result.labelValues) {
			facetResults.add(new LabelAndValueAndDim(labelAndValue, result.dim));
		}
	}

	private FacetResult checkQuery(Query query, FacetResult result) {
		if (query instanceof BooleanQuery) {
			for (BooleanClause clause : ((BooleanQuery) query).clauses()) {
				if (clause.getOccur()==Occur.MUST || clause.getOccur()==Occur.SHOULD) {
					Query q = clause.getQuery();
					if (q instanceof PrefixQuery || q instanceof TermQuery) {
						Pattern pattern = null;
						if (q instanceof PrefixQuery) {
							pattern = Pattern.compile("\\b"+((PrefixQuery) q).getPrefix().text(), Pattern.CASE_INSENSITIVE); 
						}
						else if (q instanceof TermQuery) {
							pattern = Pattern.compile("\\b"+((TermQuery) q).getTerm().text()+"\\b", Pattern.CASE_INSENSITIVE); 
						}
						if (pattern!=null) {
							List<LabelAndValue> labelAndValueKeepers = new ArrayList<LabelAndValue>();
							for (LabelAndValue labelAndValue : result.labelValues) {
								if (pattern.matcher(labelAndValue.label).find()) {
									labelAndValueKeepers.add(labelAndValue);
								}
							}
							if (result.labelValues.length>labelAndValueKeepers.size()) {
								result = new FacetResult(result.dim, result.path, result.value, labelAndValueKeepers.toArray(new LabelAndValue[0]), result.childCount);
							}
						}
					}
					else if (q instanceof PhraseQuery) {
						// we'll cheet and make this a boolean (so no slop)
						BooleanQuery.Builder builder = new BooleanQuery.Builder();
						for (Term term : ((PhraseQuery) q).getTerms()) {
							builder.add(new TermQuery(term), Occur.MUST);
						}
						result = checkQuery(builder.build(), result);
					}
					else if (q instanceof BooleanQuery) {
						result = checkQuery(q, result);
					}
				}
			}
		}
		return result;
	}
	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.corpus.AbstractTerms#runAllTerms(org.voyanttools.trombone.lucene.CorpusMapper, org.voyanttools.trombone.model.Keywords)
	 */
	@Override
	protected void runAllTerms(CorpusMapper corpusMapper, Keywords stopwords)
			throws IOException {
		if (parameters.containsKey("facet")) {
			String[] queries = parameters.getParameterValues("facet");
			for (int i=0; i<queries.length; i++) {
				queries[i] = queries[i]+":*";
			}
			runQueries(corpusMapper, stopwords, queries);
		}
		else {
			throw new RuntimeException("This tool requires either a query or a facet");
		}
	}
	
	private class LabelAndValueAndDim implements Comparable<LabelAndValueAndDim> {
		private LabelAndValue labelAndValue;
		private String dim;
		private LabelAndValueAndDim(LabelAndValue labelAndValue, String dim) {
			this.labelAndValue = labelAndValue;
			this.dim = dim;
		}

		@Override
		public int compareTo(LabelAndValueAndDim o) {
			return labelAndValue.value==o.labelAndValue.value ? labelAndValue.label.compareTo(o.labelAndValue.label) : Integer.compare(o.labelAndValue.value.intValue(), labelAndValue.value.intValue());
		}
	}
	
	public static class CorpusFacetsConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return CorpusFacets.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			CorpusFacets corpusFacets = (CorpusFacets) source;
			
			
			int total = 0;
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "facets", Map.class);
			for (LabelAndValueAndDim facetResult : corpusFacets.facetResults) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "labels", String.class); // not written in JSON
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "facet", String.class);
				writer.setValue(facetResult.dim);
				writer.endNode();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "label", String.class);
				writer.setValue(facetResult.labelAndValue.label);
				writer.endNode();
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "inDocumentsCount", Integer.class);
				writer.setValue(String.valueOf(facetResult.labelAndValue.value));
				writer.endNode();
				writer.endNode();
				total++;
			}
			writer.endNode();
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "total", Integer.class);
			writer.setValue(String.valueOf(total));
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

	
}

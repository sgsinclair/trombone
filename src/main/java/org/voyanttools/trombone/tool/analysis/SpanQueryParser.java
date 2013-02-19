package org.voyanttools.trombone.tool.analysis;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.voyanttools.trombone.model.TokenType;

public class SpanQueryParser {
	
	private final static Pattern QUERY_SEPARATOR = Pattern.compile(";");	
	private final static Pattern TERM_SEPARATOR = Pattern.compile(",");
	private final static String QUOTE = "\"";
	private final static String EMPTY = "";
	private final static String WILDCARD_ASTERISK = "*";
	private final static String WILDCARD_QUESTION = "?";
	private final static Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
	private final static Pattern SLOP_PATTERN = Pattern.compile("~(\\d+)$");
	private Analyzer analyzer;
	
	
	public SpanQueryParser(Analyzer analyzer) {
		this.analyzer = analyzer;
	}
	
	
	public Map<String, SpanQuery> getSpanQueries(IndexReader reader, String[] queries, TokenType tokenType, boolean collapse) throws IOException {
		Map<String, SpanQuery> spanQueries = new HashMap<String, SpanQuery>();
		// separate queries are always treated as individual (not to be collapsed)
		for (String query : queries) {
			// queries can also be separated by the query separator (semi-colon): one,two;three,four
			for (String q : QUERY_SEPARATOR.split(query.replace(QUOTE, EMPTY).trim())) {
				spanQueries.putAll(getSpanQueries(reader, q.trim(), tokenType, collapse));
			}
		}
		return spanQueries;
	}
	
	private Map<String, SpanQuery> getSpanQueries(IndexReader reader, String query, TokenType tokenType, boolean collapse) throws IOException {
		Map<String, SpanQuery> spanQueries = new HashMap<String, SpanQuery>();
		for (String termQuery : TERM_SEPARATOR.split(query)) {
			
			termQuery = termQuery.trim();
			
			// determine if we have a single query or a phrase (with whitespace and optional quotes)
			String[] parts = WHITESPACE_PATTERN.split(termQuery);
			
			// we have a regular term (can be a wildcard, but it's not a phrase)
			if (parts.length==1) {
				spanQueries.putAll(getSingleTermSpanQueries(reader, termQuery, tokenType, collapse));
			}
			
			// we have a phrase, let's create a SpanNear
			else {

				// determine if our phrase has a trailing slop: ~\d+
				int slop = 0;
				Matcher slopMatcher = SLOP_PATTERN.matcher(termQuery);
				if (slopMatcher.find()) {
					slop = Integer.parseInt(slopMatcher.group(1));
					// now remove the slop pattern before continuing
					parts = WHITESPACE_PATTERN.split(termQuery.substring(0, termQuery.length()-slopMatcher.group(1).length()-1));
				}

				List<SpanQuery> nearSpanQueries = new ArrayList<SpanQuery>();
				for (String part : parts) {
					nearSpanQueries.addAll(getSingleTermSpanQueries(reader, part, tokenType, true).values());
				}
				spanQueries.put(termQuery, new SpanNearQuery(nearSpanQueries.toArray(new SpanQuery[0]), slop, slop==0));
			}
		}
		
		// we need to build a SpanOr Query if we have multiple items and we're collapsing
		if (collapse && spanQueries.size()>1) {
			SpanOrQuery spanOrQuery = new SpanOrQuery();
			for (SpanQuery sq : spanQueries.values()) {
				spanOrQuery.addClause(sq);
			}
			spanQueries.clear();
			spanQueries.put(query, spanOrQuery);
		}
		return spanQueries;
	}
	
	private Map<String, SpanQuery> getSingleTermSpanQueries(IndexReader reader, String termQuery, TokenType tokenType, boolean collapse) throws IOException {
		Map<String, SpanQuery> spanQueries = new HashMap<String, SpanQuery>();
		if (termQuery.contains(WILDCARD_ASTERISK) || termQuery.contains(WILDCARD_QUESTION)) { // contains a wildcard
			WildcardQuery wildcard = new WildcardQuery(new Term(tokenType.name(), termQuery));
			SpanQuery query = (SpanQuery) new SpanMultiTermQueryWrapper<WildcardQuery>(wildcard).rewrite(reader);
			if (collapse) { // treat all wildcard variants as a single term
				spanQueries.put(termQuery, query);
			}
			else { // separate each wildcard term into its own query
				Set<Term> terms = new HashSet<Term>();
				query.extractTerms(terms);
				for (Term term : terms) {
					// we don't need to analyze term here since it's already from the index
					spanQueries.put(term.text(), new SpanTermQuery(term));
				}
			}
		}
		else { // regular term (we hope)
			Term term = getAnalyzedTerm(tokenType, termQuery); // analyze it first
			spanQueries.put(term.text(), new SpanTermQuery(term));
		}
		return spanQueries;
	}
	
	private Term getAnalyzedTerm(TokenType tokenType, String term) throws IOException {
		TokenStream tokenStream = analyzer.tokenStream(tokenType.name(), new StringReader(term));
		CharTermAttribute termAtt = tokenStream.addAttribute(CharTermAttribute.class);
		StringBuilder sb = new StringBuilder();
		while (tokenStream.incrementToken()) {
			sb.append(termAtt.toString());
		}
		return new Term(tokenType.name(), sb.toString());
	}

}

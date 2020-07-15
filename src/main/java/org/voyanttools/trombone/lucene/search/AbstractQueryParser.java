/**
 * 
 */
package org.voyanttools.trombone.lucene.search;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Weight;
import org.voyanttools.trombone.model.TokenType;

/**
 * @author sgs
 *
 */
public abstract class AbstractQueryParser {
	protected final static Pattern QUERY_SEPARATOR = Pattern.compile(";");	
	protected final static Pattern TERM_SEPARATOR = Pattern.compile("[,\\|]");
	protected final static String QUOTE = "\"";
	protected final static String EMPTY = "";
	protected final static String WILDCARD_ASTERISK = "*";
	protected final static String WILDCARD_QUESTION = "?";
	protected final static Pattern REGEX_PATTERN = Pattern.compile("(\\[.+?\\])");
	protected final static Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
	protected final static Pattern SLOP_PATTERN = Pattern.compile("~(\\d+)$");
	protected final static String FIELD_SEPARATOR = ":";
	protected final static String OPERATOR_AND = "+";
	protected IndexSearcher indexSearcher;
	protected IndexReader indexReader;
	protected Analyzer analyzer;

	/**
	 * 
	 */
	public AbstractQueryParser(IndexReader indexReader, Analyzer analyzer) {
		this.indexReader = indexReader;
		this.indexSearcher = new IndexSearcher(indexReader); // TODO: this is probably inefficient
		this.analyzer = analyzer;
	}

	protected Term getAnalyzedTerm(TokenType tokenType, String termString) throws IOException {
		Term term = getTerm(termString, tokenType); // first ensure that we've stripped any prefixes
		TokenStream tokenStream = analyzer.tokenStream(term.field(), new StringReader(term.text()));
		tokenStream.reset();
		CharTermAttribute termAtt = tokenStream.addAttribute(CharTermAttribute.class);
		StringBuilder sb = new StringBuilder();
		while (tokenStream.incrementToken()) {
			sb.append(termAtt.toString());
		}
		tokenStream.end();
		tokenStream.close();
		return new Term(term.field(), sb.toString());
	}
	
	public Map<String, Query> getQueriesMap(String[] queries, TokenType tokenType, boolean collapse) throws IOException {
		Map<String, Query> queriesMap = new HashMap<String, Query>();
		// separate queries are always treated as individual (not to be collapsed)
		for (String query : queries) {
			// queries can also be separated by the query separator (semi-colon): one,two;three,four
			for (String q : QUERY_SEPARATOR.split(query.replace(QUOTE, EMPTY).trim())) {
				String qt = q.trim();
				Map<String, Query> qs = getQueries(qt, tokenType, collapse);				
				queriesMap.putAll(qs);
			}
		}
		return queriesMap;
		
	}

	private Map<String, Query> getQueries(String query, TokenType tokenType, boolean collapse) throws IOException {
		Map<String, Query> queriesMap = new HashMap<String, Query>();

		for (String termQuery : TERM_SEPARATOR.split(query)) {
			
			termQuery = termQuery.trim();
			
			// determine if we have a single query or a phrase (with whitespace and optional quotes)
			String[] parts = WHITESPACE_PATTERN.split(termQuery);
			
			// we have a regular term (can be a wildcard, but it's not a phrase)
			if (parts.length==1) {
				queriesMap.putAll(getSingleTermQueries(termQuery, tokenType, collapse));
			}
			
			// we have a phrase, let's create a Near
			else {

				// determine if our phrase has a trailing slop: ~\d+
				int slop = 0;
				Matcher slopMatcher = SLOP_PATTERN.matcher(termQuery);
				if (slopMatcher.find()) {
					slop = Integer.parseInt(slopMatcher.group(1));
					// now remove the slop pattern before continuing
					parts = WHITESPACE_PATTERN.split(termQuery.substring(0, termQuery.length()-slopMatcher.group(1).length()-1));
				}

				List<Query> nearQueries = new ArrayList<Query>();
				for (String part : parts) {
					Collection<Query> qs = getSingleTermQueries(part, tokenType, true).values();
					nearQueries.addAll(qs);

				}
				queriesMap.put(termQuery, getNearQuery(nearQueries.toArray(new Query[0]), slop, slop==0));
			}
		}
		
		// we need to build a SpanOr Query if we have multiple items and we're collapsing
		if (collapse && queriesMap.size()>1) {			
			Query q = getBooleanQuery(queriesMap);
			queriesMap.clear();
			queriesMap.put(query, q);
		}
		return queriesMap;
	}

	private Map<String, Query> getSingleTermQueries(String termQuery, TokenType tokenType, boolean collapse) throws IOException {
		Map<String, Query> queriesMap = new HashMap<String, Query>();
		boolean isRegex = REGEX_PATTERN.matcher(termQuery).find();
		if (termQuery.contains(WILDCARD_ASTERISK) || termQuery.contains(WILDCARD_QUESTION) || isRegex) {
			Term term = getTerm(termQuery, tokenType);
			Query query = isRegex ? getRegexQuery(term) : getWildCardQuery(term);
			if (collapse) { // treat all wildcard variants as a single term
				queriesMap.put(termQuery, query);
			}
			else { // separate each wildcard term into its own query
				Set<Term> terms = new HashSet<Term>();
				Weight weight = query.createWeight(indexSearcher, ScoreMode.COMPLETE_NO_SCORES, 1f);
				weight.extractTerms(terms);
				for (Term t : terms) {
					// we don't need to analyze term here since it's already from the index
					queriesMap.put(t.text(), getTermQuery(t));
				}
			}
		}
		else { // regular term (we hope)
			Term term = getAnalyzedTerm(tokenType, termQuery); // analyze it first
			queriesMap.put(termQuery, getTermQuery(term));
		}
		return queriesMap;
	}
	
	/**
	 * This is to ensure that we're dealing with field prefixes
	 * @param term this is the original term string
	 * @param tokenType this is the default to use if no field prefix is present
	 * @return a new {@link Term}
	 */
	protected Term getTerm(String term, TokenType tokenType) {
		// strip operators
		if (term.startsWith(OPERATOR_AND)) {term=term.substring(1);}
		String field = tokenType.name(); // default
		if (term.contains(FIELD_SEPARATOR)) {
			int pos = term.indexOf(FIELD_SEPARATOR);
			field = term.substring(0, pos);
			term = term.substring(pos+1);
		}
		return new Term(field, term);
	}
	
	protected abstract Query getBooleanQuery(Map<String, Query> queriesMap) throws IOException;

	protected abstract Query getNearQuery(Query[] queries, int slop, boolean inOrder);

	protected abstract Query getWildCardQuery(Term term) throws IOException;
	
	protected abstract Query getRegexQuery(Term term) throws IOException;
	
	protected abstract Query getTermQuery(Term term);
}

package org.voyanttools.trombone.model;

import java.text.Normalizer;
import java.util.Comparator;

import org.voyanttools.trombone.util.FlexibleParameters;


public class CorpusCollocate implements Comparable<CorpusCollocate> {
	
	private String keyword;
	
	private int keywordRawFreq;
	
	private String contextTerm;
	
	private int contextTermRawFreq;
	
	private transient String normalizedContextTerm = null;

	private transient String normalizedKeyword = null;

	public enum Sort {
		RAWFREQASC, RAWFREQDESC, TERMASC, TERMDESC;

		public static Sort getForgivingly(FlexibleParameters parameters) {
			String sort = parameters.getParameterValue("sort", "").toUpperCase();
			String sortPrefix = "RAWFREQ"; // default
			if (sort.startsWith("TERM")) {sortPrefix = "TERM";}
			String dir = parameters.getParameterValue("dir", "").toUpperCase();
			String dirSuffix = "DESC";
			if (dir.endsWith("ASC")) {dirSuffix="ASC";}
			return valueOf(sortPrefix+dirSuffix);
		}
	}
	
	public CorpusCollocate(String keyword, int keywordRawFrequency, String contextTerm, int contextTermRawFrequency) {
		this.keyword = keyword;
		this.keywordRawFreq = keywordRawFrequency;
		this.contextTerm = contextTerm;
		this.contextTermRawFreq = contextTermRawFrequency;
	}
	
	private String getNormalizedContextTerm() {
		if (normalizedContextTerm==null) {normalizedContextTerm = Normalizer.normalize(contextTerm, Normalizer.Form.NFD);}
		return normalizedContextTerm;
	}
	
	public String getContextTerm() {
		return contextTerm;
	}
	
	public int getContextTermRawFrequency() {
		return contextTermRawFreq;
	}
	
	private String getNormalizedKeyword() {
		if (normalizedKeyword==null) {normalizedKeyword = Normalizer.normalize(keyword, Normalizer.Form.NFD);}
		return normalizedKeyword;
	}

	public static Comparator<CorpusCollocate> getComparator(Sort sort) {
		switch (sort) {
		case RAWFREQASC:
			return ContextTermRawFrequencyAscendingComparator;
		case TERMASC:
			return ContextTermAscendingComparator;
		case TERMDESC:
			return ContextTermDescendingComparator;
		default: // rawFrequencyDesc
			return ContextTermRawFrequencyDescendingComparator;
		}
	}
	
	private static Comparator<CorpusCollocate> ContextTermRawFrequencyAscendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			
			return corpusCollocate1.contextTermRawFreq==corpusCollocate2.contextTermRawFreq ?
					corpusCollocate1.compareTo(corpusCollocate2) :
					Integer.compare(corpusCollocate1.contextTermRawFreq, corpusCollocate2.contextTermRawFreq);
		}
	};

	private static Comparator<CorpusCollocate> ContextTermRawFrequencyDescendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			return corpusCollocate1.compareTo(corpusCollocate2); // this is essentially the default comparasion algorithm, reversed
		}
	};
	
	private static Comparator<CorpusCollocate> ContextTermAscendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			return corpusCollocate1.contextTerm.equals(corpusCollocate2.contextTerm) ? 
					corpusCollocate1.compareTo(corpusCollocate2) : 
					corpusCollocate1.getNormalizedContextTerm().compareTo(corpusCollocate2.getNormalizedContextTerm());
		}
	};
	
	private static Comparator<CorpusCollocate> ContextTermDescendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			return corpusCollocate1.contextTerm.equals(corpusCollocate2.contextTerm) ? 
					corpusCollocate1.compareTo(corpusCollocate2) : 
					corpusCollocate2.getNormalizedContextTerm().compareTo(corpusCollocate1.getNormalizedContextTerm());
		}
	};

	@Override
	public int compareTo(CorpusCollocate o) {
		
		// first by context term desending frequency
		if (contextTermRawFreq!=o.contextTermRawFreq) {
			return Integer.compare(o.contextTermRawFreq, contextTermRawFreq);
		}
		
		// next by ascending context term
		if (!contextTerm.equals(o.contextTerm)) {
			return getNormalizedContextTerm().compareTo(o.getNormalizedContextTerm());
		}
		
		// next by descending keyword raw frequency
		if (keywordRawFreq!=o.keywordRawFreq) {
			return Integer.compare(o.keywordRawFreq, keywordRawFreq);
		}
		
		// next by ascending keyword term
		if (!keyword.equals(o.keyword)) {
			return getNormalizedKeyword().compareTo(o.getNormalizedKeyword());
		}
		
		// next by hashcode
		return Integer.compare(hashCode(), o.hashCode());
	}
	
	public String toString() {
		return new StringBuilder("{corpus collocate - context: ").append(contextTerm).append(" (").append(contextTermRawFreq).append("); keyword: ").append(keyword).append(" (").append(keywordRawFreq).append(")}").toString();
	}



}

package org.voyanttools.trombone.model;

import java.text.Normalizer;
import java.util.Comparator;

import org.voyanttools.trombone.util.FlexibleParameters;


public class CorpusCollocate implements Comparable<CorpusCollocate> {
	
	private String term;
	
	private int rawFreq;
	
	private String contextTerm;
	
	private int contextTermRawFreq;
	
	private transient String normalizedContextTerm = null;

	private transient String normalizedKeyword = null;

	public enum Sort {
		RAWFREQASC, RAWFREQDESC, TERMASC, TERMDESC, CONTEXTTERMASC, CONTEXTTERMDESC, CONTEXTTERMRAWFREQASC, CONTEXTTERMRAWFREQDESC;

		public static Sort getForgivingly(FlexibleParameters parameters) {
			String sort = parameters.getParameterValue("sort", "").toUpperCase();
			String sortPrefix = "RAWFREQ"; // default
			if (sort.startsWith("TERM")) {sortPrefix = "TERM";}
			if (sort.startsWith("CONTEXTTERM")) {sortPrefix = "CONTEXTTERM";}
			if (sort.startsWith("CONTEXTTERMRAWFREQ")) {sortPrefix = "CONTEXTTERMRAWFREQ";}
			String dir = parameters.getParameterValue("dir", "").toUpperCase();
			String dirSuffix = "DESC";
			if (dir.endsWith("ASC")) {dirSuffix="ASC";}
			return valueOf(sortPrefix+dirSuffix);
		}
	}
	
	public CorpusCollocate(String keyword, int keywordRawFrequency, String contextTerm, int contextTermRawFrequency) {
		this.term = keyword;
		this.rawFreq = keywordRawFrequency;
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
		if (normalizedKeyword==null) {normalizedKeyword = Normalizer.normalize(term, Normalizer.Form.NFD);}
		return normalizedKeyword;
	}

	public static Comparator<CorpusCollocate> getComparator(Sort sort) {
		switch (sort) {
		case RAWFREQASC:
			return RawFrequencyAscendingComparator;
		case TERMASC:
			return TermAscendingComparator;
		case TERMDESC:
			return TermDescendingComparator;
		case CONTEXTTERMASC:
			return ContextTermAscendingComparator;
		case CONTEXTTERMDESC:
			return ContextTermDescendingComparator;
		case CONTEXTTERMRAWFREQASC:
			return ContextTermRawFrequencyAscendingComparator;
		case CONTEXTTERMRAWFREQDESC:
			return ContextTermRawFrequencyDescendingComparator;
		default: // rawFrequencyDesc
			return RawFrequencyDescendingComparator;
		}
	}

	private static Comparator<CorpusCollocate> RawFrequencyAscendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			return corpusCollocate1.compareTo(corpusCollocate2);
		}
	};

	private static Comparator<CorpusCollocate> RawFrequencyDescendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			return corpusCollocate2.compareTo(corpusCollocate1);
		}
	};

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
	
	private static Comparator<CorpusCollocate> TermAscendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			return corpusCollocate1.term.equals(corpusCollocate2.term) ? 
					corpusCollocate1.compareTo(corpusCollocate2) : 
					corpusCollocate1.getNormalizedKeyword().compareTo(corpusCollocate2.getNormalizedKeyword());
		}
	};
	
	private static Comparator<CorpusCollocate> TermDescendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			return corpusCollocate1.term.equals(corpusCollocate2.term) ? 
					corpusCollocate1.compareTo(corpusCollocate2) : 
					corpusCollocate2.getNormalizedKeyword().compareTo(corpusCollocate1.getNormalizedKeyword());
		}
	};

	@Override
	public int compareTo(CorpusCollocate o) {
		
		// first by keyword raw frequency
		if (rawFreq!=o.rawFreq) {
			return Integer.compare(o.rawFreq, rawFreq);
		}

		// next by ascending keyword term
		if (!term.equals(o.term)) {
			return o.getNormalizedKeyword().compareTo(getNormalizedKeyword());
		}


		// next by context term desending frequency
		if (contextTermRawFreq!=o.contextTermRawFreq) {
			return Integer.compare(contextTermRawFreq, o.contextTermRawFreq);
		}

		// next by ascending context term
		if (!contextTerm.equals(o.contextTerm)) {
			return o.getNormalizedContextTerm().compareTo(getNormalizedContextTerm());
		}

		// next by hashcode
		return Integer.compare(hashCode(), o.hashCode());
	}
	
	public String toString() {
		return new StringBuilder("{corpus collocate - context: ").append(contextTerm).append(" (").append(contextTermRawFreq).append("); keyword: ").append(term).append(" (").append(rawFreq).append(")}").toString();
	}



}

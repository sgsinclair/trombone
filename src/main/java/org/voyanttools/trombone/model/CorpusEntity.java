/**
 * 
 */
package org.voyanttools.trombone.model;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.Comparator;

import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
public class CorpusEntity implements Serializable, Cloneable {

	public enum Sort {
		RAWFREQASC, RAWFREQDESC, TERMASC, TERMDESC, INDOCUMENTSCOUNTASC, INDOCUMENTSCOUNTDESC;

		public static Sort getForgivingly(FlexibleParameters parameters) {
			String sort = parameters.getParameterValue("sort", "").toUpperCase();
			String sortPrefix = "RAWFREQ"; // default
			if (sort.startsWith("TERM")) {sortPrefix = "TERM";}
			else if (sort.startsWith("INDOCUMENTSCOUNT")) {sortPrefix = "INDOCUMENTSCOUNT";}
			String dir = parameters.getParameterValue("dir", "").toUpperCase();
			String dirSuffix = "DESC";
			if (dir.endsWith("ASC")) {dirSuffix="ASC";}
			return valueOf(sortPrefix+dirSuffix);
		}

	}
	
	private String term;
	private EntityType type;
	private int rawFreq;
	private int[] rawFreqs;
	private int inDocumentsCount;
	@XStreamOmitField
	private transient String normalizedString = null;

	/**
	 * @param inDocumentsCount 
	 * 
	 */
	public CorpusEntity(String term, EntityType type, int rawFreq, int inDocumentsCount, int[] rawFreqs) {
		this.term = term;
		this.type = type;
		this.rawFreq = rawFreq;
		this.inDocumentsCount = inDocumentsCount;
		this.rawFreqs = rawFreqs;
	}

	public String getTerm() {
		return term;
	}

	public EntityType getType() {
		return type;
	}
	
	public CorpusEntity clone() {
		return new CorpusEntity(term, type, rawFreq, inDocumentsCount, rawFreqs);
	}

	private String getNormalizedTerm() {
		if (normalizedString==null) {normalizedString = Normalizer.normalize(term, Normalizer.Form.NFD);}
		return normalizedString;
	}

	public static Comparator<CorpusEntity> getComparator(Sort sort) {
		switch (sort) {
		case RAWFREQASC:
			return RawFrequencyAscendingComparator;
		case TERMASC:
			return TermAscendingComparator;
		case TERMDESC:
			return TermDescendingComparator;
		case INDOCUMENTSCOUNTASC:
			return InDocumentsCountAscendingComparator;
		case INDOCUMENTSCOUNTDESC:
			return InDocumentsCountDescendingComparator;
		default: // rawFrequencyDesc
			return RawFrequencyDescendingComparator;
		}
	}

	private static Comparator<CorpusEntity> TermAscendingComparator = new Comparator<CorpusEntity>() {
		@Override
		public int compare(CorpusEntity term1, CorpusEntity term2) {
			int i = term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
			if (i==0) {
				return term1.rawFreq - term2.rawFreq;
			}
			return i;
		}
	};

	private static Comparator<CorpusEntity> TermDescendingComparator = new Comparator<CorpusEntity>() {
		@Override
		public int compare(CorpusEntity term1, CorpusEntity term2) {
			int i = term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			if (i==0) {
				return term1.rawFreq - term2.rawFreq;
			}
			return i;
		}
	};

	private static Comparator<CorpusEntity> RawFrequencyDescendingComparator = new Comparator<CorpusEntity>() {

		@Override
		public int compare(CorpusEntity term1, CorpusEntity term2) {
			if (term1.rawFreq==term2.rawFreq) {
				return term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			}
			else {
				return term2.rawFreq - term1.rawFreq;
			}
		}
		
	};

	private static Comparator<CorpusEntity> RawFrequencyAscendingComparator = new Comparator<CorpusEntity>() {

		@Override
		public int compare(CorpusEntity term1, CorpusEntity term2) {
			if (term1.rawFreq==term2.rawFreq) {
				return term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			}
			else {
				return term1.rawFreq - term2.rawFreq;
			}
		}
		
	};
	
	private static Comparator<CorpusEntity> InDocumentsCountAscendingComparator = new Comparator<CorpusEntity>() {
		@Override
		public int compare(CorpusEntity term1, CorpusEntity term2) {
			if (term1.inDocumentsCount==term2.inDocumentsCount) {
				return term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			}
			else {
				return term1.inDocumentsCount - term2.inDocumentsCount;
			}
		}
		
	};
	private static Comparator<CorpusEntity> InDocumentsCountDescendingComparator = new Comparator<CorpusEntity>() {
		@Override
		public int compare(CorpusEntity term1, CorpusEntity term2) {
			if (term1.inDocumentsCount==term2.inDocumentsCount) {
				return term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			}
			else {
				return term2.inDocumentsCount - term1.inDocumentsCount;
			}
		}
		
	};
	
}

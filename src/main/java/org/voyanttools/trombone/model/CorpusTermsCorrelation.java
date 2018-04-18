/**
 * 
 */
package org.voyanttools.trombone.model;

import java.util.Comparator;

import org.voyanttools.trombone.model.CorpusTerm.Sort;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class CorpusTermsCorrelation {

	private CorpusTerm source;
	private CorpusTerm target;
	private float correlation;
	private float significance;
	
	public enum Sort {CORRELATIONASC, CORRELATIONDESC, CORRELATIONABS;
		
		public static Sort getForgivingly(FlexibleParameters parameters) {
			String sort = parameters.getParameterValue("sort", "").toUpperCase();
			String sortPrefix = "CORRELATION"; // default
			String dir = parameters.getParameterValue("dir", "").toUpperCase();
			String dirSuffix = "DESC";
			if (dir.endsWith("ASC")) {dirSuffix="ASC";}
			else if (dir.endsWith("ABS")) {dirSuffix="ABS";}
			return valueOf(sortPrefix+dirSuffix);
		}

	}
	/**
	 * 
	 */
	public CorpusTermsCorrelation(CorpusTerm source, CorpusTerm target, float correlation, float significance) {
		this.source = source;
		this.target = target;
		this.correlation = correlation;
		this.significance = significance;
	}
	public float getCorrelation() {
		return correlation;
	}
	public float getSignificance() {
		return significance;
	}
	public static Comparator<CorpusTermsCorrelation> getComparator(Sort sort) {
		switch (sort) {
		case CORRELATIONASC:
			return CorrelationAscending;
		case CORRELATIONABS:
			return CorrelationAbsolute;
		}
		return CorrelationDescending;
	}
	
	private static Comparator<CorpusTermsCorrelation> TieBreaker = new Comparator<CorpusTermsCorrelation>() {
		@Override
		public int compare(CorpusTermsCorrelation o1, CorpusTermsCorrelation o2) {
			int compare = Integer.compare(o2.source.getRawFrequency()+o2.target.getRawFrequency(), o1.source.getRawFrequency()+o1.target.getRawFrequency());
			if (compare!=0) {return compare;}
			compare = Integer.compare(o2.source.getInDocumentsCount()+o2.target.getInDocumentsCount(), o1.source.getInDocumentsCount()+o1.target.getInDocumentsCount());
			if (compare!=0) {return compare;}
			compare = o1.source.getTerm().compareTo(o2.source.getTerm());
			if (compare!=0) {return compare;}
			return o1.target.getTerm().compareTo(o2.target.getTerm());
		}
	};
	
	public static Comparator<CorpusTermsCorrelation> CorrelationAscending = new Comparator<CorpusTermsCorrelation>() {
		@Override
		public int compare(CorpusTermsCorrelation o1, CorpusTermsCorrelation o2) {
			int compare = Float.compare(o1.getCorrelation(), o2.getCorrelation());
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};

	public static Comparator<CorpusTermsCorrelation> CorrelationDescending = new Comparator<CorpusTermsCorrelation>() {
		@Override
		public int compare(CorpusTermsCorrelation o1, CorpusTermsCorrelation o2) {
			int compare = Float.compare(o2.getCorrelation(), o1.getCorrelation());
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};

	public static Comparator<CorpusTermsCorrelation> CorrelationAbsolute = new Comparator<CorpusTermsCorrelation>() {
		@Override
		public int compare(CorpusTermsCorrelation o1, CorpusTermsCorrelation o2) {
			int compare = Float.compare(Math.abs(o2.getCorrelation()), Math.abs(o1.getCorrelation()));
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};

	public CorpusTerm[] getCorpusTerms() {
		return new CorpusTerm[]{source, target};
	}
}

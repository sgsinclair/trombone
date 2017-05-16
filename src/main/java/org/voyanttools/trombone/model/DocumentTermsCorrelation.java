package org.voyanttools.trombone.model;

import java.util.Comparator;

import org.voyanttools.trombone.model.CorpusTermsCorrelation.Sort;
import org.voyanttools.trombone.util.FlexibleParameters;

public class DocumentTermsCorrelation {

	private DocumentTerm source;
	private DocumentTerm target;
	private float correlation;
	
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
	public DocumentTermsCorrelation(DocumentTerm source, DocumentTerm target, float correlation) {
		this.source = source;
		this.target = target;
		this.correlation = correlation;
	}
	
	public float getCorrelation() {
		return correlation;
	}
	
	public DocumentTerm[] getDocumentTerms() {
		return new DocumentTerm[]{source, target};
	}

	public static Comparator<DocumentTermsCorrelation> getComparator(DocumentTermsCorrelation.Sort sort) {
		switch (sort) {
		case CORRELATIONASC:
			return CorrelationAscending;
		case CORRELATIONABS:
			return CorrelationAbsolute;
		}
		return CorrelationDescending;
	}
	private static Comparator<DocumentTermsCorrelation> TieBreaker = new Comparator<DocumentTermsCorrelation>() {
		@Override
		public int compare(DocumentTermsCorrelation o1, DocumentTermsCorrelation o2) {
			int compare = Integer.compare(o2.source.getRawFrequency()+o2.target.getRawFrequency(), o1.source.getRawFrequency()+o1.target.getRawFrequency());
			if (compare!=0) {return compare;}
			compare = o1.source.getTerm().compareTo(o2.source.getTerm());
			if (compare!=0) {return compare;}
			return o1.target.getTerm().compareTo(o2.target.getTerm());
		}
	};
	
	public static Comparator<DocumentTermsCorrelation> CorrelationAscending = new Comparator<DocumentTermsCorrelation>() {
		@Override
		public int compare(DocumentTermsCorrelation o1, DocumentTermsCorrelation o2) {
			int compare = Float.compare(o1.getCorrelation(), o2.getCorrelation());
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};

	public static Comparator<DocumentTermsCorrelation> CorrelationDescending = new Comparator<DocumentTermsCorrelation>() {
		@Override
		public int compare(DocumentTermsCorrelation o1, DocumentTermsCorrelation o2) {
			int compare = Float.compare(o2.getCorrelation(), o1.getCorrelation());
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};

	public static Comparator<DocumentTermsCorrelation> CorrelationAbsolute = new Comparator<DocumentTermsCorrelation>() {
		@Override
		public int compare(DocumentTermsCorrelation o1, DocumentTermsCorrelation o2) {
			int compare = Float.compare(Math.abs(o2.getCorrelation()), Math.abs(o1.getCorrelation()));
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};
}

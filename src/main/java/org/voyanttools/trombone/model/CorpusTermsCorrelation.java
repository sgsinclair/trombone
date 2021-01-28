/**
 * 
 */
package org.voyanttools.trombone.model;

import java.util.Comparator;
import java.util.List;

import org.voyanttools.trombone.model.CorpusTerm.Sort;
import org.voyanttools.trombone.tool.util.ToolSerializer;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author sgs
 *
 */
@XStreamConverter(CorpusTermsCorrelation.CorpusTermsCorrelationConverter.class)
public class CorpusTermsCorrelation {

	private CorpusTerm source;
	private CorpusTerm target;
	private float correlation;
	private float significance;
	
	public enum Sort {CORRELATIONASC, CORRELATIONDESC, CORRELATIONABS, SIGNIFICANCEASC, SIGNIFICANCEDESC, SIGNIFICANCEABS;
		
		public static Sort getForgivingly(FlexibleParameters parameters) {
			String sort = parameters.getParameterValue("sort", "").toUpperCase();
			String sortPrefix = "CORRELATION"; // default
			if (sort.startsWith("SIGNIFICANCE")) {sortPrefix = "SIGNIFICANCE";}
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
		case CORRELATIONDESC:
			return CorrelationDescending;
		case SIGNIFICANCEASC:
			return SignificanceAscending;
		case SIGNIFICANCEABS:
			return SignificanceAbsolute;
		case SIGNIFICANCEDESC:
			return SignificanceDescending;
		default:
			return CorrelationDescending;
		}
		
	}
	
	private static Comparator<CorpusTermsCorrelation> TieBreaker = new Comparator<CorpusTermsCorrelation>() {
		@Override
		public int compare(CorpusTermsCorrelation o1, CorpusTermsCorrelation o2) {
			int compare = Integer.compare(o2.source.getRawFrequency(), o1.source.getRawFrequency());
			if (compare!=0) {return compare;}
			compare = Integer.compare(o2.source.getInDocumentsCount(), o1.source.getInDocumentsCount());
			if (compare!=0) {return compare;}
			compare = o2.source.getTerm().compareTo(o1.source.getTerm());
			if (compare!=0) {return compare;}
			return o1.target.getTerm().compareTo(o2.target.getTerm());
		}
	};
	
	public static Comparator<CorpusTermsCorrelation> CorrelationAscending = new Comparator<CorpusTermsCorrelation>() {
		@Override
		public int compare(CorpusTermsCorrelation o1, CorpusTermsCorrelation o2) {
			int compare = Float.compare(o2.getCorrelation(), o1.getCorrelation());
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};

	public static Comparator<CorpusTermsCorrelation> CorrelationDescending = new Comparator<CorpusTermsCorrelation>() {
		@Override
		public int compare(CorpusTermsCorrelation o1, CorpusTermsCorrelation o2) {
			int compare = Float.compare(o1.getCorrelation(), o2.getCorrelation());
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};

	public static Comparator<CorpusTermsCorrelation> CorrelationAbsolute = new Comparator<CorpusTermsCorrelation>() {
		@Override
		public int compare(CorpusTermsCorrelation o1, CorpusTermsCorrelation o2) {
			int compare = Float.compare(Math.abs(o1.getCorrelation()), Math.abs(o2.getCorrelation()));
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};

	public static Comparator<CorpusTermsCorrelation> SignificanceAscending = new Comparator<CorpusTermsCorrelation>() {
		@Override
		public int compare(CorpusTermsCorrelation o1, CorpusTermsCorrelation o2) {
			int compare = Float.compare(o2.getSignificance(), o1.getSignificance());
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};

	public static Comparator<CorpusTermsCorrelation> SignificanceDescending = new Comparator<CorpusTermsCorrelation>() {
		@Override
		public int compare(CorpusTermsCorrelation o1, CorpusTermsCorrelation o2) {
			int compare = Float.compare(o1.getSignificance(), o2.getSignificance());
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};

	public static Comparator<CorpusTermsCorrelation> SignificanceAbsolute = new Comparator<CorpusTermsCorrelation>() {
		@Override
		public int compare(CorpusTermsCorrelation o1, CorpusTermsCorrelation o2) {
			int compare = Float.compare(Math.abs(o1.getSignificance()), Math.abs(o2.getSignificance()));
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};

	public CorpusTerm[] getCorpusTerms() {
		return new CorpusTerm[]{source, target};
	}
	
	public static class CorpusTermsCorrelationConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return CorpusTermsCorrelation.class.isAssignableFrom(type);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			CorpusTermsCorrelation corpusTermCorrelation = (CorpusTermsCorrelation) source;
			
			boolean withDistributions = Boolean.TRUE.equals(context.get("withDistributions"));
			boolean termsOnly = Boolean.TRUE.equals(context.get("termsOnly"));
			
			writer.startNode("correlation");
	        
	        int i = 0;
	        for (CorpusTerm corpusTerm : corpusTermCorrelation.getCorpusTerms()) {
		        writer.startNode(i++==0 ? "source" : "target");
		        if (termsOnly) {
					writer.setValue(corpusTerm.getTerm());
		        } else {
			        writer.startNode("term");
					writer.setValue(corpusTerm.getTerm());
					writer.endNode();
					
					ToolSerializer.startNode(writer, "inDocumentsCount", Integer.class);
					writer.setValue(String.valueOf(corpusTerm.getInDocumentsCount()));
					ToolSerializer.endNode(writer);
			        
					ToolSerializer.startNode(writer, "rawFreq", Integer.class);
					writer.setValue(String.valueOf(corpusTerm.getRawFrequency()));
					ToolSerializer.endNode(writer);
			        
					ToolSerializer.startNode(writer, "relativePeakedness", Float.class);
					writer.setValue(String.valueOf(corpusTerm.getPeakedness()));
					ToolSerializer.endNode(writer);
			        
					ToolSerializer.startNode(writer, "relativeSkewness", Float.class);
					writer.setValue(String.valueOf(corpusTerm.getSkewness()));
					ToolSerializer.endNode(writer);
					
					if (withDistributions) {
						ToolSerializer.startNode(writer, "distributions", List.class);
				        context.convertAnother(corpusTerm.getRelativeDistributions());
				        ToolSerializer.endNode(writer);
					}
		        }
				writer.endNode();
	        }
	        
	        ToolSerializer.startNode(writer, "correlation", Float.class);
			writer.setValue(String.valueOf(corpusTermCorrelation.getCorrelation()));
			ToolSerializer.endNode(writer);
	        
			ToolSerializer.startNode(writer, "significance", Float.class);
			writer.setValue(String.valueOf(corpusTermCorrelation.getSignificance()));
			ToolSerializer.endNode(writer);
			
			writer.endNode();
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}

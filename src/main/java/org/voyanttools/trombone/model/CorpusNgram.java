/**
 * 
 */
package org.voyanttools.trombone.model;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;

import org.voyanttools.trombone.tool.util.ToolSerializer;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author sgs
 *
 */

@XStreamAlias("ngram")
@XStreamConverter(CorpusNgram.CorpusNgramConverter.class)
public class CorpusNgram {

	public enum Sort {
		RAWFREQASC, RAWFREQDESC, TERMASC, TERMDESC, LENGTHASC, LENGTHDESC;

		public static Sort getForgivingly(FlexibleParameters parameters) {
			String sort = parameters.getParameterValue("sort", "").toUpperCase();
			String sortPrefix = "RAWFREQ"; // default
			if (sort.startsWith("TERM")) {sortPrefix = "TERM";}
			else if (sort.startsWith("LENGTH")) {sortPrefix = "LENGTH";}
			String dir = parameters.getParameterValue("dir", "").toUpperCase();
			String dirSuffix = "DESC";
			if (dir.endsWith("ASC")) {dirSuffix="ASC";}
			return valueOf(sortPrefix+dirSuffix);
		}

	}

	private String term;
	private int rawFreq;
	private int length;
	private int[] distributions;

	@XStreamOmitField
	private transient String normalizedString = null;

	/**
	 * @param rawFreqs 
	 * @param length 
	 * @param term 
	 * 
	 */
	public CorpusNgram(String term, int length, int[] rawFreqs) {
		this.term = term;
		this.length = length;
		this.distributions = rawFreqs;
		this.rawFreq = 0;
		for (int i : rawFreqs) {
			this.rawFreq+=i;
		}
	}

	private String getNormalizedTerm() {
		if (normalizedString==null) {normalizedString = Normalizer.normalize(term, Normalizer.Form.NFD);}
		return normalizedString;
	}

	public static Comparator<CorpusNgram> getComparator(Sort sort) {
		switch (sort) {
		case RAWFREQASC:
			return RawFrequencyAscendingComparator;
		case TERMASC:
			return TermAscendingComparator;
		case TERMDESC:
			return TermDescendingComparator;
		case LENGTHASC:
			return LengthAscendingComparator;
		case LENGTHDESC:
			return LengthDescendingComparator;
		default: // rawFrequencyDesc
			return RawFrequencyDescendingComparator;
		}	
	}
	
	private static Comparator<CorpusNgram> TermAscendingComparator = new Comparator<CorpusNgram>() {
		@Override
		public int compare(CorpusNgram term1, CorpusNgram term2) {
			int i = term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
			if (i==0) {
				return term1.rawFreq - term2.rawFreq;
			}
			return i;
		}
	};

	private static Comparator<CorpusNgram> TermDescendingComparator = new Comparator<CorpusNgram>() {
		@Override
		public int compare(CorpusNgram term1, CorpusNgram term2) {
			int i = term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			if (i==0) {
				return term1.rawFreq - term2.rawFreq;
			}
			return i;
		}
	};

	private static Comparator<CorpusNgram> RawFrequencyDescendingComparator = new Comparator<CorpusNgram>() {

		@Override
		public int compare(CorpusNgram term1, CorpusNgram term2) {
			if (term1.rawFreq==term2.rawFreq) {
				return term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			}
			else {
				return term2.rawFreq - term1.rawFreq;
			}
		}
		
	};

	private static Comparator<CorpusNgram> RawFrequencyAscendingComparator = new Comparator<CorpusNgram>() {

		@Override
		public int compare(CorpusNgram term1, CorpusNgram term2) {
			if (term1.rawFreq==term2.rawFreq) {
				return term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
			}
			else {
				return term1.rawFreq - term2.rawFreq;
			}
		}
		
	};

	private static Comparator<CorpusNgram> LengthDescendingComparator = new Comparator<CorpusNgram>() {

		@Override
		public int compare(CorpusNgram term1, CorpusNgram term2) {
			if (term1.length==term2.length) {
				return term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			}
			else {
				return term2.length - term1.length;
			}
		}
		
	};

	private static Comparator<CorpusNgram> LengthAscendingComparator = new Comparator<CorpusNgram>() {

		@Override
		public int compare(CorpusNgram term1, CorpusNgram term2) {
			if (term1.length==term2.length) {
				return term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
			}
			else {
				return term1.length - term2.length;
			}
		}
		
	};

	@Override
	public String toString() {
		return "{"+term+": "+rawFreq+" ("+length+")";
	}

	public String getTerm() {
		return term;
	}

	public int getRawFreq() {
		return rawFreq;
	}

	public int getLength() {
		return length;
	}

	public Object getDistributions() {
		return distributions;
	}

	public static class CorpusNgramConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return CorpusNgram.class.isAssignableFrom(type);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			CorpusNgram corpusNgram = (CorpusNgram) source;
			
			boolean withDistributions = Boolean.TRUE.equals(context.get("withDistributions"));
			
			writer.startNode("ngram");
	        
	        writer.startNode("term");
			writer.setValue(corpusNgram.getTerm());
			writer.endNode();

	        ToolSerializer.startNode(writer, "rawFreq", Integer.class);
			writer.setValue(String.valueOf(corpusNgram.getRawFreq()));
			ToolSerializer.endNode(writer);

			ToolSerializer.startNode(writer, "length", Integer.class);
			writer.setValue(String.valueOf(corpusNgram.getLength()));
			ToolSerializer.endNode(writer);

        	if (withDistributions) {
        		ToolSerializer.startNode(writer, "distributions", List.class);
		        context.convertAnother(corpusNgram.getDistributions());
		        ToolSerializer.endNode(writer);
        	}
        	
        	writer.endNode();
			
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}

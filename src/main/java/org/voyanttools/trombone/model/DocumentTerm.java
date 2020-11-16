/*******************************************************************************
 * Trombone is a flexible text processing and analysis library used
 * primarily by Voyant Tools (voyant-tools.org).
 * 
 * Copyright (©) 2007-2012 Stéfan Sinclair & Geoffrey Rockwell
 * 
 * This file is part of Trombone.
 * 
 * Trombone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.model;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.tool.util.ToolSerializer;
import org.voyanttools.trombone.util.FlexibleParameters;

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
@XStreamConverter(DocumentTerm.DocumentTermConverter.class)
public class DocumentTerm {

	public enum Sort {
		RAWFREQASC, RAWFREQDESC, RELATIVEFREQASC, RELATIVEFREQDESC, TERMASC, TERMDESC, TFIDFASC, TFIDFDESC, ZSCOREASC, ZSCOREDESC;
		public static Sort getForgivingly(FlexibleParameters parameters) {
			String sort = parameters.getParameterValue("sort", "").toUpperCase();
			String sortPrefix = "RELATIVEFREQ"; // default
			if (sort.startsWith("RAWFREQ")) {sortPrefix = "RAWFREQ";}
			if (sort.startsWith("TERM")) {sortPrefix = "TERM";}
			if (sort.startsWith("TFIDF")) {sortPrefix = "TFIDF";}
			if (sort.startsWith("ZSCORE")) {sortPrefix = "ZSCORE";}
			String dir = parameters.getParameterValue("dir", "").toUpperCase();
			String dirSuffix = "DESC";
			if (dir.endsWith("ASC")) {dirSuffix="ASC";}
			return valueOf(sortPrefix+dirSuffix);			
		}
	}

	protected int docIndex;
	protected String docId;
	protected String term;
	@XStreamOmitField
	protected String normalizedString;
	protected int rawFreq;
	protected int totalTermsCount;
	protected float relativeFreq;
	protected float zscore;
	protected float zscoreRatio;
	protected float tfidf;
	protected int[] positions;
	protected int[] offsets;
	protected CorpusTermMinimal corpusTermMinimal;
	private transient Map<Integer, float[]> relativeDistributionsMap;
	
	public DocumentTerm(int docIndex, String docId, String term, int rawFreq, int totalTokens, float zscore, int[] positions, int[] offsets, CorpusTermMinimal corpusTermMinimal) {
		this.docIndex = docIndex;
		this.docId = docId;
		this.term = term;
		this.rawFreq = rawFreq;
		this.totalTermsCount = totalTokens;
		this.relativeFreq = totalTokens > 0 ? ((float) rawFreq / totalTokens) * 1000000 : 0;
		this.zscore = zscore;
		this.positions = positions;
		this.offsets = offsets;
		this.normalizedString = null;
		this.tfidf = Float.NaN;
		this.zscoreRatio = Float.NaN;
		this.corpusTermMinimal = corpusTermMinimal;
		relativeDistributionsMap = new HashMap<Integer, float[]>();
	}
	public int getRawFrequency() {
		return rawFreq;
	}
	public String getNormalizedTerm() {
		if (normalizedString==null) {normalizedString = Normalizer.normalize(term, Normalizer.Form.NFD);}
		return normalizedString;
	}
	
	public String getTerm() {
		return term;
	}
	@Override
	public String toString() {
		return "(doc "+docIndex+") "+term+": "+rawFreq+" ("+relativeFreq+")";
	}

	public float getRelativeFrequency() {
		return relativeFreq;
	}
	
	public float getZscore() {
		return zscore;
	}

	public int getDocumentIndex() {
		return docIndex;
	}
	
	public int[] getOffsets() {
		return offsets;
	}
	
	public int[] getPositions() {
		return positions;
	}

	public int[] getRawDistributions(int bins) {
		if (positions==null || bins ==0) return new int[0];
		int[] distributions = new int[bins];
		for(int position : positions) {
			distributions[(int) (position*bins/totalTermsCount)]++;
		}
		return distributions;
	}
	
	public float[] getRelativeDistributions(int bins) {
		if (positions==null || bins ==0) return new float[0];
		if (relativeDistributionsMap.containsKey(bins)) {
			return relativeDistributionsMap.get(bins);
		} else {
			int[] rawDistributions = getRawDistributions(bins);
			float[] distributions = new float[bins];
			for (int i=0, len = rawDistributions.length; i<len; i++) {
				distributions[i] = (float) rawDistributions[i] / totalTermsCount;
			}
			relativeDistributionsMap.put(bins, distributions);
			return distributions;
			
		}
	}
	
	public static Comparator<DocumentTerm> getComparator(Sort sort) {
		switch (sort) {
		case RAWFREQASC:
			return RawFrequencyAscendingComparator;
		case TERMASC:
			return TermAscendingComparator;
		case TERMDESC:
			return TermDescendingComparator;
		case RAWFREQDESC:
			return RawFrequencyDescendingComparator;
		case RELATIVEFREQASC:
			return RelativeFrequencyAscendingComparator;
		case TFIDFASC:
			return TfIdfAscendingComparator;
		case TFIDFDESC:
			return TfIdfDescendingComparator;
		case ZSCOREASC:
			return ZscoreAscendingComparator;
		case ZSCOREDESC:
			return ZscoreDescendingComparator;
		default: // relativeDesc
			return RelativeFrequencyDescendingComparator;
		}
	}
	private static Comparator<DocumentTerm> TermAscendingComparator = new Comparator<DocumentTerm>() {
		@Override
		public int compare(DocumentTerm term1, DocumentTerm term2) {
			if (term2.getTerm().equals(term1.getTerm())) {
				if (term1.relativeFreq==term2.relativeFreq) {
					if (term1.rawFreq==term2.rawFreq) { // 
						return Integer.compare(term2.docIndex, term1.docIndex);
					}
					else {
						return Integer.compare(term1.rawFreq, term2.rawFreq);
					}
				}
				else {
					return Float.compare(term1.relativeFreq, term2.relativeFreq);
				}
			}
			else {
				return term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
			}
		}
	};

	private static Comparator<DocumentTerm> TermDescendingComparator = new Comparator<DocumentTerm>() {
		@Override
		public int compare(DocumentTerm term1, DocumentTerm term2) {
			if (term2.getTerm().equals(term1.getTerm())) {
				if (term1.relativeFreq==term2.relativeFreq) {
					if (term1.rawFreq==term2.rawFreq) { // 
						return Integer.compare(term2.docIndex, term1.docIndex);
					}
					else {
						return Integer.compare(term1.rawFreq, term2.rawFreq);
					}
				}
				else {
					return Float.compare(term1.relativeFreq, term2.relativeFreq);
				}
			}
			else {
				return term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
			}
		}
	};

	private static Comparator<DocumentTerm> RawFrequencyDescendingComparator = new Comparator<DocumentTerm>() {

		@Override
		public int compare(DocumentTerm term1, DocumentTerm term2) {
			if (term1.rawFreq==term2.rawFreq) {
				if (term1.relativeFreq==term2.relativeFreq) {
					if (term2.getTerm().equals(term1.getTerm())) {
						return Integer.compare(term2.docIndex, term1.docIndex);
					}
					else {
						return term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
					}
				}
				else {
					return Float.compare(term2.relativeFreq, term1.relativeFreq);
				}
			}
			else {
				return term1.rawFreq - term2.rawFreq;
			}
		}
		
	};
	
	private static Comparator<DocumentTerm> RawFrequencyAscendingComparator = new Comparator<DocumentTerm>() {

		@Override
		public int compare(DocumentTerm term1, DocumentTerm term2) {
			if (term1.rawFreq==term2.rawFreq) {
				if (term1.relativeFreq==term2.relativeFreq) {
					if (term2.getTerm().equals(term1.getTerm())) {
						return Integer.compare(term2.docIndex, term1.docIndex);
					}
					else {
						return term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
					}
				}
				else {
					return Float.compare(term1.relativeFreq, term2.relativeFreq);
				}
			}
			else {
				return term2.rawFreq - term1.rawFreq;
			}
		}
		
	};

	private static Comparator<DocumentTerm> RelativeFrequencyAscendingComparator = new Comparator<DocumentTerm>() {

		@Override
		public int compare(DocumentTerm term1, DocumentTerm term2) {
			if (term1.relativeFreq==term2.relativeFreq) {
				if (term1.rawFreq==term2.rawFreq) {
					if (term2.getTerm().equals(term1.getTerm())) {
						return Integer.compare(term2.docIndex, term1.docIndex);
					}
					else {
						return term2.getNormalizedTerm().compareTo(term1.getNormalizedTerm());
					}
				}
				else {
					return Integer.compare(term1.rawFreq, term2.rawFreq);
				}
			}
			else {
				return Float.compare(term1.relativeFreq,  term2.relativeFreq);
			}
		}
		
	};

	private static Comparator<DocumentTerm> RelativeFrequencyDescendingComparator = new Comparator<DocumentTerm>() {

		@Override
		public int compare(DocumentTerm term1, DocumentTerm term2) {
			if (term1.relativeFreq==term2.relativeFreq) {
				if (term1.rawFreq==term2.rawFreq) {
					if (term2.getTerm().equals(term1.getTerm())) {
						return Integer.compare(term2.docIndex, term1.docIndex);
					}
					else {
						return term1.getNormalizedTerm().compareTo(term2.getNormalizedTerm());
					}
				}
				else {
					return Integer.compare(term1.rawFreq, term2.rawFreq);
				}
			}
			else {
				return Float.compare(term2.relativeFreq,  term1.relativeFreq);
			}
		}
		
	};
	
	private static Comparator<DocumentTerm> TfIdfDescendingComparator = new Comparator<DocumentTerm>() {

		@Override
		public int compare(DocumentTerm term1, DocumentTerm term2) {
			float f1 = term1.getTfIdf();
			float f2 = term2.getTfIdf();
			if (f1==f2) {
				return TermAscendingComparator.compare(term1, term2);
			}
			else {
				return Float.compare(f2, f1);
			}
		}
		
	};
	
	private static Comparator<DocumentTerm> TfIdfAscendingComparator = new Comparator<DocumentTerm>() {

		@Override
		public int compare(DocumentTerm term1, DocumentTerm term2) {
			float f1 = term1.getTfIdf();
			float f2 = term2.getTfIdf();
			if (f1==f2) {
				return TermAscendingComparator.compare(term1, term2);
			}
			else {
				return Float.compare(f1, f2);
			}
		}
		
	};
	
	private static Comparator<DocumentTerm> ZscoreDescendingComparator = new Comparator<DocumentTerm>() {

		@Override
		public int compare(DocumentTerm term1, DocumentTerm term2) {
			float f1 = term1.getZscore();
			float f2 = term2.getZscore();
			if (f1==f2) {
				return TermAscendingComparator.compare(term1, term2);
			}
			else {
				return Float.compare(f2, f1);
			}
		}
		
	};
	
	private static Comparator<DocumentTerm> ZscoreAscendingComparator = new Comparator<DocumentTerm>() {

		@Override
		public int compare(DocumentTerm term1, DocumentTerm term2) {
			float f1 = term1.getZscore();
			float f2 = term2.getZscore();
			if (f1==f2) {
				return TermAscendingComparator.compare(term1, term2);
			}
			else {
				return Float.compare(f1, f2);
			}
		}
		
	};
	public int getTotalTermsCount() {
		return totalTermsCount;
	}
	
	public int getDocIndex() {
		return docIndex;
	}
	
	public String getDocId() {
		return docId;
	}
	public float getZscoreRatio() {
		if (corpusTermMinimal!=null && Float.isNaN(zscoreRatio)) {
			float corpusZscore = corpusTermMinimal.getZscore();
			if (zscore!=0 && corpusZscore!=0) {
				zscoreRatio = zscore > corpusZscore ? zscore / corpusZscore : -(corpusZscore / zscore);
			}
		}
		return zscoreRatio;
	}
	public float getTfIdf() {
		if (corpusTermMinimal!=null && Float.isNaN(tfidf)) {
			int inDocuments = corpusTermMinimal.getInDocumentsCount();
			if (inDocuments>0) {
				this.tfidf = ((float) rawFreq / (float) totalTermsCount) * (float) Math.log10((float) corpusTermMinimal.getDocumentsCount() / (float) inDocuments);
			}
		}
		return tfidf;
	}
	
	public static class DocumentTermConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return DocumentTerm.class.isAssignableFrom(type);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			DocumentTerm documentTerm = (DocumentTerm) source;
			
			boolean withRawDistributions = Boolean.TRUE.equals(context.get("withRawDistributions"));
			boolean withRelativeDistributions = Boolean.TRUE.equals(context.get("withRelativeDistributions"));
			boolean withOffsets = Boolean.TRUE.equals(context.get("withOffsets"));
			boolean withPositions = Boolean.TRUE.equals(context.get("withPositions"));
			int bins = Integer.parseInt(String.valueOf(context.get("bins")));
			
			writer.startNode("term"); // not written in JSON
	        
	        writer.startNode("term");
			writer.setValue(documentTerm.getTerm());
			writer.endNode();
			
	        ToolSerializer.startNode(writer, "rawFreq", Integer.class);
			writer.setValue(String.valueOf(documentTerm.getRawFrequency()));
			ToolSerializer.endNode(writer);

			ToolSerializer.startNode(writer, "relativeFreq", Float.class);
			writer.setValue(String.valueOf(documentTerm.getRelativeFrequency()));
			ToolSerializer.endNode(writer);
			
			ToolSerializer.startNode(writer, "zscore", Float.class);
			writer.setValue(String.valueOf(documentTerm.getZscore()));
			ToolSerializer.endNode(writer);
			
			ToolSerializer.startNode(writer, "zscoreRatio", Float.class);
			writer.setValue(String.valueOf(documentTerm.getZscoreRatio()));
			ToolSerializer.endNode(writer);
			
			ToolSerializer.startNode(writer, "tfidf", Float.class);
			writer.setValue(String.valueOf(documentTerm.getTfIdf()));
			ToolSerializer.endNode(writer);
			
			ToolSerializer.startNode(writer, "totalTermsCount", Integer.class);
			writer.setValue(String.valueOf(documentTerm.getTotalTermsCount()));
			ToolSerializer.endNode(writer);
			
			ToolSerializer.startNode(writer, "docIndex", Integer.class);
			writer.setValue(String.valueOf(documentTerm.getDocIndex()));
			ToolSerializer.endNode(writer);
			
	        writer.startNode("docId");
			writer.setValue(documentTerm.getDocId());
			writer.endNode();

			if (withRawDistributions) {
				ToolSerializer.startNode(writer, "distributions", List.class);
		        context.convertAnother(documentTerm.getRawDistributions(bins));
		        ToolSerializer.endNode(writer);
			}
			if (withRelativeDistributions) {
				ToolSerializer.startNode(writer, "distributions", List.class);
		        context.convertAnother(documentTerm.getRelativeDistributions(bins));
		        ToolSerializer.endNode(writer);
			}
			
			if (withOffsets) {
				ToolSerializer.startNode(writer, "offsets", List.class);
		        context.convertAnother(documentTerm.getOffsets());
		        ToolSerializer.endNode(writer);
			}
			
			if (withPositions) {
				ToolSerializer.startNode(writer, "positions", List.class);
		        context.convertAnother(documentTerm.getPositions());
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

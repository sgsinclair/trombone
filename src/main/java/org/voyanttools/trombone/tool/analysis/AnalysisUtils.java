package org.voyanttools.trombone.tool.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.voyanttools.trombone.model.RawAnalysisTerm;
import org.voyanttools.trombone.model.RawCATerm;
import org.voyanttools.trombone.model.RawCATerm.CategoryType;
import org.voyanttools.trombone.model.table.Table;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public final class AnalysisUtils {
	
	private AnalysisUtils() {}
	
	public static double[][] getMatrixFromParameters(FlexibleParameters parameters, List<RawCATerm> analysisTerms) {
		double[][] freqMatrix = null;
		
		if (parameters.containsKey("analysisInput")) {
			
			String format = parameters.getParameterValue("inputFormat").toLowerCase();
			if (format.equals("tsv")) {
				boolean columnHeaders = parameters.getParameterBooleanValue("columnHeaders");
				boolean rowHeaders = parameters.getParameterBooleanValue("rowHeaders");
				Table table = new Table(parameters.getParameterValue("analysisInput"),
						Table.Format.getForgivingly(parameters.getParameterValue("inputFormat", "tsv")),
						columnHeaders,
						false // always send false for rowHeaders since we need to get them as a column later
					);
				
				String[] terms = table.getColumn(0);
				int rows = terms.length;
				
				for (int i = 0; i < rows; i++) {
					String term = terms[i];
					if (!rowHeaders) {
						term = String.valueOf(i);
					}
					analysisTerms.add(new RawCATerm(term, 1, 1, CategoryType.TERM));
				}
				int cols = table.getColumnsCount();
				if (rowHeaders) {
					cols--;
				}
				freqMatrix = new double[rows][cols];
				int offset = rowHeaders ? 1 : 0;
				for (int i = 0; i < cols; i++) {
					double[] col = table.getColumnAsDoubles(i+offset);
					for (int j = 0; j < col.length; j++) {
						freqMatrix[j][i] = col[j];
					}
				}
			} else if (format.equals("matrix")) {
				String matrixStr = parameters.getParameterValue("analysisInput");
				freqMatrix = AnalysisUtils.getMatrixFromString(matrixStr);
				AnalysisUtils.addTermsFromMatrix(freqMatrix, analysisTerms);
			}
		}
		
		return freqMatrix;
	}
	
	// expected format: [[1,2,3],[4,5,6]]
	private static double[][] getMatrixFromString(String matrixStr) {
		double[][] freqMatrix = null;
		matrixStr = matrixStr.replaceAll("^\\[", "").replaceAll("\\]$", "").replaceAll("\\s", "");
		String[] rows = matrixStr.split("(?<=\\]),(?=\\[)");
		for (int i = 0; i < rows.length; i++) {
			String row = rows[i];
			row = row.replaceAll("^\\[", "").replaceAll("\\]$", "");
			String[] nums = row.split(",");
			for (int j = 0; j < nums.length; j++) {
				String num = nums[j];
				if (freqMatrix == null) {
					freqMatrix = new double[rows.length][nums.length];
				}
				double d = Double.valueOf(num);
				freqMatrix[i][j] = d;
			}
		}
		return freqMatrix;
	}
	
	private static void addTermsFromMatrix(double[][] matrix, List<RawCATerm> analysisTerms) {
		int dimensionSize = matrix.length;
		if (dimensionSize > 0) {
			for (int i = 0; i < dimensionSize; i++) {
				analysisTerms.add(new RawCATerm(String.valueOf(i), 1, 1, CategoryType.TERM));
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void filterTermsByTarget(List<? extends RawAnalysisTerm> terms, double[] target, double maxDistance, List<String> whitelist) {
		Iterator<RawAnalysisTerm> it = (Iterator<RawAnalysisTerm>) terms.iterator();
		while (it.hasNext()) {
			RawAnalysisTerm term = it.next();
			double distance = getDistance(term.getVector(), target);
			if (!whitelist.contains(term.getTerm())) {
				if (distance > maxDistance) {
					it.remove();
				}
			}
		}
	}
	
	public static double getDistance(double[] p1, double[] p2) {
		if (p1 == null || p2 == null) {
			return Double.MAX_VALUE;
		} else {
			return Math.sqrt(Math.pow(p2[0] - p1[0], 2) + Math.pow(p2[1] - p1[1], 2));
		}
	}
	
	public static double[][] getMinMax(double[][] input) {
		double[][] minMax = new double[2][2];
		// min xy
		minMax[0][0] = Double.MAX_VALUE;
		minMax[0][1] = Double.MAX_VALUE;
		// max xy
		minMax[1][0] = Double.MIN_VALUE;
		minMax[1][1] = Double.MIN_VALUE;
		
		int n = input.length;
		for (int i = 0; i < n; i++) {
			double[] testVal = input[i];
			if (testVal[0] < minMax[0][0]) minMax[0][0] = testVal[0];
			if (testVal[1] < minMax[0][1]) minMax[0][1] = testVal[1];
			if (testVal[0] > minMax[1][0]) minMax[1][0] = testVal[0];
			if (testVal[1] > minMax[1][1]) minMax[1][1] = testVal[1];
		}

		return minMax;
	}
	
	public static void clusterPoints(List<? extends RawAnalysisTerm> terms, int k) {
		Collection<DoublePoint> data = new ArrayList<DoublePoint>();
		for (RawAnalysisTerm term : terms) {
			data.add(new DoublePoint(term));
		}
		
		List<CentroidCluster<DoublePoint>> clusters = null;
		try {
			KMeansPlusPlusClusterer<DoublePoint> clusterer = new KMeansPlusPlusClusterer<DoublePoint>(k, 5000);
			clusters = clusterer.cluster(data);
		} catch (Exception e) {
			// couldn't cluster
		}
		if (clusters != null) {
			int clusterCounter = 0;
			for (CentroidCluster<DoublePoint> cluster : clusters) {
				List<DoublePoint> points = cluster.getPoints();
				Clusterable center = cluster.getCenter();
				for (DoublePoint p : points) {
					p.getTerm().setCluster(clusterCounter);
					// TODO center seems to be calculated and not selected from initial data, therefore no points will ever be the center
					if (p.getPoint().equals(center.getPoint())) {
						p.getTerm().setClusterCenter(true);
					}
				}
				clusterCounter++;
			}
		}
	}
	
	public static void outputTerms(List<RawCATerm> terms, boolean includeCAFields, HierarchicalStreamWriter writer, MarshallingContext context) {
		ExtendedHierarchicalStreamWriterHelper.startNode(writer, "tokens", Map.class);
		for (RawCATerm term : terms) {
			writer.startNode("token");
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "term", String.class);
			writer.setValue(term.getTerm());
			writer.endNode();
			
			if (includeCAFields) {
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "category", String.class);
				writer.setValue(String.valueOf(((RawCATerm)term).getCategory()).toLowerCase());
				writer.endNode();
				
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, "docIndex", Integer.class);
				writer.setValue(String.valueOf(((RawCATerm)term).getDocIndex()));
				writer.endNode();
			}
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "rawFreq", Integer.class);
			writer.setValue(String.valueOf(term.getRawFrequency()));
			writer.endNode();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "relativeFreq", Float.class);
			writer.setValue(String.valueOf(term.getRelativeFrequency()));
			writer.endNode();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "cluster", Integer.class);
			writer.setValue(String.valueOf(term.getCluster()));
			writer.endNode();
			
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "clusterCenter", Boolean.class);
			writer.setValue(String.valueOf(term.isClusterCenter()));
			writer.endNode();
			
			double[] vectorDouble = term.getVector();
			float[] vectorFloat = new float[vectorDouble.length];
			for (int i = 0, size = vectorDouble.length; i < size; i++) 
				vectorFloat[i] = (float) vectorDouble[i];
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, "vector", vectorFloat.getClass());
            context.convertAnother(vectorFloat);
            writer.endNode();
			
        	writer.endNode();
		}
		writer.endNode();
	}
}

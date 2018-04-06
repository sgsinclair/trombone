package org.voyanttools.trombone.tool.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.voyanttools.trombone.model.RawCATerm;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.AnalysisTool;
import org.voyanttools.trombone.tool.analysis.AnalysisUtils;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

public abstract class TableAnalysisTool extends AbstractTool implements AnalysisTool {

	private String target;
	private int clusters;
	private int dimensions;
	
	private double[] targetVector;
	
	private List<RawCATerm> analysisTerms;
	
	public TableAnalysisTool(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		
		target = parameters.getParameterValue("target");
		clusters = parameters.getParameterIntValue("clusters");
		dimensions = parameters.getParameterIntValue("dimensions", 2);
		
		targetVector = null;
		
		analysisTerms = new ArrayList<RawCATerm>();
	}
	
	@Override
	public void run() throws IOException {
		double[][] result = runAnalysis(getInput());
		
		if (target != null && targetVector != null) {
			double[][] minMax = AnalysisUtils.getMinMax(result);
			double distance = AnalysisUtils.getDistance(minMax[0], minMax[1]) / 50;
			List<String> initialTerms = new ArrayList<String>(Arrays.asList(this.parameters.getParameterValues("term")));
			AnalysisUtils.filterTermsByTarget(analysisTerms, targetVector, distance, initialTerms);
		}
		
		if (clusters > 0) {
			AnalysisUtils.clusterPoints(analysisTerms, clusters);
		}
	}
	
	@Override
	public double[][] getInput() throws IOException {
		return AnalysisUtils.getMatrixFromParameters(parameters, getAnalysisTerms());
	}

	@Override
	public List<RawCATerm> getAnalysisTerms() {
		return analysisTerms;
	}

	@Override
	public String getTarget() {
		return target;
	}

	@Override
	public int getClusters() {
		return clusters;
	}

	@Override
	public int getDimensions() {
		return dimensions;
	}

	@Override
	public double[] getTargetVector() {
		return targetVector;
	}

	@Override
	public void setTargetVector(double[] tv) {
		targetVector = tv;
	}
}

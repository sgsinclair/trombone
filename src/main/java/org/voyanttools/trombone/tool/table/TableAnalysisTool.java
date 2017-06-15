package org.voyanttools.trombone.tool.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.voyanttools.trombone.model.RawCATerm;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.AnalysisUtils;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

public abstract class TableAnalysisTool extends AbstractTool {

	protected String target;
	private int clusters;
	protected int dimensions;
	
	protected double[] targetVector;
	
	protected List<RawCATerm> analysisTerms;
	
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
		double[][] result = runAnalysis();
		
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

	protected abstract double[][] runAnalysis() throws IOException;

}

package org.voyanttools.trombone.tool.analysis;

import java.io.IOException;
import java.util.List;

import org.voyanttools.trombone.model.RawCATerm;

public interface AnalysisTool {

	public int getClusters();
	public int getDimensions();
	
	public String getTarget();
	public double[] getTargetVector();
	public void setTargetVector(double[] tv);
	
	public List<RawCATerm> getAnalysisTerms();
	
	public double[][] getInput() throws IOException;
	
	public double[][] runAnalysis(double[][] input) throws IOException;
	
}

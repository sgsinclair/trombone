package org.voyanttools.trombone.tool.analysis;

import com.jujutsu.tsne.barneshut.BHTSne;
import com.jujutsu.tsne.barneshut.BarnesHutTSne;
import com.jujutsu.tsne.barneshut.TSneConfiguration;
import com.jujutsu.utils.TSneUtils;

public class TSNEAnalysis {

	/**
	 * The parameter theta specifies how coarse the Barnes-Hut approximation is:
	 * setting theta to 0 runs the original O(N2) t-SNE algorithm,
	 * whereas using higher values runs the O(N log N) with increasingly better constant.
	 * The value of theta should be between 0 and 1, and its default value is 0.5
	 */
	private final double defaultTheta = 0.5;
	
	/**
	 * The performance of t-SNE is fairly robust under different settings of the perplexity.
	 * The most appropriate value depends on the density of your data.
	 * Loosely speaking, one could say that a larger / denser dataset requires a larger perplexity.
	 * Typical values for the perplexity range between 5 and 50.
	 * 
	 * Perplexity is a measure for information that is defined as 2 to the power of the Shannon entropy.
	 * The perplexity of a fair die with k sides is equal to k.
	 * In t-SNE, the perplexity may be viewed as a knob that sets the number of effective nearest neighbors
	 */
	private final double defaultPerplexity = 25;
	
	/**
	 * Use PCA to reduce input dimensions, if necessary
	 */
	private boolean use_pca = false;
	
	private int iterations = 2000;
	
	private float perplexity;
	
	private float theta;
	
	private int dimensions;
	
	private double[][] input;
	
	private double[][] result;
	
	public TSNEAnalysis(double[][] input) {
		this.input = input;
		theta = (float) defaultTheta;
	}

	public void runAnalysis() {		
		int initial_dims = 2;
		
		int rows = input.length;
		
		float maxPerplexity = (rows-2)/3f; // more than this and tsne will fail
		if (perplexity <= 0) {
			perplexity = maxPerplexity;
		} else if (perplexity*3 > rows-1) {
			perplexity = maxPerplexity;
		}
		
	    BarnesHutTSne tsne = new BHTSne();
	    
	    TSneConfiguration config = TSneUtils.buildConfig(input, dimensions, initial_dims, perplexity, iterations, use_pca, theta, true, true);
	    
	    double[][] r = tsne.tsne(config);
	    result = r;
	}
	
	public void setIterations(int iterations) {
		this.iterations = Math.min(5000, Math.max(iterations, 10));
	}
	
	public void setPerplexity(float perplexity) {
		this.perplexity = perplexity;
	}
	
	public void setTheta(float theta) {
		theta = Math.min(1f, Math.max(theta, 0f));
	}

	public void setDimensions(int dimensions) {
		this.dimensions = dimensions;
	}
	
	public double[][] getResult() {
		return result;
	}


}

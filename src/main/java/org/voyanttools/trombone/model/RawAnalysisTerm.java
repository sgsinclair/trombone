package org.voyanttools.trombone.model;

/**
 * @author Andrew MacDonald
 */
public abstract class RawAnalysisTerm {
	
	private final String term;
	private double[] vector;
	private int cluster;
	private boolean clusterCenter;
	
	public RawAnalysisTerm(String term) {
		this.term = term;
	}
	
	public RawAnalysisTerm(String term, double[] vector) {
		this.term = term;
		this.vector = vector;
		this.cluster = -1;
		this.clusterCenter = false;
	}
	
	public String getTerm() {
		return this.term;
	}
	
	public double[] getVector() {
		return this.vector;
	}
	
	public void setVector(double[] vector) {
		this.vector = vector;
	}

	public int getCluster() {
		return this.cluster;
	}

	public void setCluster(int cluster) {
		this.cluster = cluster;
	}

	public void setClusterCenter(boolean clusterCenter) {
		this.clusterCenter = clusterCenter;
	}

	public boolean isClusterCenter() {
		return this.clusterCenter;
	}

}
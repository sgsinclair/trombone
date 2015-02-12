package org.voyanttools.trombone.model;

/**
 * @author Andrew MacDonald
 */
public abstract class RawAnalysisType {
	
	private final String type;
	private final double[] vector;
	private int cluster;
	private boolean clusterCenter;
	
//	@edu.umd.cs.findbugs.annotations.SuppressWarnings({ "EI_EXPOSE_REP2" })
	public RawAnalysisType(String type, double[] vector) {
		this.type = type;
		this.vector = vector;
		this.cluster = -1;
		this.clusterCenter = false;
	}
	
	public String getType() {
		return this.type;
	}
	
//	@edu.umd.cs.findbugs.annotations.SuppressWarnings({ "EI_EXPOSE_REP" })
	public double[] getVector() {
		return this.vector;
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
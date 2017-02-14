package org.voyanttools.trombone.tool.algorithms.pca;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

public class PrincipalComponentsAnalysis {

	private Matrix covMatrix;
	private EigenvalueDecomposition eigenstuff;
	private double[] eigenvalues;
	private Matrix eigenvectors;
	private SortedSet<PrincipleComponent> principleComponents;
	private double[] means;

//	@edu.umd.cs.findbugs.annotations.SuppressWarnings({ "EI_EXPOSE_REP2" })
	public PrincipalComponentsAnalysis(double[][] input) {
		this.means = new double[input[0].length];
		double[][] cov = getCovariance(input, this.means);
		this.covMatrix = new Matrix(cov);
		this.eigenstuff = this.covMatrix.eig();
		this.eigenvalues = this.eigenstuff.getRealEigenvalues();
		this.eigenvectors = this.eigenstuff.getV();
		double[][] vecs = this.eigenvectors.getArray();
		int numComponents = this.eigenvectors.getColumnDimension(); // same as num rows.
		this.principleComponents = new TreeSet<PrincipleComponent>();
		for (int i = 0; i < numComponents; i++) {
			double[] eigenvector = new double[numComponents];
			for (int j = 0; j < numComponents; j++) {
				eigenvector[j] = vecs[i][j];
			}
			
			this.principleComponents.add(new PrincipleComponent(this.eigenvalues[i], eigenvector));
		}
	}

//	@edu.umd.cs.findbugs.annotations.SuppressWarnings({ "EI_EXPOSE_REP" })
	public double[] getMeans() {
		return this.means;
	}

	/**
	 * Subtracts the mean value from each column. The means must be precomputed, which you get for
	 * free when you make a PCA instance (just call getMeans()).
	 * 
	 * @param input
	 *          Some data, where each row is a sample point, and each column is a dimension.
	 * @param mean
	 *          Subtract the dimension's mean from each value.
	 * @return TODO describe return value
	 */
	public static double[][] getMeanAdjusted(double[][] input, double[] mean) {
		int nRows = input.length;
		int nCols = input[0].length;
		double[][] ret = new double[nRows][nCols];
		for (int row = 0; row < nRows; row++) {
			for (int col = 0; col < nCols; col++) {
				ret[row][col] = input[row][col] - mean[col];
				//System.out.println(input[row][col] + " - " + mean[col]);
			}
			//System.out.println("---");
		}
		return ret;
	}

	// from tapor code
	public static Matrix getCorrelationCoefficient(double[][] means) {
		int rows = means.length;
		int cols = means[0].length;

		Matrix coeMatrix = new Matrix(rows, rows);
		int i = 0;
		while (i < rows) {
			int k = i;
			while (k < rows) {
				int j = 0;
				double numerator = 0;
				double sx = 0;
				double sy = 0;
				while (j < cols) {
					numerator += means[i][j] * means[k][j];
					sx += means[i][j] * means[i][j];
					sy += means[k][j] * means[k][j];
					j++;
				}
				double tmp = numerator / Math.sqrt(sx * sy);
				coeMatrix.set(i, k, tmp);
				coeMatrix.set(k, i, tmp);
				k++;
			}
			i++;
		}
		return coeMatrix;
	}

	public List<PrincipleComponent> getDominantComponents(int n) {
		List<PrincipleComponent> ret = new ArrayList<PrincipleComponent>();
		int count = 0;
		for (PrincipleComponent pc : this.principleComponents) {
			ret.add(pc);
			count++;
			if (count >= n) {
				break;
			}
		}
		return ret;
	}

	public static Matrix getDominantComponentsMatrix(List<PrincipleComponent> dom) {
		int nRows = dom.get(0).eigenVector.length;
		int nCols = dom.size();
		Matrix matrix = new Matrix(nRows, nCols);
		for (int col = 0; col < nCols; col++) {
			for (int row = 0; row < nRows; row++) {
				matrix.set(row, col, dom.get(col).eigenVector[row]);
			}
		}
		return matrix;
	}

	public int getNumComponents() {
		return this.eigenvalues.length;
	}

	public static class PrincipleComponent implements Comparable<PrincipleComponent> {
		public double eigenValue;
		public double[] eigenVector;

//		@edu.umd.cs.findbugs.annotations.SuppressWarnings({ "EI_EXPOSE_REP2" })
		public PrincipleComponent(double eigenValue, double[] eigenVector) {
			this.eigenValue = eigenValue;
			this.eigenVector = eigenVector;
		}

		@Override
		public int compareTo(PrincipleComponent o) {
			int ret = 0;
			if (this.eigenValue > o.eigenValue) {
				ret = -1;
			} else if (this.eigenValue < o.eigenValue) {
				ret = 1;
			}
			if (ret == 0) {
				if (this.eigenVector.length > o.eigenVector.length) return -1;
				else if (this.eigenVector.length < o.eigenVector.length) return 1;
				else {
					boolean exact = true;
					for (int i = 0; i < this.eigenVector.length; i++) {
						if (this.eigenVector[i] != o.eigenVector[i]) {
							exact = false;
							break;
						}
					}
					if (exact) ret = 0;
					else ret = -1; // TODO how else to compare differing vectors?
				}
			}
			return ret;
		}
		
		@Override
		public boolean equals(Object obj) {
			
			if ((obj == null) || (obj instanceof PrincipleComponent == false)) return false;

			final PrincipleComponent other = (PrincipleComponent) obj;

			return this.compareTo(other) == 0;
			
		}
		
		@Override
		public int hashCode() {
		
			return (int) Math.round(this.eigenValue);
		
		}
	}

	public static double[][] getCovariance(double[][] input, double[] meanValues) {
		int numDataVectors = input.length;
		int n = input[0].length;

		// get the sum for each col
		double[] sum = new double[n];
		double[] mean = new double[n];
		for (int i = 0; i < numDataVectors; i++) {
			double[] vec = input[i];
			for (int j = 0; j < n; j++) {
				sum[j] = sum[j] + vec[j];
			}
		}
		for (int i = 0; i < sum.length; i++) {
			mean[i] = sum[i] / numDataVectors;
		}

		double[][] ret = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = i; j < n; j++) {
				double v = getCovariance(input, i, j, mean);
				ret[i][j] = v;
				ret[j][i] = v;
			}
		}
		if (meanValues != null) {
			System.arraycopy(mean, 0, meanValues, 0, mean.length);
		}
		return ret;
	}

	/**
	 * Gives covariance between vectors in an n-dimensional space. The two input arrays store values
	 * with the mean already subtracted. Read the code.
	 */
	private static double getCovariance(double[][] matrix, int colA, int colB, double[] mean) {
		//System.out.println("---");
		double sum = 0;
		for (int i = 0; i < matrix.length; i++) {
			double v1 = matrix[i][colA] - mean[colA];
			double v2 = matrix[i][colB] - mean[colB];
			//System.out.println(v1 + ", "+v2);
			sum = sum + (v1 * v2);
		}
		int n = matrix.length;
		double ret = (sum / (n - 1));
		return ret;
	}

	public SortedSet<PrincipleComponent> getPrincipleComponents() {
		return this.principleComponents;
	}

}
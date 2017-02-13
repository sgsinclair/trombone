package org.voyanttools.trombone.tool.algorithms.pca;


import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * @author Andrew MacDonald
 */
public class CorrespondenceAnalysis {

	public static final double EPS = 1.0e-8;
	public static final double SMALL = -1.0e10;
	public static final double MAXVAL = 1.0e12;

	private int numRows;
	private int numColumns;
	private double[] rowSums;
	private double[] columnSums;

	private double[][] inputMatrix;
	
	private double[][] rowProjections;
	private double[][] columnProjections;
	
	private double[] dimensionPercentages;

//	@edu.umd.cs.findbugs.annotations.SuppressWarnings({ "EI_EXPOSE_REP2" })
	public CorrespondenceAnalysis(double[][] inputMatrix) {
		this.inputMatrix = inputMatrix;
		
		this.numRows = inputMatrix.length;
		this.numColumns = inputMatrix[0].length;
		
		this.rowSums = new double[this.numRows];
		this.columnSums = new double[this.numColumns];
		
		this.rowProjections = new double[this.numRows][this.numColumns];
		this.columnProjections = new double[this.numColumns][this.numColumns];
		
		this.dimensionPercentages = new double[this.numColumns];
	}

	public void doAnalysis() {
		double total = 0.0; 

		for (int i = 0; i < this.numRows; i++) {
			this.rowSums[i] = 0.0;
			for (int j = 0; j < this.numColumns; j++) {
				this.rowSums[i] += this.inputMatrix[i][j];
				total += this.inputMatrix[i][j];
			}  
		}         
 
		for (int j = 0; j < this.numColumns; j++) {
			this.columnSums[j] = 0.0;
			for (int i = 0; i <this.numRows; i++) this.columnSums[j] += this.inputMatrix[i][j];
		}

		for (int i = 0; i < this.numRows; i++) this.rowSums[i] /= total;
		for (int j = 0; j < this.numColumns; j++) this.columnSums[j] /= total;
		for (int i = 0; i < this.numRows; i++) {
			for (int j = 0; j < this.numColumns; j++) this.inputMatrix[i][j] /= total;
		}

		double[] eigenValues = new double[this.numColumns];
		double[][] eigenVectors = new double[this.numColumns][this.numColumns];
		double[][] crossProducts = new double[this.numColumns][this.numColumns]; 
		double[] inertiaRates = new double[this.numColumns];

		diagonalization(crossProducts, eigenValues, eigenVectors, inertiaRates);

		projections(eigenVectors, eigenValues, crossProducts, this.rowProjections, this.columnProjections); 

	}

	private void diagonalization (double[][] CP, double[] Evals, double[][] Evex, double[] rate) {

		for (int j1 = 0; j1 < this.numColumns; j1++) {
			for (int j2 = 0; j2 < this.numColumns; j2++) {
				CP[j1][j2] = 0.0;
				for (int i = 0; i < this.numRows; i++) {
					double r = ( this.inputMatrix[i][j1] * this.inputMatrix[i][j2] ) / ( this.rowSums[i] * Math.sqrt(this.columnSums[j1]*this.columnSums[j2]) );
					CP[j1][j2] +=  r;
					if (Double.isNaN(CP[j1][j2])) {
						CP[j1][j2] = 0.0;
					}
				}
			}
		}

		Matrix cp = new Matrix(CP);

		// Eigen decomposition
		EigenvalueDecomposition evaldec = cp.eig();
		Matrix evecs = evaldec.getV();
		double[] evals = evaldec.getRealEigenvalues();

		// Trace is adjusted by a value 1.0 because always in CA, 
		// the first eigenvalue is trivially 1-valued.
		double trce = cp.trace() - 1.0;

		// evecs contains the cols. ordered right to left.
		// Evecs is the more natural order with cols. ordered left to right.
		// So to repeat: leftmost col. of Evecs is assoc'd. with largest Evals.
		// Evals and Evecs ordered from left to right.
		double tot = 0.0; 
		for (int j = 0; j < evals.length; j++)  tot += evals[j]; 

		// Reverse order of evals into Evals.
		for (int j = 0; j < this.numColumns; j++) Evals[j] = evals[this.numColumns - j - 1];

		// Reverse order of Matrix evecs into Matrix Evecs.
		double[][] tempold = evecs.getArray();
		for (int j1 = 0; j1 < this.numColumns; j1++) {
			for (int j2 = 0; j2 < this.numColumns; j2++) {
				if (this.columnSums[j1] == 0) { // don't divide by zero
					Evex[j1][j2] = 0.0;
				} else {
					Evex[j1][j2] = tempold[j1][this.numColumns - j2 - 1]/Math.sqrt(this.columnSums[j1]);
				}
					
			}
		}

//		double runningtotal = 0.0;
//		double[] percentevals = new double[this.numColumns];
//		// Low index in following = 1 to exclude first trivial eval.
//		percentevals[0] = 0.0; 
//		rate[0] = 0.0;
		for (int j = 1; j < Evals.length; j++) {
//			percentevals[j] = runningtotal + 100.0*Evals[j]/(tot-1.0);
//			rate[j] = Evals[j]/trce;
//			runningtotal = percentevals[j];
			this.dimensionPercentages[j-1] = 100.0*Evals[j]/trce;
		}

	}

	private void projections (double[][] Evex, double[] Evals, double[][] CP, double[][] rowproj, double[][] colproj) {

		// Projections on factors - row, and column
		// Row projections in new space, X U  Dims: (n x m) x (m x m)
		for (int i = 0; i < this.numRows; i++) {
			for (int j1 = 0; j1 < this.numColumns; j1++) {
				rowproj[i][j1] = 0.0; 
				for (int j2 = 0; j2 < this.numColumns; j2++) {
					rowproj[i][j1] += this.inputMatrix[i][j2] * Evex[j2][j1];
				}
				if (this.rowSums[i] >= EPS) rowproj[i][j1] /= this.rowSums[i];
				if (this.rowSums[i] < EPS) rowproj[i][j1] = 0.0;
				
				if (Double.isNaN(rowproj[i][j1])) {
					rowproj[i][j1] = 0.0;
				}
			}
		}
		for (int j1 = 0; j1 < this.numColumns; j1++) {
			for (int j2 = 0; j2 < this.numColumns; j2++) {
				colproj[j1][j2] = 0.0;
				for (int j3 = 0; j3 < this.numColumns; j3++) {
					colproj[j1][j2] += CP[j1][j3] * Evex[j3][j2] * Math.sqrt(this.columnSums[j3]);
				}
				if (this.columnSums[j1] >= EPS && Evals[j2] >= EPS) colproj[j1][j2] /= Math.sqrt(Evals[j2]*this.columnSums[j1]);
				if (this.columnSums[j1] < EPS && Evals[j2] < EPS) colproj[j1][j2] = 0.0; 
				
				if (Double.isNaN(colproj[j1][j2])) {
					colproj[j1][j2] = 0.0;
				}
			}
		}

	}

//	@edu.umd.cs.findbugs.annotations.SuppressWarnings({ "EI_EXPOSE_REP" })
	public double[][] getRowProjections() {
		return this.rowProjections;
	}

//	@edu.umd.cs.findbugs.annotations.SuppressWarnings({ "EI_EXPOSE_REP" })
	public double[][] getColumnProjections() {
		return this.columnProjections;
	}
	
//	@edu.umd.cs.findbugs.annotations.SuppressWarnings({ "EI_EXPOSE_REP" })
	public double[] getDimensionPercentages() {
		return this.dimensionPercentages;
	}

}
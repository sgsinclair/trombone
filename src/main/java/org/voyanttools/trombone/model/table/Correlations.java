/**
 * 
 */
package org.voyanttools.trombone.model.table;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;

/**
 * @author sgs
 *
 */
public class Correlations {
	
	public enum Implementation {
		pearson, spearman, kendall;
		public static Implementation getForgivingly(String value) {
			String v = value != null ? value.toLowerCase() : "";
			for (Implementation i : Implementation.values()) {
				if (v.equals(i.name())) return i;
			}
			return getDefault();
		}
		private static Implementation getDefault() {
			return pearson;
		}
	}

	private Table table;

	public Correlations(Table table) {
		this.table = table;
	}
	
	public double getCorrelation(String column1, String column2) {
		return getCorrelation(table.getColumnAsDoubles(column1), table.getColumnAsDoubles(column2), Implementation.getDefault());
	}
	
	public double getCorrelation(int column1, int column2) {
		return getCorrelation(table.getColumnAsDoubles(column1), table.getColumnAsDoubles(column2), Implementation.getDefault());
	}
	
	public double getCorrelation(String column1, String column2, Implementation implementation) {
		return getCorrelation(table.getColumnAsDoubles(column1), table.getColumnAsDoubles(column2), implementation);
	}
	
	public double getCorrelation(int column1, int column2, Implementation implementation) {
		return getCorrelation(table.getColumnAsDoubles(column1), table.getColumnAsDoubles(column2), implementation);
	}

	public double getCorrelation(double[] xArray, double[] yArray, Implementation implementation) {
		switch(implementation) {
		case spearman:
			SpearmansCorrelation spearmansCorrelation = new SpearmansCorrelation();
			return spearmansCorrelation.correlation(xArray, yArray);
		case kendall:
			KendallsCorrelation kendallsCorrelation = new KendallsCorrelation();
			return kendallsCorrelation.correlation(xArray, yArray);
		default:
			PearsonsCorrelation pearsonCorrelation = new PearsonsCorrelation();
			return pearsonCorrelation.correlation(xArray, yArray);
		}
	}
}

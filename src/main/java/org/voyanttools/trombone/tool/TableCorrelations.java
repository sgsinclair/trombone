/**
 * 
 */
package org.voyanttools.trombone.tool;

import java.io.IOException;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

import org.voyanttools.trombone.model.Table;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("correlations")
public class TableCorrelations extends AbstractTool {
	
	private enum Implementation {
		pearson, spearman, kendall;
		private static Implementation getForgivingly(String value) {
			String v = value.toLowerCase();
			for (Implementation i : Implementation.values()) {
				if (value.equals(i.name())) return i;
			}
			return pearson;
		}
	}
	
	private float correlation = 0;

	public TableCorrelations(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.tool.utils.RunnableTool#run()
	 */
	@Override
	public void run() throws IOException {
		TableManager tableManager = new TableManager(storage, parameters);
		tableManager.run();
		Table table = tableManager.getTable();
		if (table.getColumnsCount()<2) {
			throw new IllegalArgumentException("The table needs to contain at least two columns.");
		}
		String[] columns = parameters.getParameterValues("columns", new String[]{"0","1"});
		double[] x = table.getColumnAsDoubles(columns[0]);
		double[] y = table.getColumnAsDoubles(columns[1]);
		Implementation implementation = Implementation.getForgivingly(parameters.getParameterValue("implementation", "pearson"));
		switch(implementation) {
		case spearman:
			SpearmansCorrelation spearmansCorrelation = new SpearmansCorrelation();
			correlation = (float) spearmansCorrelation.correlation(x, y);
			break;
		// FIXME: when Math 3.3 is available, add Kendall's
//		case kendall:
//			KendallsCorrelation kendallsCorrelation = new KendallsCorrelation();
//			correlation = (float) kendallsCorrelation.correlation(x, y);
//			break;
		default:
			PearsonsCorrelation pearsonCorrelation = new PearsonsCorrelation();
			correlation = (float) pearsonCorrelation.correlation(x, y);
			break;
		}
		if (Float.isNaN(correlation)) {
			throw new IllegalArgumentException("Calculation of the correlation value resulted in a non-numeric result (NaN). One cause of this can be when there's no variation in values in a given column.");
		}
	}

	public float getCorrelation() {
		return correlation;
	}

}

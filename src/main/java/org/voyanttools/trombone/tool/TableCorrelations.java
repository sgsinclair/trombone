/**
 * 
 */
package org.voyanttools.trombone.tool;

import java.io.IOException;

import org.voyanttools.trombone.model.table.Correlations;
import org.voyanttools.trombone.model.table.Table;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("correlations")
public class TableCorrelations extends AbstractTool {
	
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
		Correlations.Implementation implementation = Correlations.Implementation.getForgivingly(parameters.getParameterValue("implementation", ""));
		Correlations correlations = new Correlations(table);
		correlation = (float) correlations.getCorrelation(columns[0], columns[1], implementation);
		if (Float.isNaN(correlation)) {
			throw new IllegalArgumentException("Calculation of the correlation value resulted in a non-numeric result (NaN). One cause of this can be when there's no variation in values in a given column.");
		}
	}

	public float getCorrelation() {
		return correlation;
	}

}

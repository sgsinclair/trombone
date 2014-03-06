/**
 * 
 */
package org.voyanttools.trombone.tool;

import java.io.IOException;
import java.util.UUID;

import org.voyanttools.trombone.model.Table;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class TableManager extends AbstractTool {
	
	private String id = "";
	
	private Table table = null;

	public TableManager(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run() throws IOException {
		if (this.parameters.containsKey("verifyTableId")) {
			String id = this.parameters.getParameterValue("verifyTableId");
			if (this.storage.isStored(Table.getSerializedId(id))) {
				this.id = id;
			}
		}
		else if (this.parameters.containsKey("storeTable")) {
			Table table = new Table(this.parameters.getParameterValue("storeTable"),
					Table.Format.getForgivingly(this.parameters.getParameterValue("inputFormat", "tsv")),
					this.parameters.getParameterBooleanValue("columnHeaders"),
					this.parameters.getParameterBooleanValue("rowHeaders"));
			
			this.id = this.parameters.containsKey("storeTableId") ? this.parameters.getParameterValue("storeTableId") : UUID.randomUUID().toString();
			this.storage.store(table, Table.getSerializedId(id));
		}
		else if (this.parameters.containsKey("retrieveTableId")) {
			String id = this.parameters.getParameterValue("retrieveTableId");
			try {
				this.table = (Table) this.storage.retrieve(Table.getSerializedId(id));
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
			this.id = id; // do this afterwards in case there's a problem
		}
	}

	public String getTableId() {
		return this.id;
	}

	public Table getTable() {
		return table;
	}

}

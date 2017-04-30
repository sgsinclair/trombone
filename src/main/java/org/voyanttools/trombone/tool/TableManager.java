/**
 * 
 */
package org.voyanttools.trombone.tool;

import java.io.IOException;
import java.util.UUID;

import org.voyanttools.trombone.model.table.Table;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.utils.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("table")
public class TableManager extends AbstractTool {
	
	private String id = "";
	
	private Table table = null;

	public TableManager(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run() throws IOException {
		if (this.parameters.containsKey("input")) {
			this.table = new Table(this.parameters.getParameterValue("input"),
					Table.Format.getForgivingly(this.parameters.getParameterValue("inputFormat", "tsv")),
					this.parameters.getParameterBooleanValue("columnHeaders"),
					this.parameters.getParameterBooleanValue("rowHeaders"));
			// this doesn't work since table is defined in the if above
			this.id = this.parameters.containsKey("table") ? this.parameters.getParameterValue("table") : UUID.randomUUID().toString();
			this.storage.store(table, Table.getSerializedId(id), Storage.Location.object);
		}
		else if (this.parameters.containsKey("table")) {
			String id = this.parameters.getParameterValue("table");
			if (this.storage.isStored(Table.getSerializedId(id), Storage.Location.object)) {
				this.id = id;
				try {
					this.table = (Table) this.storage.retrieve(Table.getSerializedId(this.id), Storage.Location.object);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
			else if (this.parameters.getParameterBooleanValue("verify")) {
				this.id = "";
			}
		}
		if (this.id==null) {
			throw new IllegalArgumentException("No table ID and no table data provided.");
		}
//		else if (this.parameters.containsKey("retrieveTableId")) {
//			String id = this.parameters.getParameterValue("retrieveTableId");
//			try {
//				this.table = (Table) this.storage.retrieve(Table.getSerializedId(id));
//			} catch (ClassNotFoundException e) {
//				throw new RuntimeException(e);
//			}
//			this.id = id; // do this afterwards in case there's a problem
//		}
	}

	public String getTableId() {
		return this.id;
	}

	public Table getTable() {
		return table;
	}

}

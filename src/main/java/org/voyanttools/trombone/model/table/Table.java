/**
 * 
 */
package org.voyanttools.trombone.model.table;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import cc.mallet.util.ArrayUtils;

/**
 * @author sgs
 *
 */
public class Table implements Serializable {
	
	public enum Format {
		tsv;
		public static Format getForgivingly(String format) {
			String formatlower = format.toLowerCase();
			for (Format f : Format.values()) {
				if (f.name().equals(formatlower)) {return f;}
			}
			return tsv;
		}
	};
	
	String[][] values = new String[0][0];

	Map<String, Integer> columnsMap = new HashMap<String, Integer>();
	
	Map<String, Integer> rowsMap = new HashMap<String, Integer>();
	
	/**
	 * 
	 */
	public static final long serialVersionUID = 8047193666708200784L;
	
	public Table(String string, Format format, boolean columnHeaders, boolean rowHeaders) {
		String[] lines = string.split("\n");
		for (int i=0; i<lines.length; i++) {
			String[] cols = lines[i].split("\t");
			if (i==0) {
				// this assumes that the first row has the maximum number of columns filled
				values = new String[columnHeaders ? lines.length-1 : lines.length][rowHeaders ? cols.length-1 : cols.length];
				if (columnHeaders) {
					for (int j=rowHeaders ? 1 : 0; j<cols.length; j++) {
						columnsMap.put(cols[j], j);
					}
				}
			}
			if (i>0 || (i==0 && !columnHeaders)) {
				if (rowHeaders) {
					rowsMap.put(cols[0], columnHeaders ? i-1 : i);
				}
				for (int j=rowHeaders ? 1 : 0; j<cols.length; j++) {
					values[columnHeaders ? i-1 : i][rowHeaders ? j-1 : j] = cols[j];
				}
			}
		}
	}
	
	public static String getSerializedId(String id) {
		return Table.class.toString()+"-"+Table.serialVersionUID+"-"+id;
	}
	
	

	public String toTsv() {
		StringBuilder tsv = new StringBuilder();
		if (!columnsMap.isEmpty()) {
			String[] cols = new String[columnsMap.size()];
			for (Map.Entry<String, Integer> entry : columnsMap.entrySet()) {
				cols[entry.getValue()] = entry.getKey();
			}
			if (!rowsMap.isEmpty()) {
				tsv.append("\t");
			}
			tsv.append(StringUtils.join(cols, "\t")).append("\n");
		}
		for (String[] vals : values) {
			if (!rowsMap.isEmpty()) {
				tsv.append("\t");
			}
			tsv.append(StringUtils.join(vals,"\t")).append("\n");
		}
		return tsv.toString().substring(0, tsv.length()-1);
	}

	public String[] getColumn(String column) {
		return getColumn(StringUtils.isNumeric(column) ? Integer.parseInt(column) : columnsMap.get(column));
	}
	
	public String[] getColumn(int column) {
		String[] vals = new String[values.length];
		for (int i=0; i<vals.length; i++) {
			vals[i] = values[i][column];
		}
		return vals;
	}
	
	public double[] getColumnAsDoubles(String column) {
		return Arrays.stream(getColumn(column))
			.mapToDouble(Double::valueOf)
			.toArray();
	}

	public double[] getColumnAsDoubles(int column) {
		return Arrays.stream(getColumn(column))
				.mapToDouble(Double::valueOf)
				.toArray();
	}
	
	public int getColumnsCount() {
		return values[0].length;
	}
	
//	public double[] getColumnAsDoubles(int index) {
//		
//	}
//
//	public List<Object> getColumn(String key) {
//		
//	}

}

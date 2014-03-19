/**
 * 
 */
package org.voyanttools.trombone.model;

import static org.junit.Assert.*;

import org.junit.Test;
import org.voyanttools.trombone.model.table.Table;

/**
 * @author sgs
 *
 */
public class TableTest {

	@Test
	public void test() {
		Table table;
		
		table = new Table("zero	one	two	three\na	0	1	2\nb	0	2	1", Table.Format.tsv, true, true);
	}

}

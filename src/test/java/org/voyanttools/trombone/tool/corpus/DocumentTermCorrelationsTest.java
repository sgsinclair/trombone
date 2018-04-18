package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.*;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.junit.Test;

public class DocumentTermCorrelationsTest {

	@Test
	public void test() {
		//fail("Not yet implemented");
	}
	
	@Test
	public void simpleTest() {
		// this is to check that we get results similar to
		// http://support.minitab.com/en-us/minitab-express/1/help-and-how-to/modeling-statistics/regression/how-to/correlation/methods-and-formulas/#spearman-s-correlation-coefficient
		double[] a = new double[]{4,6,3,5,1.5,1.5};
		double[] b = new double[]{1,3,3,3,6,5};
		SimpleRegression regression = new SimpleRegression();
		for (int i=0, len=a.length; i<len; i++) {
			regression.addData(a[i], b[i]);
		}
		assertEquals(-0.678, regression.getR(), .001);
		assertEquals(.139, regression.getSignificance(), .001);
	}

}

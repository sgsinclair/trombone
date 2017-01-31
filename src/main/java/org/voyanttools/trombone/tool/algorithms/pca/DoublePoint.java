package org.voyanttools.trombone.tool.algorithms.pca;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.stat.clustering.Clusterable;
import org.voyanttools.trombone.model.RawAnalysisTerm;
import org.voyanttools.trombone.tool.corpus.AnalysisTool;

/**
 * @author Andrew MacDonald
 */
public class DoublePoint implements Clusterable<DoublePoint> {

	private RawAnalysisTerm term;
	private double[] point;
	
	public DoublePoint(RawAnalysisTerm term) {
		this.term = term;
		this.point = term.getVector();
	}
	
	public double distanceFrom(DoublePoint p) {
		return AnalysisTool.getDistance(this.point, p.point);
	}

	public DoublePoint centroidOf(Collection<DoublePoint> cluster) {
        List<DoublePoint> instances = new ArrayList<DoublePoint>(cluster);
        double[] sumDistance = new double[instances.size()];

        for (int i = 0; i < instances.size(); i++) {
        	DoublePoint i1 = instances.get(i);
            for (int j = i + 1; j < instances.size(); j++) {
            	DoublePoint i2 = instances.get(j);
                
            	double d = AnalysisTool.getDistance(i1.point, i2.point);
                sumDistance[i] += d;
                sumDistance[j] += d;
            }
        }

        int index = 0;
        double minDistance = (1.0D / 0.0D);
        for (int i = 0; i < instances.size(); i++) {
                if (sumDistance[i] < minDistance) {
                        index = i;
                        minDistance = sumDistance[i];
                }
        }
        return instances.get(index);
	}
	
	public RawAnalysisTerm getTerm() {
		return this.term;
	}

}

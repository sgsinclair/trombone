/*******************************************************************************
 * Trombone is a flexible text processing and analysis library used
 * primarily by Voyant Tools (voyant-tools.org).
 * 
 * Copyright (©) 2007-2012 Stéfan Sinclair & Geoffrey Rockwell
 * 
 * This file is part of Trombone.
 * 
 * Trombone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.tool.analysis;


import java.text.Normalizer;
import java.util.Comparator;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;


/**
 * @author sgs
 *
 */
public class DistributedTermFrequencies implements Comparable<DistributedTermFrequencies> {
	private String string;
	private int[] freqs;
	private int totalRawFrequency;
	private String normalizedString; // for better sorting
	private DescriptiveStatistics stats = null;
	public DistributedTermFrequencies(String string, int bins) {
		this(string, new int[bins]);
	}
	public DistributedTermFrequencies(String string, int[] freqs) {
		this.string = string;
		this.freqs = new int[freqs.length];
		for (int i=0, len=freqs.length; i<len; i++) {add(i,freqs[i]);}
		this.normalizedString = Normalizer.normalize(string.toLowerCase(), Normalizer.Form.NFD);
	}
	
	public void add(int bin, int freq) {
		freqs[bin] = freq;
		this.totalRawFrequency += freq;
	}
	
	@Override
	public int compareTo(DistributedTermFrequencies other) {
		return this.normalizedString.compareTo(other.normalizedString);
	}

	public String getString() {
		return this.string;
	}

	public int getRawFrequency() {
		return this.totalRawFrequency;
	}
	
	public double getMean() {
		if (this.stats==null) {buildStats();}
		return stats.getMean();
	}
	
	public double getSkewness() {
		if (this.stats==null) {buildStats();}
		return stats.getSkewness();
	}
	public double getKurtosis() {
		if (this.stats==null) {buildStats();}
		return stats.getKurtosis();
	}

	public double getStandardDeviation() {
		if (this.stats==null) {buildStats();}
		return stats.getStandardDeviation();
	}
	
	@Override
	public String toString() {
		return string+" ("+totalRawFrequency+"): "+stats;
	}
	
	private void buildStats() {
		stats = new DescriptiveStatistics(freqs.length);
		for (int i : freqs) {stats.addValue(i);}
	}
	
	public static class DistributedTermFrequenciesTotalFrequencyComparator implements Comparator<DistributedTermFrequencies> {

		@Override
		public int compare(DistributedTermFrequencies arg0,
				DistributedTermFrequencies arg1) {
			if (arg0.totalRawFrequency==arg1.totalRawFrequency) {
				return arg0.compareTo(arg1);
			}
			else {
				return arg0.totalRawFrequency > arg1.totalRawFrequency ? -1 : 1;
			}
		}
		
	}
	

	public static class DistributedTermFrequenciesDescriptiveStatsComparator implements Comparator<DistributedTermFrequencies> {

		public enum STATS {MEAN, SKEWNESS, KURTOSIS, STANDARDDEVIATION};
		
		private Comparator<DistributedTermFrequencies> statsComparator;
		public DistributedTermFrequenciesDescriptiveStatsComparator(STATS stat) {
			switch(stat) {
				case MEAN:
					statsComparator = new DistributedTermFrequenciesMeanComparator(); break;
				case SKEWNESS:
					statsComparator = new DistributedTermFrequenciesMeanComparator(); break;
				case KURTOSIS:
					statsComparator = new DistributedTermFrequenciesMeanComparator(); break;
				case STANDARDDEVIATION:
					statsComparator = new DistributedTermFrequenciesMeanComparator(); break;
			}
		}
		
		@Override
		public int compare(DistributedTermFrequencies arg0,
				DistributedTermFrequencies arg1) {
			return this.statsComparator.compare(arg0,arg1);
		}
		
	}
	
	public static class DistributedTermFrequenciesMeanComparator implements Comparator<DistributedTermFrequencies> {

		@Override
		public int compare(DistributedTermFrequencies arg0,
				DistributedTermFrequencies arg1) {
			double a = arg0.getMean();
			double b = arg1.getMean();
			if (a==b) {
				return arg0.compareTo(arg1);
			}
			else {return a > b ? -1 : 1;}
		}
		
	}

	public static class DistributedTermFrequenciesKurtosisComparator implements Comparator<DistributedTermFrequencies> {

		@Override
		public int compare(DistributedTermFrequencies arg0,
				DistributedTermFrequencies arg1) {
			double a = arg0.getKurtosis();
			double b = arg1.getKurtosis();
			if (a==b) {
				return arg0.compareTo(arg1);
			}
			else {return a > b ? -1 : 1;}
		}
		
	}

	public static class DistributedTermFrequenciesSkewnessComparator implements Comparator<DistributedTermFrequencies> {

		@Override
		public int compare(DistributedTermFrequencies arg0,
				DistributedTermFrequencies arg1) {
			double a = arg0.getSkewness();
			double b = arg1.getSkewness();
			if (a==b) {
				return arg0.compareTo(arg1);
			}
			else {return a > b ? -1 : 1;}
		}
		
	}

	public static class DistributedTermFrequenciesStandardDeviationComparator implements Comparator<DistributedTermFrequencies> {

		@Override
		public int compare(DistributedTermFrequencies arg0,
				DistributedTermFrequencies arg1) {
			double a = arg0.getStandardDeviation();
			double b = arg1.getStandardDeviation();
			if (a==b) {
				return arg0.compareTo(arg1);
			}
			else {return a > b ? -1 : 1;}
		}
		
	}
}

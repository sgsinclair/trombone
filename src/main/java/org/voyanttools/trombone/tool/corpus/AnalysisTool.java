package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.stat.clustering.Cluster;
import org.apache.commons.math3.stat.clustering.KMeansPlusPlusClusterer;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.DocumentTerm;
import org.voyanttools.trombone.model.RawAnalysisType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.algorithms.pca.DoublePoint;
import org.voyanttools.trombone.tool.corpus.AbstractCorpusTool;
import org.voyanttools.trombone.tool.corpus.CorpusTerms;
import org.voyanttools.trombone.tool.corpus.DocumentTerms;
import org.voyanttools.trombone.util.FlexibleParameters;

public abstract class AnalysisTool extends AbstractCorpusTool {

	final static int CORPUS = 0;
	final static int DOCUMENT = 1;
	
	private List<?> typesList;
	
	protected int bins;
	protected int maxOutputDataItemCount;
	
	public AnalysisTool(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		
		bins = parameters.getParameterIntValue("bins", 50);
	}
	
	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		runAnalysis(corpusMapper);
	}

	protected abstract void runAnalysis(CorpusMapper corpusMapper) throws IOException;
	
	@SuppressWarnings("unchecked")
	protected double[][] buildFrequencyMatrix(CorpusMapper corpusMapper, int type) throws IOException {
		int i, j;
		double[][] freqMatrix = null;
		switch(type) {
			case CORPUS: {
				this.typesList = this.getCorpusTypes(corpusMapper);
	
				this.maxOutputDataItemCount = this.typesList.size();
				int numDocs = corpusMapper.getCorpus().size();
	
				freqMatrix = new double[this.maxOutputDataItemCount][numDocs];
	
				Iterator<CorpusTerm> it = (Iterator<CorpusTerm>) this.typesList.iterator();
				i = 0;
				j = 0;
				while (it.hasNext()) {
					CorpusTerm corpusType = it.next();
	
					int[] freqs = corpusType.getRawDistributions();
					for (j = 0; j < freqs.length; j++) {
						freqMatrix[i][j] = freqs[j];
					}
					i++;
				}
				break;
			} case DOCUMENT: {
				this.typesList = this.getDocumentTypes(corpusMapper);
	
				this.maxOutputDataItemCount = this.typesList.size();
				
				freqMatrix = new double[this.maxOutputDataItemCount][bins];
				
				Iterator<DocumentTerm> it = (Iterator<DocumentTerm>) this.typesList.iterator();
				i = 0;
				j = 0;
				while (it.hasNext()) {
					DocumentTerm docType = it.next();
					//int[] freqs = docType.getDocumentType().getDistributionFreqs(); // obsolete API
					int[] freqs = docType.getRawDistributions(bins);// getRawStats().getValues(); // new API introduced towards #322
					for (j = 0; j < freqs.length; j++) {
						freqMatrix[i][j] = freqs[j];
					}
					i++;
				}
				break;
			}
		}
		
		return freqMatrix;
	}
	
	@SuppressWarnings("unchecked")
	private List<CorpusTerm> getCorpusTypes(CorpusMapper corpusMapper) throws IOException {
		CorpusTerms ct = new CorpusTerms(storage, parameters);
		ct.run(corpusMapper);
		
		Iterator<CorpusTerm> it = ct.iterator();
		List<CorpusTerm> list = new ArrayList<CorpusTerm>();
		while (it.hasNext()) {
			CorpusTerm term = it.next();
			if (term.getRawFreq() > 0) {
				list.add(term);
			}
		}
		
		return list;
		
//		CorpusTypeFrequencies ctf = new CorpusTypeFrequencies(this.toolContext);
//		ctf.invoke();
//		List<CorpusTypePair> corpusTypes = (List<CorpusTypePair>) ctf.getOutputData().getOutputDataPage();
//		Iterator<CorpusTypePair> it = corpusTypes.iterator();
//		while (it.hasNext()) {
//			CorpusTypePair type = it.next();
//			if (type.getBaseCorpusType().getRawFreq() == 0) {
//				it.remove();
//			}
//		}
//		return corpusTypes;
	}
	
	@SuppressWarnings("unchecked")
	private List<DocumentTerm> getDocumentTypes(CorpusMapper corpusMapper) throws IOException {
		DocumentTerms dt = new DocumentTerms(storage, parameters);
		dt.run(corpusMapper);
		
		Iterator<DocumentTerm> it = dt.iterator();
		List<DocumentTerm> list = new ArrayList<DocumentTerm>();
		while (it.hasNext()) {
			DocumentTerm term = it.next();
			if (term.getRawFrequency() > 0) {
				list.add(term);
			}
		}
		
		return list;
		
//		DocumentTypeFrequencies dtf = new DocumentTypeFrequencies(this.toolContext);
//		dtf.invoke();
//		List<CorpusAwareDocumentType> documentTypes = (List<CorpusAwareDocumentType>) dtf.getOutputData().getOutputDataPage();
//		Iterator<CorpusAwareDocumentType> it = documentTypes.iterator();
//		while (it.hasNext()) {
//			CorpusAwareDocumentType type = it.next();
//			if (type.getDocumentType().getRawFreq() == 0) {
//				it.remove();
//			}
//		}
//		return documentTypes;
	}
	
	@SuppressWarnings("unchecked")
	protected static void filterTypesByTarget(List<? extends RawAnalysisType> types, double[] target, double maxDistance, List<String> whitelist) {
		Iterator<RawAnalysisType> it = (Iterator<RawAnalysisType>) types.iterator();
		while (it.hasNext()) {
			RawAnalysisType type = it.next();
			double distance = getDistance(type.getVector(), target);
			if (!whitelist.contains(type.getType())) {
				if (distance > maxDistance) {
					it.remove();
				}
			}
		}
	}
	
	public static double getDistance(double[] p1, double[] p2) {
		if (p1 == null || p2 == null) {
			return Double.MAX_VALUE;
		} else {
			return Math.sqrt(Math.pow(p2[0] - p1[0], 2) + Math.pow(p2[1] - p1[1], 2));
		}
	}
	
	protected static double[][] getMinMax(double[][] input) {
		double[][] minMax = new double[2][2];
		// min xy
		minMax[0][0] = Double.MAX_VALUE;
		minMax[0][1] = Double.MAX_VALUE;
		// max xy
		minMax[1][0] = Double.MIN_VALUE;
		minMax[1][1] = Double.MIN_VALUE;
		
		int n = input.length;
		for (int i = 0; i < n; i++) {
			double[] testVal = input[i];
			if (testVal[0] < minMax[0][0]) minMax[0][0] = testVal[0];
			if (testVal[1] < minMax[0][1]) minMax[0][1] = testVal[1];
			if (testVal[0] > minMax[1][0]) minMax[1][0] = testVal[0];
			if (testVal[1] > minMax[1][1]) minMax[1][1] = testVal[1];
		}

		return minMax;
	}
	
	protected static void clusterPoints(List<? extends RawAnalysisType> types, int k) {
		Collection<DoublePoint> data = new ArrayList<DoublePoint>();
		for (RawAnalysisType type : types) {
			data.add(new DoublePoint(type));
		}
		
		KMeansPlusPlusClusterer<DoublePoint> clusterer = new KMeansPlusPlusClusterer<DoublePoint>(new Random());
		List<Cluster<DoublePoint>> clusters = clusterer.cluster(data, k, 5000);
		int clusterCounter = 0;
		for (Cluster<DoublePoint> cluster : clusters) {
			List<DoublePoint> points = cluster.getPoints();
			DoublePoint center = cluster.getCenter();
			for (DoublePoint p : points) {
				p.getType().setCluster(clusterCounter);
				if (p.equals(center)) p.getType().setClusterCenter(true);
			}
			clusterCounter++;
		}
	}
	
	public List<?> getTypesList() {
		return this.typesList;
	}
}

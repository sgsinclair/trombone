package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.stat.clustering.Cluster;
import org.apache.commons.math3.stat.clustering.KMeansPlusPlusClusterer;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.RawAnalysisType;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.algorithms.pca.DoublePoint;
import org.voyanttools.trombone.util.FlexibleParameters;

public abstract class AnalysisTool extends AbstractCorpusTool {

	// what frequency stat to use for comparison
	enum ComparisonType {
		RAW, RELATIVE, TFIDF
	}
	protected ComparisonType comparisonType = ComparisonType.RELATIVE;
	
	// how to divide up the corpus
	enum DivisionType {
		DOCS, BINS
	}
	protected DivisionType divisionType = DivisionType.DOCS; 
	
	// what feature to use for rows in the frequency matrix
	enum MatrixType {
		TERM, DOCUMENT
	}
	
	private List<CorpusTerm> termsList;
	
	protected String target;
	protected int clusters;
	protected String[] docId;
	protected int bins;
	protected int dimensions;
	
	public AnalysisTool(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		
		target = parameters.getParameterValue("target");
		clusters = parameters.getParameterIntValue("clusters");
		docId = parameters.getParameterValues("docId");
		bins = parameters.getParameterIntValue("bins", 10);
		dimensions = parameters.getParameterIntValue("dimensions", 2);
		
		String compType = parameters.getParameterValue("comparisonType", "");
		if (compType.toUpperCase().equals("TFIDF")) {
			comparisonType = ComparisonType.TFIDF;
		} else if (compType.toUpperCase().equals("RAW")) {
			comparisonType = ComparisonType.RAW;
		} else {
			comparisonType = ComparisonType.RELATIVE;
		}
	}
	
	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		runAnalysis(corpusMapper);
	}

	protected abstract void runAnalysis(CorpusMapper corpusMapper) throws IOException;
	
	/**
	 * Returns a doc/terms frequency matrix for use in further analysis.
	 * @param corpusMapper
	 * @param type Determines row value in matrix: MatrixType.DOCUMENT or MatrixType.TERM
	 * @param minDims The minimum number of dimensions required for analysis
	 * @return
	 * @throws IOException
	 */
	protected double[][] buildFrequencyMatrix(CorpusMapper corpusMapper, MatrixType type, int minDims) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		
		List<String> ids = this.getCorpusStoredDocumentIdsFromParameters(corpus);
		List<Integer> indexes = new ArrayList<Integer>();
		for (String id : ids) {
			int index = corpus.getDocumentPosition(id);
			indexes.add(index);
		}
		int numDocs = ids.size();
		
		Map<String, float[]> termDistributionsMap = new HashMap<String, float[]>();
		// if there are enough docs, get document terms
		if (numDocs >= minDims) {
			divisionType = DivisionType.DOCS;
			
			termsList = this.getCorpusTerms(corpusMapper, corpus.size());
			for (CorpusTerm ct : termsList) {
				String term = ct.getTerm();
				if (comparisonType == ComparisonType.RAW) {
					int[] rawDist = ct.getRawDistributions();
					float[] rawDistFloat = new float[rawDist.length];
					for (int i = 0; i < rawDist.length; i++) {
						rawDistFloat[i] = (float) rawDist[i];
					}
					termDistributionsMap.put(term, rawDistFloat);
				} else if (comparisonType == ComparisonType.TFIDF) {
					int[] rawDist = ct.getRawDistributions();
					float[] tfidfDist = new float[rawDist.length];
					for (int i = 0; i < rawDist.length; i++) {
						int rawFreq = rawDist[i];
						int totalTermsCount = corpus.getDocument(i).getMetadata().getTokensCount(TokenType.lexical);
						int inDocuments = ct.getInDocumentsCount();
						float tfidf = ((float) rawFreq / (float) totalTermsCount) * (float) Math.log10((float) corpus.size() / (float) inDocuments);
						tfidfDist[i] = tfidf;
					}
					termDistributionsMap.put(term, tfidfDist);
				} else {
					termDistributionsMap.put(term, ct.getRelativeDistributions());
				}
			
			}	
		// if there aren't enough docs, get corpus terms split into bins
		} else {
			divisionType = DivisionType.BINS;
			numDocs = bins; // bins are the new docs
			
			termsList = this.getCorpusTerms(corpusMapper, bins);
						
			for (CorpusTerm ct : termsList) {
				String term = ct.getTerm();
				
				if (comparisonType == ComparisonType.RAW) {
					int[] rawDist = ct.getRawDistributions();
					float[] rawDistFloat = new float[rawDist.length];
					for (int i = 0; i < rawDist.length; i++) {
						rawDistFloat[i] = (float) rawDist[i];
					}
					termDistributionsMap.put(term, rawDistFloat);
//				} else if (comparisonType == ComparisonType.TFIDF) {
//					int[] rawDist = ct.getRawDistributions();
//					float[] tfidfDist = new float[rawDist.length];
//					for (int i = 0; i < rawDist.length; i++) {
//						int rawFreq = rawDist[i];
//						int totalTermsCount = corpus.getDocument(i).getMetadata().getTokensCount(TokenType.lexical);
//						int inDocuments = ct.getInDocumentsCount();
//						float tfidf = ((float) rawFreq / (float) totalTermsCount) * (float) Math.log10((float) corpus.size() / (float) inDocuments);
//						tfidfDist[i] = tfidf;
//					}
//					termDistributionsMap.put(term, tfidfDist);
				} else {
					termDistributionsMap.put(term, ct.getRelativeDistributions());
				}
			}
		}
		
		Set<Entry<String, float[]>> entrySet = termDistributionsMap.entrySet();
		int numTerms = entrySet.size();
		
		double[][] freqMatrix;
		if (type == MatrixType.DOCUMENT) {
			freqMatrix = new double[numDocs][numTerms];
			
			int termIndex = 0;
			for (Entry<String, float[]> entry : entrySet) {
				float[] values = entry.getValue();
				
				for (int valIndex = 0; valIndex < values.length; valIndex++) {
					int docIndex;
					if (divisionType == DivisionType.BINS) {
						docIndex = valIndex;
					} else {
						docIndex = indexes.indexOf(valIndex);
					}
					
					if (docIndex != -1) {
						freqMatrix[docIndex][termIndex] = values[valIndex];
					}
				}
				termIndex++;
			}
		
		} else {
			freqMatrix = new double[numTerms][numDocs];
			
			int termIndex = 0;
			for (Entry<String, float[]> entry : entrySet) {
				float[] values = entry.getValue();
				for (int valIndex = 0; valIndex < values.length; valIndex++) {
					int docIndex;
					if (divisionType == DivisionType.BINS) {
						docIndex = valIndex;
					} else {
						docIndex = indexes.indexOf(valIndex);
					}
					
					if (docIndex != -1) {
						freqMatrix[termIndex][docIndex] = values[valIndex];
					}
				}
				termIndex++;
			}
			
		}
		
		return freqMatrix;
	}
	
	protected List<CorpusTerm> getCorpusTerms(CorpusMapper corpusMapper, int bins) throws IOException {
		FlexibleParameters params = parameters.clone();
		params.setParameter("bins", bins);
		params.setParameter("withDistributions", "true");
		
		CorpusTerms ct = new CorpusTerms(storage, params);
		ct.run(corpusMapper);
		
		Iterator<CorpusTerm> it = ct.iterator();
		List<CorpusTerm> list = new ArrayList<CorpusTerm>();
		while (it.hasNext()) {
			CorpusTerm term = it.next();
			if (term.getRawFrequency() > 0) {
				list.add(term);
			}
		}
		
		return list;
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
	
	public List<CorpusTerm> getTermsList() {
		return this.termsList;
	}
}

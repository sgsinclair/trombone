package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.DocumentTerm;
import org.voyanttools.trombone.model.RawAnalysisTerm;
import org.voyanttools.trombone.model.RawCATerm;
import org.voyanttools.trombone.model.RawPCATerm;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.model.RawCATerm.CategoryType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.analysis.AnalysisUtils;
import org.voyanttools.trombone.util.FlexibleParameters;

public abstract class CorpusAnalysisTool extends AbstractCorpusTool {

	// what frequency stat to use for comparison
	enum ComparisonType {
		RAW, RELATIVE, TFIDF
	}
	
	// how to divide up the corpus
	enum DivisionType {
		DOCS, BINS
	}
	
	// what feature to use for rows in the frequency matrix
	enum MatrixType {
		TERM, DOCUMENT
	}
	
	private ComparisonType comparisonType = ComparisonType.RELATIVE;
	protected DivisionType divisionType = DivisionType.DOCS; 
	
	protected String target;
	private int clusters;
	protected int dimensions;
	protected int bins;
	
	protected double[] targetVector;
	
	protected List<RawCATerm> analysisTerms;
	
	public CorpusAnalysisTool(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		
		target = parameters.getParameterValue("target");
		clusters = parameters.getParameterIntValue("clusters");
		dimensions = parameters.getParameterIntValue("dimensions", 2);
		bins = parameters.getParameterIntValue("bins", 10);
		
		targetVector = null;
		
		analysisTerms = new ArrayList<RawCATerm>();
		
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
		double[][] result = runAnalysis(corpusMapper);
		
		if (target != null && targetVector != null) {
			double[][] minMax = AnalysisUtils.getMinMax(result);
			double distance = AnalysisUtils.getDistance(minMax[0], minMax[1]) / 50;
			List<String> initialTerms = new ArrayList<String>(Arrays.asList(this.parameters.getParameterValues("term")));
			AnalysisUtils.filterTermsByTarget(analysisTerms, targetVector, distance, initialTerms);
		}
		
		if (clusters > 0) {
			AnalysisUtils.clusterPoints(analysisTerms, clusters);
		}
	}

	protected abstract double[][] runAnalysis(CorpusMapper corpusMapper) throws IOException;
	
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
		
		double[][] freqMatrix;
		
		Map<String, float[]> termDistributionsMap = new HashMap<String, float[]>();
		// if there are enough docs, get document terms
		
		int[] tokenCounts = corpusMapper.getCorpus().getTokensCounts(TokenType.lexical); 
		
		if (numDocs >= minDims) {
			divisionType = DivisionType.DOCS;
			
			FlexibleParameters params = parameters.clone();
			params.setParameter("bins", corpus.size());
			params.setParameter("withDistributions", "true");
			params.setParameter("minRawFreq", 1);
			params.setParameter("sort", "rawFreq");
			params.setParameter("dir", "DESC");
			CorpusTerms termsList = new CorpusTerms(storage, params);
			termsList.run(corpusMapper);

			for (CorpusTerm ct : termsList) {
				String term = ct.getTerm();
				analysisTerms.add(new RawCATerm(term, ct.getRawFrequency(), ct.getRelativeFrequency(), CategoryType.TERM));
				
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
						int inDocuments = ct.getInDocumentsCount();
						float tfidf = ((float) rawFreq / (float) tokenCounts[i]) * (float) Math.log10((float) corpus.size() / (float) inDocuments);
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
			
			List<String> docIds = this.getCorpusStoredDocumentIdsFromParameters(corpusMapper.getCorpus());
			int binsPerDoc = bins / docIds.size();
			bins = binsPerDoc * docIds.size(); // re-set bins to account for potential rounding
			numDocs = bins; // bins are the new docs
			
			// get the top terms
			FlexibleParameters params = parameters.clone();
			params.setParameter("withDistributions", "false");
			CorpusTerms cts = new CorpusTerms(storage, params);
			cts.run(corpusMapper);
			
			// get the distributions for the top terms in each document
			Map<String, SortedMap<Integer, int[]>> distributionsMap = new HashMap<String, SortedMap<Integer, int[]>>();
			
			FlexibleParameters docParams = parameters.clone();
			docParams.setParameter("withDistributions", "true");
			docParams.setParameter("minRawFreq", 1);//binsPerDoc); // need at least this many occurrences for proper bin distribution
			docParams.setParameter("sort", "rawFreq");
			docParams.setParameter("dir", "DESC");
			StringBuilder qsb = new StringBuilder();
			for (CorpusTerm ct : cts) {
				qsb.append(ct.getTerm()).append(",");
			}
			String queryString = qsb.substring(0, qsb.length()-1);
			docParams.setParameter("query", queryString);
			
			for (String docId : docIds) {
				docParams.setParameter("docId", docId);
				DocumentTerms dts = new DocumentTerms(storage, docParams);
				dts.run(corpusMapper);
				
				Iterator<DocumentTerm> it = dts.iterator();
				while (it.hasNext()) {
					DocumentTerm dt = it.next();
					String term = dt.getTerm();
					
					if (!distributionsMap.containsKey(term)) {
						distributionsMap.put(term, new TreeMap<Integer, int[]>());
					}
					
					distributionsMap.get(term).put(dt.getDocIndex(), dt.getRawDistributions(binsPerDoc));
				}
			}
			
			// combine distributions
			for (Entry<String, SortedMap<Integer, int[]>> docMap : distributionsMap.entrySet()) {
				String term = docMap.getKey();
				int[] combinedDist = new int[bins];
				int combinedTokenCount = 0;
				int currentIndex = 0;
				for (Entry<Integer, int[]> docsMap : docMap.getValue().entrySet()) {
					int docIndex = docsMap.getKey();
					int[] dist = docsMap.getValue();
					combinedTokenCount += tokenCounts[docIndex];
					int pos = currentIndex * binsPerDoc;
					System.arraycopy(dist, 0, combinedDist, pos, dist.length);
					currentIndex++;
				}
				
				int rawFreq = 0;
				for (int i = 0; i < combinedDist.length; i++) {
					rawFreq += combinedDist[i];
				}
				float relFreq = rawFreq / combinedTokenCount;
				
				float[] floatDist = new float[bins];
				for (int i = 0; i < combinedDist.length; i++) {
					if (comparisonType == ComparisonType.RELATIVE) {
						floatDist[i] = (float)combinedDist[i] / combinedTokenCount;
					} else if (comparisonType == ComparisonType.TFIDF) {
						int inDocuments = docMap.getValue().size();
						float tfidf = ((float) rawFreq / (float) combinedTokenCount) * (float) Math.log10((float) docIds.size() / (float) inDocuments);
						floatDist[i] = tfidf;
					} else {
						floatDist[i] = combinedDist[i];
					}
				}
				
				analysisTerms.add(new RawCATerm(term, rawFreq, relFreq, CategoryType.TERM));
				
				termDistributionsMap.put(term, floatDist);
			}
		}
		
		// sort by raw freq, descending
		Collections.sort(analysisTerms, new Comparator<RawPCATerm>() {
			@Override
			public int compare(RawPCATerm arg0, RawPCATerm arg1) {
				if (arg0.getRawFrequency() > arg1.getRawFrequency()) return -1;
				else if (arg0.getRawFrequency() < arg1.getRawFrequency()) return 1;
				else return 0;
			}
		});
		
		int numTerms = analysisTerms.size();
		
		if (type == MatrixType.DOCUMENT) {
			freqMatrix = new double[numDocs][numTerms];
			
			int termIndex = 0;
			for (RawPCATerm aTerm : analysisTerms) {
				float[] values = termDistributionsMap.get(aTerm.getTerm());
				
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
			for (RawPCATerm aTerm : analysisTerms) {
				float[] values = termDistributionsMap.get(aTerm.getTerm());
				
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
	
	public List<RawCATerm> getAnalysisTerms() {
		return this.analysisTerms;
	}
}

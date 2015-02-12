package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.CorpusTerm;
import org.voyanttools.trombone.model.DocumentTerm;
import org.voyanttools.trombone.model.RawPCAType;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.algorithms.pca.PrincipalComponentsAnalysis;
import org.voyanttools.trombone.tool.algorithms.pca.PrincipalComponentsAnalysis.PrincipleComponent;
import org.voyanttools.trombone.util.FlexibleParameters;

import Jama.Matrix;

public class PCA extends AnalysisTool {

	private List<RawPCAType> pcaTypes;
	private List<PrincipleComponent> principalComponents;
	
	private String target;
	private int clusters;
	private String docId;
	private int bins;
	private int dimensions;
	
	public PCA(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		
		target = parameters.getParameterValue("target");
		clusters = parameters.getParameterIntValue("clusters");
		docId = parameters.getParameterValue("docId");
		bins = parameters.getParameterIntValue("bins", 10);
		dimensions = parameters.getParameterIntValue("dimensions", 2);
		
		this.pcaTypes = new ArrayList<RawPCAType>();
		this.principalComponents = new ArrayList<PrincipleComponent>();
	}
	
	private double[][] doPCA(double[][] freqMatrix) {
		//Matrix originalData = new Matrix(freqMatrix);
	    //originalData.print(8, 4);
		
	    PrincipalComponentsAnalysis pca = new PrincipalComponentsAnalysis(freqMatrix);
	    
	    double[][] matrixAdjusted = PrincipalComponentsAnalysis.getMeanAdjusted(freqMatrix, pca.getMeans());
	    Matrix adjustedInput = new Matrix(matrixAdjusted);
	    
	    SortedSet<PrincipleComponent> principalComponents = pca.getPrincipleComponents();
	    Iterator<PrincipleComponent> it = principalComponents.iterator();
	    while (it.hasNext()) {
	    	PrincipleComponent pc = it.next();
	    	this.principalComponents.add(pc);
	    }
	    
	    Matrix features = PrincipalComponentsAnalysis.getDominantComponentsMatrix(pca.getDominantComponents(dimensions));

	    Matrix featuresXpose = features.transpose();

	    Matrix xformedData = featuresXpose.times(adjustedInput.transpose());

	    double[][] result = xformedData.transpose().getArray();
	    return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void runAnalysis(CorpusMapper corpusMapper) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		
		int i;
		int numDocs = corpus.size();
		
		double[] targetVector = null;
		List<String> initialTypes = new ArrayList<String>();
//		List<String> initialTypes = new ArrayList<String>(Arrays.asList(this.properties.getParameterValues("type")));
//		if (target != null) this.properties.setParameter("type", "");
		
		double[][] result = null;
		
		if (numDocs > 1 && docId == null) {
			double[][] freqMatrix = this.buildFrequencyMatrix(corpusMapper, CORPUS);
			List<CorpusTerm> corpusTerms = (List<CorpusTerm>) this.getTypesList();
			
			result = this.doPCA(freqMatrix);
			
		    for (i = 0; i < result.length; i++) {
		    	final CorpusTerm corpusTerm = corpusTerms.get(i);
		    	
		    	if (corpusTerm.getTerm().equals(target)) targetVector = result[i];
			    
		    	int rawFreq = corpusTerm.getRawFreq();
		    	double relFreq = (double) rawFreq / corpus.getTokensCount(TokenType.lexical);
		    	
		    	this.pcaTypes.add(new RawPCAType(corpusTerm.getTerm(), rawFreq, relFreq, result[i]));
		    }
		} else {
			double[][] freqMatrix = this.buildFrequencyMatrix(corpusMapper, DOCUMENT);
			final List<DocumentTerm> docTerms = (List<DocumentTerm>) this.getTypesList();
			
			result = this.doPCA(freqMatrix);
			
		    for (i = 0; i < result.length; i++) {
		    	DocumentTerm docTerm = docTerms.get(i);
		    	
		    	if (docTerm.getTerm().equals(target)) targetVector = result[i];
		    	
		    	this.pcaTypes.add(new RawPCAType(docTerm.getTerm(), docTerm.getRawFrequency(), docTerm.getRelativeFrequency(), result[i]));
		    }
		}
		
		if (clusters > 0) {
			AnalysisTool.clusterPoints(this.pcaTypes, clusters);
		}
		
		if (target != null) {
			double[][] minMax = AnalysisTool.getMinMax(result);
			double distance = AnalysisTool.getDistance(minMax[0], minMax[1]) / 50;
			AnalysisTool.filterTypesByTarget(this.pcaTypes, targetVector, distance, initialTypes);
			this.maxOutputDataItemCount = this.pcaTypes.size();
		}
	}

}
/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

//import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
//import org.deeplearning4j.models.word2vec.Word2Vec;
//import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
//import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
//import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
//import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
//import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.Storage.Location;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

/**
 * @author sgs
 *
 */
@XStreamAlias("corpusVectors")
public class CorpusVectors extends AbstractCorpusTool {
	
	Map<String, Collection<String>> nearestWords = null;
	
	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusVectors(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		/*
		Word2Vec word2Vec = getWord2Vec(corpusMapper);
		if (parameters.containsKey("query")) {
			nearestWords = new HashMap<String, Collection<String>>();
			int limit = parameters.getParameterIntValue("limit", 10);
			for (String query : getQueries()) {
				nearestWords.put(query, word2Vec.wordsNearest(query, limit));
			}
		}
		
		Collection<String> kingList = word2Vec.wordsNearest(Arrays.asList("man", "mrs"), Arrays.asList("woman"), 1);
		System.out.println(kingList);
		*/
	}

	/*
	private Word2Vec getWord2Vec(CorpusMapper corpusMapper) throws IOException {
//		new File(getClass().getClassLoader().getResource("opennlp/GoogleNews-vectors-negative300.bin.gz").getFile());
//		File gModel = new File(getClass().getClassLoader().getResource("opennlp/GoogleNews-vectors-negative300.bin.gz").getFile());
//	    Word2Vec vec = WordVectorSerializer.readWord2VecModel(gModel);
//	    return vec;
		
		// much of the code here adapted from https://deeplearning4j.org/docs/latest/deeplearning4j-nlp-word2vec
		
		// build the vector id
		String corpusVectorsId = corpusMapper.getCorpus().getId()+"-vectors.txt";
		
		// determine location of file
		File file;
		if (storage instanceof FileStorage) {
			file = ((FileStorage) storage).getResourceFile(corpusVectorsId, Location.cache);
		} else if (storage instanceof MemoryStorage) {
			File temp = File.createTempFile(corpusVectorsId, null);
			file = new File(temp.getParentFile(), corpusVectorsId);
			temp.delete();
		} else {
			throw new IllegalStateException("Unrecognized storage format.");
		}
		// make sure parent exists, especially for tests
		File parent = file.getParentFile();
		if (parent.exists()==false) {
			if (!parent.mkdirs()) {
				throw new IOException("Unable to create: "+parent);
			}
		}

		// generate word2vec
		Word2Vec word2Vec;
		if (false &&  file.exists()) {
			word2Vec = WordVectorSerializer.readWord2VecModel(file);
		} else {
			
			File tempSentencesFile = File.createTempFile(corpusVectorsId, null);
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempSentencesFile));
			
			// adapted from https://www.tutorialspoint.com/opennlp/opennlp_sentence_detection.htm
			// Loading sentence detector model 
			//Loading sentence detector model
			
			File sentenceModel = new File(getClass().getClassLoader().getResource("opennlp/en-sent.bin").getFile());
			InputStream inputStream = new FileInputStream(sentenceModel); 
			SentenceModel model = new SentenceModel(inputStream);
			
		    // Instantiating the SentenceDetectorME class 
		    SentenceDetectorME detector = new SentenceDetectorME(model);
		    
		    // cycle through each document to get sentences
			for (IndexedDocument doc : corpusMapper.getCorpus()) {
				
				// Detecting the sentence
				String text = doc.getDocumentString();
				text = text.replaceAll("<.+?>", ""); // simple strip tags but don't modify too much in case sentence model can use info (like case)
				String sentences[] = detector.sentDetect(text);
				for (String sentence : sentences) {
					sentence = sentence.toLowerCase().replaceAll("(\r|\n|\r\n)+", "").trim();
					writer.write(sentence);
					writer.newLine();
				}
			}
			writer.close();
			
			
			// use a very simple iterator based on our temp file format
			SentenceIterator iter = new LineSentenceIterator(tempSentencesFile);
			
	        // Split on white spaces in the line to get words
	        TokenizerFactory t = new DefaultTokenizerFactory();
	        t.setTokenPreProcessor(new CommonPreprocessor());
	        
	        word2Vec = new Word2Vec.Builder()
		                .minWordFrequency(5)
		                .layerSize(100)
		                .seed(42)
		                .windowSize(5)
		                .iterate(iter)
		                .tokenizerFactory(t)
		                .build();

	        word2Vec.fit();
	        
	        // save for next time
			WordVectorSerializer.writeWord2VecModel(word2Vec, file);
		}
		return word2Vec;
	}
		*/

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package postaggersalanguage.five;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.voyanttools.trombone.nlp.PosLemmas;

import com.shef.ac.uk.util.Util;

import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

/**
 *
 * @author ahmetaker
 */
public class POSTaggersALanguage {

	private String lang;
    private POSModel itsPOSModel = null;
    private SentenceModel itsSentenceModel = null;
    private TokenizerModel itsTokenizerModel = null;
    private Map<String, String> nounDic;
    private Map<String, String> adjDic;
    private Map<String, String> advDic;
    private Map<String, String> verbDic;
    private Map<String, String> detDic;
    private Map<String, String> pronDic;
    private Map<String, String> posMap;


    public POSTaggersALanguage(String lang) throws IOException {
    	this.lang = lang;
    	String file = this.getClass().getResource("").getFile();
        nounDic = Util.loadDictionary(file + "//dictionaries//" + lang + "//nounDic.txt");
        adjDic = Util.loadDictionary(file + "//dictionaries//" + lang + "//adjDic.txt");
        advDic = Util.loadDictionary(file + "//dictionaries//" + lang + "//advDic.txt");
        verbDic = Util.loadDictionary(file + "//dictionaries//" + lang + "//verbDic.txt");
        detDic = Util.loadDictionary(file + "//dictionaries//" + lang + "//detDic.txt");
        pronDic = Util.loadDictionary(file + "//dictionaries//" + lang + "//pronounDic.txt");
        posMap = Util.getFileContentAsMap(file + "/universal-pos-tags/" + lang + "POSMapping.txt", "######", true);
	}

    public Span[] tokenizePos(String aSentence, String aResourceFolder) throws InvalidFormatException, IOException {
        if (itsTokenizerModel == null) {
            InputStream is = new FileInputStream(aResourceFolder + "/tokenizerModels/" + lang + "-token.bin");
            itsTokenizerModel = new TokenizerModel(is);
            is.close();
        }
        Tokenizer tokenizer = new TokenizerME(itsTokenizerModel);
        Span[] tokens = tokenizer.tokenizePos(aSentence);


        //now apply also some rules!
        ArrayList<Span> array = new ArrayList<Span>();
        for (int i = 0; i < tokens.length; i++) {
            String token = aSentence.substring(tokens[i].getStart(), tokens[i].getEnd());
            if ("".equals(token)) {
                continue;
            }
            char chraters[] = token.toCharArray();
            Vector<String> take = new Vector<String>();
            StringBuffer buffer = new StringBuffer();
            for (int j = 0; j < chraters.length; j++) {
                String c = chraters[j] + "";
                if (Heuristics.isPunctuation(c)) {
                    String str = buffer.toString().trim();
                    if (!str.equals("")) {
                        take.add(buffer.toString());
                    }
                    buffer = new StringBuffer();
                    take.add(c);
                } else {
                    buffer.append(c);
                }
            }
            if (!buffer.toString().equals("")) {
                take.add(buffer.toString());
            }
            for (int j = 0; j < take.size(); j++) {
                String string = take.get(j);
                array.add(new Span(tokens[i].getStart(), tokens[i].getEnd(), string));
            }
        }

        Span a[] = new Span[array.size()];
        return array.toArray(a);

    }

    public Span[] sentenceDetectPos(String aText) throws InvalidFormatException, IOException {
    	if (itsSentenceModel == null) {
            InputStream is = new FileInputStream(this.getClass().getResource("").getFile() + "/setenceDetectionModels/" + lang + "-sent.bin");
            itsSentenceModel = new SentenceModel(is);
            is.close();
        }
        SentenceDetectorME sdetector = new SentenceDetectorME(itsSentenceModel);

        Span[] sentences = sdetector.sentPosDetect(aText);
        return sentences;
    }

    public String[] posTag(String aSentence[], String aResourceFolder) {
        String posTaggedVersion[] = null;
        if (itsPOSModel == null) {
            itsPOSModel = new POSModelLoader()
                    .load(new File(this.getClass().getResource("").getFile() + "/posModels/" + lang + "-pos-maxent.bin"));
        }
        //PerformanceMonitor perfMon = new PerformanceMonitor(System.err, "sent");
        POSTaggerME tagger = new POSTaggerME(itsPOSModel);

        posTaggedVersion = tagger.tag(aSentence);
        return posTaggedVersion;
    }
    
    public PosLemmas getLemmatized(String text) throws IOException {
    	
    	String file = this.getClass().getResource("").getFile();
    	PosLemmas posLemmas = new PosLemmas(text);
    	Span[] sentences = sentenceDetectPos(text);
    	for (Span sentence : sentences) {
    		int sentenceStart = sentence.getStart();
    		String sentenceString = text.substring(sentenceStart, sentence.getEnd());
    		Span[] tokens = tokenizePos(sentenceString, file);
    		String[] strings = Span.spansToStrings(tokens, sentenceString);
    		String[] pos = posTag(strings, file);
    		for (int i=0; i<tokens.length; i++) {
                String token = strings[i];
                String lemma = null;
                String posType = pos[i];
                if ("it".equalsIgnoreCase(lang)) {
                    posType = posType.substring(0, 1);
                }
                String generalType = posMap.get(posType.toLowerCase());
                
                if (Heuristics.isNumber(token)==false && Heuristics.isPunctuation(token)==false) {
                    
                    if (generalType != null) {
                        if ("NOUN".equalsIgnoreCase(generalType)) {
                            lemma = nounDic.get(token.toLowerCase());
                        } else if ("VERB".equalsIgnoreCase(generalType)) {
                            lemma = verbDic.get(token.toLowerCase());
                        } else if ("ADJ".equalsIgnoreCase(generalType)) {
                            lemma = adjDic.get(token.toLowerCase());
                        } else if ("ADV".equalsIgnoreCase(generalType)) {
                            lemma = advDic.get(token.toLowerCase());
                        } else if ("PRON".equalsIgnoreCase(generalType)) {
                            lemma = pronDic.get(token.toLowerCase());

                        }
                        if (!"nl".equalsIgnoreCase(lang) && lemma == null) {
                            try {
                                lemma = Lemmatizer.getLemma(file, token, lang, generalType);
                            } catch (Exception e) {
                                try {
                                    lemma = Lemmatizer.getLemma(file, token.toLowerCase(), lang, generalType);
                                } catch (Exception e2) {
                                }
                            }
                        }
                    }
                	posLemmas.add(token, generalType, lemma, sentenceStart+tokens[i].getStart(), sentenceStart+tokens[i].getEnd());
                }
//                if (lemma!=null) {
//                	posLemmas.add(token, generalType, lemma, sentenceStart+tokens[i].getStart(), sentenceStart+tokens[i].getEnd());
//                	spans.add(new Span(sentenceStart+tokens[i].getStart(), sentenceStart+tokens[i].getEnd(), lemma));
//                }
    			
    		}
    	}
    	return posLemmas;
    }

    public static void main(String args[]) throws InvalidFormatException, IOException {
    	String lang = "en";
        POSTaggersALanguage posTagger = new POSTaggersALanguage(lang);
        String text = "This time, it’s your turn: advise Parliament in the first LinkedIn discussion on an ongoing report. The rapporteur wants to hear your views @...(read more). --- Keywords ---";
        PosLemmas lemmas = posTagger.getLemmatized(text);
        Iterator<PosLemmas> iterator = lemmas.iterator();
        while (iterator.hasNext()) {
        	iterator.next();
        	System.out.println(lemmas.getCurrentTerm()+"-"+lemmas.getCurrentLemma());
        }
    }
}

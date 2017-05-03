/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package postaggersalanguage.five;

import com.shef.ac.uk.util.Util;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.voyanttools.trombone.nlp.PosLemmas;

import net.sf.hfst.NoTokenizationException;
import opennlp.tools.cmdline.PerformanceMonitor;
import opennlp.tools.cmdline.postag.POSModelLoader;
//import opennlp.tools.lang.spanish.SentenceDetector;
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

    private POSModel itsPOSModel = null;
    private SentenceModel itsSentenceModel = null;
    private TokenizerModel itsTokenizerModel = null;

    public String[] tokenize(String aSentence, String aLang, String aResourceFolder) throws InvalidFormatException, IOException {
        if (itsTokenizerModel == null) {
            InputStream is = new FileInputStream(aResourceFolder + "/tokenizerModels/" + aLang + "-token.bin");
            itsTokenizerModel = new TokenizerModel(is);
            is.close();
        }
        Tokenizer tokenizer = new TokenizerME(itsTokenizerModel);
        String[] tokens = tokenizer.tokenize(aSentence);


        //now apply also some rules!
        ArrayList<String> array = new ArrayList<String>();
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].trim();
            if ("".equals(token)) {
                continue;
            }
//            if (token.equals("...")) {
//                array.add(".");
//                array.add(".");
//                array.add(".");
//                continue;
//            }

            //    String first = token.charAt(0) + "";
            //System.out.println("here we got a token " + token);
//            if (Heuristics.isPunctuation(first)) {
//                token = token.substring(1);
//                String secondFirst = null;
//                if (token.length() > 0) {
//                    secondFirst = token.charAt(0) + "";
//                    if (Heuristics.isPunctuation(secondFirst)) {
//                        token = token.substring(1);
//                    } else {
//                        secondFirst = null;
//                    }
//                }
//                array.add(first);
//                if (secondFirst != null) {
//                    array.add(secondFirst);
//                }
//            } else {
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
                array.add(string);
            }
//            }



//            if (token.length() > 0) {
//                String last = token.charAt(token.length() - 1) + "";
//
//                if (Heuristics.isPunctuation(last)) {
//                    token = token.substring(0, token.length() - 1);
//                    if (token.length() > 0) {
//                        String secondLast = token.charAt(token.length() - 1) + "";
//                        if (Heuristics.isPunctuation(secondLast)) {
//                            token = token.substring(0, token.length() - 1);
//                            if (token.length() > 0) {
//                                array.add(token);
//                            }
//                            array.add(secondLast);
//                            array.add(last);
//                        } else {
//                            array.add(token);
//                            array.add(last);
//                        }
//                    }
//                } else {
//                    array.add(token);
//                }
//            }

        }

        String a[] = new String[array.size()];
        return array.toArray(a);

    }
    
    public Span[] tokenizePos(String aSentence, String aLang, String aResourceFolder) throws InvalidFormatException, IOException {
        if (itsTokenizerModel == null) {
            InputStream is = new FileInputStream(aResourceFolder + "/tokenizerModels/" + aLang + "-token.bin");
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
//            }



//            if (token.length() > 0) {
//                String last = token.charAt(token.length() - 1) + "";
//
//                if (Heuristics.isPunctuation(last)) {
//                    token = token.substring(0, token.length() - 1);
//                    if (token.length() > 0) {
//                        String secondLast = token.charAt(token.length() - 1) + "";
//                        if (Heuristics.isPunctuation(secondLast)) {
//                            token = token.substring(0, token.length() - 1);
//                            if (token.length() > 0) {
//                                array.add(token);
//                            }
//                            array.add(secondLast);
//                            array.add(last);
//                        } else {
//                            array.add(token);
//                            array.add(last);
//                        }
//                    }
//                } else {
//                    array.add(token);
//                }
//            }

        }

        Span a[] = new Span[array.size()];
        return array.toArray(a);

    }

    public String[] sentenceDetect(String aText, String aLang, String aResourceFolder) throws InvalidFormatException, IOException {
        if (itsSentenceModel == null) {
            InputStream is = new FileInputStream(aResourceFolder + "/setenceDetectionModels/" + aLang + "-sent.bin");
            itsSentenceModel = new SentenceModel(is);
            is.close();
        }
        SentenceDetectorME sdetector = new SentenceDetectorME(itsSentenceModel);

        String[] sentences = sdetector.sentDetect(aText);
        return sentences;
    }

    public Span[] sentenceDetectPos(String aText, String aLang, String aResourceFolder) throws InvalidFormatException, IOException {
        if (itsSentenceModel == null) {
            InputStream is = new FileInputStream(aResourceFolder + "/setenceDetectionModels/" + aLang + "-sent.bin");
            itsSentenceModel = new SentenceModel(is);
            is.close();
        }
        SentenceDetectorME sdetector = new SentenceDetectorME(itsSentenceModel);

        Span[] sentences = sdetector.sentPosDetect(aText);
        return sentences;
    }

    public String[] posTag(String aSentence[], String aLang, String aResourceFolder) {
        String posTaggedVersion[] = null;
        if (itsPOSModel == null) {
            itsPOSModel = new POSModelLoader()
                    .load(new File(aResourceFolder + "/posModels/" + aLang + "-pos-maxent.bin"));
        }
        //PerformanceMonitor perfMon = new PerformanceMonitor(System.err, "sent");
        POSTaggerME tagger = new POSTaggerME(itsPOSModel);

        posTaggedVersion = tagger.tag(aSentence);
        return posTaggedVersion;
    }
    
    public PosLemmas getLemmatized(String text, String lang) throws IOException {
    	
    	PosLemmas posLemmas = new PosLemmas(text);
    	String file = this.getClass().getResource("").getFile();
        Map<String, String> nounDic = Util.loadDictionary(file + "//dictionaries//" + lang + "//nounDic.txt");
        Map<String, String> adjDic = Util.loadDictionary(file + "//dictionaries//" + lang + "//adjDic.txt");
        Map<String, String> advDic = Util.loadDictionary(file + "//dictionaries//" + lang + "//advDic.txt");
        Map<String, String> verbDic = Util.loadDictionary(file + "//dictionaries//" + lang + "//verbDic.txt");
        Map<String, String> detDic = Util.loadDictionary(file + "//dictionaries//" + lang + "//detDic.txt");
        Map<String, String> pronDic = Util.loadDictionary(file + "//dictionaries//" + lang + "//pronounDic.txt");


        Map<String, String> posMap = Util.getFileContentAsMap(file + "/universal-pos-tags/" + lang + "POSMapping.txt", "######", true);

    	Span[] sentences = sentenceDetectPos(text, lang, file);
    	for (Span sentence : sentences) {
    		int sentenceStart = sentence.getStart();
    		String sentenceString = text.substring(sentenceStart, sentence.getEnd());
    		Span[] tokens = tokenizePos(sentenceString, lang, file);
    		String[] strings = Span.spansToStrings(tokens, sentenceString);
    		String[] pos = posTag(strings, lang, file);
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
        POSTaggersALanguage posTagger = new POSTaggersALanguage();
        String text = "This time, it’s your turn: advise Parliament in the first LinkedIn discussion on an ongoing report. The rapporteur wants to hear your views @...(read more). --- Keywords ---";
        String lang = "en";
        PosLemmas lemmas = posTagger.getLemmatized(text, lang);
        Iterator<PosLemmas> iterator = lemmas.iterator();
        while (iterator.hasNext()) {
        	iterator.next();
        	System.out.println(lemmas.getCurrentTerm()+"-"+lemmas.getCurrentLemma());
        }
    }
}

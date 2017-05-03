/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package postaggersalanguage.five;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import static net.sf.hfst.HfstOptimizedLookup.runTransducer;

import net.sf.hfst.FormatException;
import net.sf.hfst.NoTokenizationException;
import net.sf.hfst.Transducer;
import net.sf.hfst.TransducerAlphabet;
import net.sf.hfst.TransducerHeader;
import net.sf.hfst.UnweightedTransducer;
import net.sf.hfst.WeightedTransducer;

/**
 *
 * @author ahmetaker
 */
public class Lemmatizer {

    static Transducer transducer = null;
    public final static long TRANSITION_TARGET_TABLE_START = 2147483648l; // 2^31 or UINT_MAX/2 rounded up
    public final static long NO_TABLE_INDEX = 4294967295l;
    public final static float INFINITE_WEIGHT = (float) 4294967295l; // this is hopefully the same as
    // static_cast<float>(UINT_MAX) in C++
    public final static int NO_SYMBOL_NUMBER = 65535; // this is USHRT_MAX

    public static enum FlagDiacriticOperator {

        P, N, R, D, C, U
    };

    public static String getLemma(String aResourceFolder, String aWord, String aLang, String aPOSType) throws IOException, NoTokenizationException, FormatException {
        if (transducer == null) {
            FileInputStream transducerfile = null;
            transducerfile = new FileInputStream(aResourceFolder + "/lemmaModels/" + aLang + ".hfst.ol");
            TransducerHeader h = new TransducerHeader(transducerfile);
            DataInputStream charstream = new DataInputStream(transducerfile);
            TransducerAlphabet a = new TransducerAlphabet(charstream, h.getSymbolCount());
            if (h.isWeighted()) {
                transducer = new WeightedTransducer(transducerfile, h, a);
            } else {
                transducer = new UnweightedTransducer(transducerfile, h, a);
            }

        }
        Collection<String> analyses = transducer.analyze(aWord);
        for (String analysis : analyses) {
            if ("en".equalsIgnoreCase(aLang)) {
                String grammar = "NONE";
                String grammarCheck = "NONE";
                if ("NOUN".equalsIgnoreCase(aPOSType)) {
                    grammar = "\\[N\\]\\+N.*";
                    grammarCheck = "[N]+N";
                } else if ("VERB".equalsIgnoreCase(aPOSType)) {
                    grammar = "\\[V\\]\\+V.*";
                    grammarCheck = "[V]+V";
                } else if ("ADJ".equalsIgnoreCase(aPOSType)) {
                    grammar = "\\[ADJ\\]\\+ADJ.*";
                    grammarCheck = "[ADJ]+ADJ";
                } else if ("ADV".equalsIgnoreCase(aPOSType)) {
                    grammar = "\\[ADV\\]\\+ADV.*";
                    grammarCheck = "[ADV]+ADV";
                }
                //System.out.println(analysis);
                if (analysis.contains(grammarCheck)) {
                    String lemma = analysis.replaceAll(grammar, "");
                    if ((lemma.contains("+") && !lemma.contains("-")) && (aWord.contains("-") && !aWord.contains("+"))) {
                        lemma = lemma.replaceAll("\\+", "-");
                    }
                    if (lemma.contains("+") && !aWord.contains("+")) {
                        lemma = lemma.replaceAll("\\+", "");
                    }
                    return lemma.toLowerCase();
                }
            } else if ("de".equalsIgnoreCase(aLang)) {
                String grammar = "NONE";
                String grammar2 = ">";
                String grammarCheck = "NONE";
                if ("NOUN".equalsIgnoreCase(aPOSType)) {
                    grammar = "<\\+NN>.*";
                    grammarCheck = "<+NN>";
                } else if ("VERB".equalsIgnoreCase(aPOSType)) {
                    grammar = "<\\+V>.*";
                    grammarCheck = "<+V>";
                } else if ("ADJ".equalsIgnoreCase(aPOSType)) {
                    grammar = "<\\+ADJ>.*";
                    grammarCheck = "<+ADJ>";
                } else if ("ADV".equalsIgnoreCase(aPOSType)) {
                    grammar = "<\\+ADV>.*";
                    grammarCheck = "<+ADV>";
                } else if ("CONJ".equalsIgnoreCase(aPOSType)) {
                    grammar = "<\\+KONJ>.*";
                    grammarCheck = "<+KONJ>";
                }
                //System.out.println(analysis);
                if (analysis.contains(grammarCheck)) {
                    String remaining = analysis.replaceAll(grammar, "");
                    String vals[] = remaining.split(grammar2);
                    StringBuffer buffer = new StringBuffer();
                    String suffix = "";
                    for (int i = 0; i < vals.length - 1; i++) {
                        String val = vals[i];
                        //System.out.println(val);
                        if (!val.startsWith("<CAP")) {
                            val = val.replaceAll("<.*", "");
                            buffer.append(val.toLowerCase());
                        }
                    }
                    String lastWord = vals[vals.length - 1].toString().replaceAll("<.*", "");
                    if (lastWord.endsWith("<SUFF")) {
                        suffix = lastWord.toLowerCase();
                    }
                    String result = null;
//                    if (aWord.toLowerCase().startsWith(buffer.toString() + "s") && !buffer.toString().trim().equals("") && !secondWord.startsWith("s")) {
//                        result = buffer.append("s").append(vals[vals.length - 1].toLowerCase()).toString().replaceAll("<.*", "");
//                    } else 
                    if (aWord.toLowerCase().equals(buffer.toString())) {
                        return aWord.toLowerCase();
                    } else {
                        String lastChar = lastWord.substring(lastWord.length()-1, lastWord.length());
                        String local = buffer.toString() + lastChar;
                        //System.out.println(local);
                        if (local.equalsIgnoreCase(aWord)) {
                            return local;
                        }
                        String last2Char = lastWord.substring(lastWord.length()-2, lastWord.length());
                        local = buffer.toString() + last2Char;
                        //System.out.println(local);
                        if (local.equalsIgnoreCase(aWord)) {
                            return local;
                        }
                    }
                    if (aWord.toLowerCase().startsWith(buffer.toString()) && !buffer.toString().trim().equals("")) {
                        String wordRemaining = aWord.toLowerCase().replaceAll(buffer.toString(), "");
                        wordRemaining = wordRemaining.replaceAll(lastWord.toLowerCase(), "");
                        if (!wordRemaining.trim().equals("") && wordRemaining.trim().length() <= 2) {                            
                            if (!suffix.equals("")) {
                                result = buffer.append(wordRemaining).toString();
                            } else {
                                String local = buffer.toString() + lastWord.toLowerCase().toString();
                                if (aWord.toLowerCase().startsWith(local)) {
                                    result = local;
                                } else {
                        //System.out.println("hep " + aWord + " _ " + buffer.toString() + " _ " + vals[vals.length - 1].toLowerCase().toString().replaceAll("<.*", "") + " _ " + wordRemaining);
                                result = buffer.append(wordRemaining).append(lastWord.toLowerCase()).toString();                                    
                                }
                            }
                        } else {
                            result = buffer.append(lastWord.toLowerCase()).toString();

                        }
                    } else if (buffer.toString().trim().equals("")) {
                        result = buffer.append(vals[vals.length - 1].toLowerCase()).toString().replaceAll("<.*", "");
                    }

                    if (result != null) {
                        result = result.replaceAll("\\{", "").replaceAll("\\}", "");
                    }
                    return result;
                }
            } else if ("it".equalsIgnoreCase(aLang)) {

                String grammar = "NONE";
                String grammarCheck = "NONE";
                if ("NOUN".equalsIgnoreCase(aPOSType)) {
                    grammar = "#NOUN.*";
                    grammarCheck = "#NOUN";
                } else if ("VERB".equalsIgnoreCase(aPOSType)) {
                    grammar = "#VER.*";
                    grammarCheck = "#VER";
                } else if ("ADJ".equalsIgnoreCase(aPOSType)) {
                    grammar = "#ADJ.*";
                    grammarCheck = "#ADJ";
                } else if ("ADV".equalsIgnoreCase(aPOSType)) {
                    grammar = "#ADV.*";
                    grammarCheck = "#ADV";
                } else if ("CONJ".equalsIgnoreCase(aPOSType)) {
                    grammar = "#CON.*";
                    grammarCheck = "#CON";

                }
                //System.out.println(analysis);
                if (analysis.contains(grammarCheck)) {
                    String lemma = analysis.replaceAll(grammar, "");
                    if ((lemma.contains("+") && !lemma.contains("-")) && (aWord.contains("-") && !aWord.contains("+"))) {
                        lemma = lemma.replaceAll("\\+", "-");
                    }
                    if (lemma.contains("+") && !aWord.contains("+")) {
                        lemma = lemma.replaceAll("\\+", "");
                    }
                    return lemma.toLowerCase();
                }
            } else if ("fr".equalsIgnoreCase(aLang)) {
                String grammar = "NONE";
                String grammarCheck = "NONE";
                if ("NOUN".equalsIgnoreCase(aPOSType)) {
                    grammar = "\\+commonNoun.*";
                    grammarCheck = "+commonNoun";
                } else if ("VERB".equalsIgnoreCase(aPOSType)) {
                    grammar = "\\+verb+.*";
                    grammarCheck = "+verb+";
                } else if ("ADJ".equalsIgnoreCase(aPOSType)) {
                    grammar = "\\+adjective.*";
                    grammarCheck = "+adjective";
                } else if ("ADV".equalsIgnoreCase(aPOSType)) {
                    grammar = "\\+adverb.*";
                    grammarCheck = "+adverb";
                } else if ("PRON".equalsIgnoreCase(aPOSType) || "CONJ".equalsIgnoreCase(aPOSType)) {
                    grammar = "\\+functionWord.*";
                    grammarCheck = "+functionWord";

                }
                //System.out.println(analysis);
                if (analysis.contains(grammarCheck)) {
                    String lemma = analysis.replaceAll(grammar, "");
                    if ((lemma.contains("+") && !lemma.contains("-")) && (aWord.contains("-") && !aWord.contains("+"))) {
                        lemma = lemma.replaceAll("\\+", "-");
                    }
                    if (lemma.contains("+") && !aWord.contains("+")) {
                        lemma = lemma.replaceAll("\\+", "");
                    }
                    return lemma.toLowerCase();
                }
            }
        }
        if (analyses.isEmpty()) {
            return null;
        }
        return null;
    }

    public static void main(String args[]) throws IOException, NoTokenizationException, FormatException {
    	URL url = Lemmatizer.class.getResource("");
        String lemma = Lemmatizer.getLemma(url.getFile(), "hochenergetischen", "de", "adj");
        //lemma = "M" + lemma.substring(1);
        System.out.println(lemma);
    }
}

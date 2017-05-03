/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package postaggersalanguage.five;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ahmetaker
 */
public class Heuristics {


    public static boolean isAChar(String aChar) {
        String pattern = "[a-z]";
        Pattern r = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher m = r.matcher(aChar);
        return m.matches();
    }
    
    public static boolean isPunctuation(String aWord) {
        String pattern = "\\p{Punct}+$";
        Pattern r = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

        Matcher m = r.matcher(aWord);
        boolean match = m.matches();
        if (!match) {
            if (aWord.equals("\"") || aWord.equals("[") || aWord.equals("]")
                    || aWord.equals("{") || aWord.equals("}") || aWord.equals("(") || aWord.equals(")")
                    || aWord.equals("~") || aWord.equals("#") || aWord.equals("'") || aWord.equals("`") 
                    || aWord.equals("*") || aWord.equals("’") || aWord.equals("“") || aWord.equals("„") 
                    || aWord.equals("≥") || aWord.equals(">") || aWord.equalsIgnoreCase("<")) {
                return true;
            } 
        }
        return match;
    }

    public static boolean endsWithPunction(String aWord) {
        String pattern = "\\p{Punct}+$";
        Pattern r = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

        Matcher m = r.matcher(aWord.charAt(aWord.length() - 1) + "");
        return m.matches();
    }

    public static boolean isNumber(String aWord) {
        Pattern p = Pattern.compile("\\d+,\\d+");
        Matcher m = p.matcher(aWord);
        Pattern p2 = Pattern.compile("\\d+(.)*");
        Matcher m2 = p2.matcher(aWord);
        return (m.find() || m2.find());
    }

        public static boolean isStrictNumber(String aWord) {
        Pattern p = Pattern.compile("\\d+.\\d+");
        Matcher m = p.matcher(aWord);
        Pattern p2 = Pattern.compile("\\d+");
        Matcher m2 = p2.matcher(aWord);
        return (m.matches() || m2.matches());
    }

    public static boolean isAllCapital(String aWord) {
        Pattern p = Pattern.compile("\\b([A-Z])?(\\p{Lu})?[A-Z\\p{Lu}]+\\b");
        Matcher m = p.matcher(aWord);
        return (m.matches());
    }

    public static boolean isFirstCharCapital(String aWord) {
        return Character.isUpperCase(aWord.charAt(0));
    }

    public static boolean foundNounSequenceThree(String aSentence) {
        Pattern p = Pattern.compile("(\\w)+,(\\s)+(\\w)+,(\\s)+(\\w)+");
        Matcher m = p.matcher(aSentence);
        return m.find();
    }

    public static boolean foundNounSequenceTwo(String aSentence) {
        Pattern p = Pattern.compile("(\\w)+,(\\s)+(\\w)+");
        Matcher m = p.matcher(aSentence);
        return m.find();
    }

    public static void main(String args[]) {
        System.out.println(Heuristics.isPunctuation("\""));
        System.out.println("ahmet".substring(0, "ahmet".length()-1));
        //System.out.println("substring".charAt("substring".length()));
    }
}

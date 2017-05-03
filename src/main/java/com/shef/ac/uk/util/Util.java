package com.shef.ac.uk.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

/**
 * @author Ahmet Aker
 */
public class Util {

    public static String SPECIFIC_FOLDER = null;

    public static StringBuffer getFileContentAsBufferNonUTF(String aFileName) throws IOException {
        StringBuffer buffer = new StringBuffer();
        BufferedReader in = null;
        in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            buffer.append(str.trim()).append(" \n");
        }
        in.close();
        return buffer;
    }

    public static StringBuffer getFileContentAsBufferUTF(String aFileName) throws IOException {
        StringBuffer buffer = new StringBuffer();
        try {
            buffer = getFileContentAsBuffer(aFileName);
            DataInputStream stream = new DataInputStream(new FileInputStream(new File(aFileName)));
            String str = stream.readUTF();
            buffer.append(str.trim()).append("\n");
            stream.close();
        } catch (EOFException e) {
            return getFileContentAsBuffer(aFileName);
        }
        return buffer;
    }

    public static StringBuffer getFileContentAsBufferNonUTFNoBreak(String aFileName) throws IOException {
        StringBuffer buffer = new StringBuffer();
        BufferedReader in = null;
        in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            buffer.append(str.trim().replaceAll("\\n", "")).append(" ");
        }
        in.close();
        return buffer;
    }

    public static Vector<String> getFileContentAsVector(String aFileName) throws IOException {
        Vector<String> vector = new Vector<String>();
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            vector.add(str);
        }
        in.close();
        return vector;
    }

    public static List<URL> getFileContentAsURL(String aFileName) throws IOException {
        List<URL> vector = new Vector<URL>();
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            vector.add(new URL(str));
        }
        in.close();
        return vector;
    }

    public static Vector<String> getFileContentAsVectorNoDublicates(String aFileName) throws IOException {
        Vector<String> vector = new Vector<String>();
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            str = str.replaceAll("######", "###0000###");
            str = str.replaceAll("####", "###");
            if (!vector.contains(str)) {
                vector.add(str);
            }
        }
        in.close();
        return vector;
    }

    public static Vector<String> getFileContentAsVectorUTF(String aFileName) throws IOException {
        Vector<String> vector = new Vector<String>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(aFileName)), "UTF-8"));
        String str;
        while ((str = in.readLine()) != null) {
            //if (!vector.contains(str)) {
            vector.add(str);

            //}
        }
        in.close();
        return vector;
    }

    public static Vector<String> getFileContentAsVectorUTFNoDubs(String aFileName) throws IOException {
        Vector<String> vector = new Vector<String>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(aFileName)), "UTF-8"));
        String str;
        while ((str = in.readLine()) != null) {
            if (!vector.contains(str)) {
                vector.add(str);
            }
        }
        in.close();
        return vector;
    }

    public static Map<String, String> getFileContentAsMap(String aFileName) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            map.put(str, str);
        }
        in.close();
        return map;
    }

    public static Map<String, String> getFileContentAsMap(String aFileName, String aDivider, boolean inLowerCased) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(aFileName)), "UTF-8"));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                String values[] = str.split(aDivider);
                if (values.length == 2) {
                    if (inLowerCased) {
                    map.put(values[0].trim().toLowerCase(), values[1].trim());
                        
                    } else {
                    map.put(values[0].trim(), values[1].trim());
                        
                    }
                }
            }
        }
        in.close();
        return map;
    }

    public static Map<String, String> loadDictionary(String aFileName) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(aFileName)), "UTF-8"));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                String values[] = str.split("===");
                if (values.length == 2) {
                    String vals[] = values[1].split(";");
                    for (int i = 0; i < vals.length; i++) {
                        String val = vals[i];
                        map.put(val.toLowerCase(), values[0].trim());

                    }
                }
            }
        }
        in.close();
        return map;
    }

    public Map<String, Vector<String>> getCharacterMapping(String aFileName, String aDivider) throws IOException {
        Map<String, Vector<String>> map = new HashMap<String, Vector<String>>();
        BufferedReader in = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(aFileName), "UTF-8"));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                String values[] = str.split(aDivider);
                if (values.length == 2) {
                    Vector<String> list = map.get(values[1]);
                    if (list == null) {
                        list = new Vector<String>();
                        list.add("");
                    }
                    if (!list.contains(values[0])) {
                        list.add(values[0]);

                    }
                    map.put(values[1], list);

                    list = map.get(values[1].toUpperCase());
                    if (list == null) {
                        list = new Vector<String>();
                        list.add("");
                    }
                    if (!list.contains(values[0])) {
                        list.add(values[0]);

                    }
                    map.put(values[1].toUpperCase(), list);


                }
            }
        }
        Vector<String> list = new Vector<String>();
        list.add("");
        list.add(" ");
        map.put(" ", list);
        in.close();
        return map;
    }

    public static Map<String, Vector<String>> getCharacterMapping2(String aFileName, String aDivider, String anTargetAplphabet, String aSourceAlpabet) throws IOException {
        Map<String, Vector<String>> map = new HashMap<String, Vector<String>>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(aFileName)), "UTF-8"));
        String str;
        Vector<String> taken = new Vector<String>();
        while ((str = in.readLine()) != null) {
            str = str.replaceAll("\\﻿", "");
            if (taken.contains(str)) {
                continue;
            }
            taken.add(str);
            if (!"".equals(str.trim())) {
                String values[] = str.split(aDivider);
                if (values.length == 2) {
//                    System.out.println(str);
                    Vector<String> list = map.get(values[1].trim());
                    if (list == null) {
                        list = new Vector<String>();
                        list.add("");
                    }
                    if (!list.contains(values[0].trim())) {
                        list.add(values[0].trim());
//                        if (values[0].equals("k")) {
//                            list.add("c");
//                        } 

                    }
                    map.put(values[1].trim(), list);
//                    list = map.get(values[1].toUpperCase());
//                    if (list == null) {
//                        list = new Vector<String>();
//                        list.add("");
//                    }
//                    if (!list.contains(values[0])) {
//                        list.add(values[0]);
//                    }
//                    map.put(values[1].toUpperCase(), list);
                }
            }
        }
        Vector<String> list = new Vector<String>();
        list.add("");
        list.add(" ");
        map.put(" ", list);
        in.close();
        Vector<String> alphabet2 = Util.getFileContentAsVectorUTFNoDubs(anTargetAplphabet);
        Vector<String> alphabet = Util.getFileContentAsVectorUTFNoDubs(aSourceAlpabet);
        for (int i = 0; i < alphabet.size(); i++) {
            String alph = alphabet.get(i).trim();
            alph = alph.replaceAll("\\﻿", "");
            if (alph.endsWith("_v")) {
                continue;
            }
            String charToAdd = alph.replaceAll(".*\\s", "");
            Vector<String> oldList = map.get(charToAdd.trim());
            if (oldList == null) {
                continue;
            }
            Vector<String> newList = new Vector<String>();
            for (int j = 0; j < oldList.size(); j++) {
                String existing = oldList.get(j);
                if ("".equals(existing.trim())) {
                    continue;
                }
                if (existing.toCharArray().length == 1) {
                    newList.add(existing + existing);
//                    newList.add(existing + "s");
                }

                for (int s = 0; s < alphabet2.size(); s++) {
                    String alph2 = alphabet2.get(s).trim();
                    alph2 = alph2.replaceAll("\\﻿", "");
                    if (!alph2.endsWith("_v")) {
                        continue;
                    }
                    alph2 = alph2.replaceAll("_v", "");
                    String charToAdd2 = alph2.replaceAll(".*\\s", "");
                    newList.add(existing + charToAdd2);
//                    for (int k = 0; k < alphabet2.size(); k++) {
//                        String alph3 = alphabet2.get(k);
//                        if (alph3.endsWith("_v")) {
//                            continue;
//                        }
//                        String charToAdd3 = alph3.replaceAll(".*\\s", "");
//                        newList.add(existing + charToAdd2 + charToAdd3);
//                    }

                }
            }
            oldList.addAll(newList);

            //map.put(charToAdd, oldList);
            System.out.println(charToAdd + " " + oldList);
        }
        System.out.println(map);
        return map;
    }

    public static Map<String, String> getFileContentAsMapAccuratExp1(String aFileName) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(aFileName)), "UTF-8"));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                String values[] = str.split("\\s");
                if (values.length >= 2) {
                    map.put(values[0].trim().toLowerCase(), values[1].trim().replaceAll("ï»¿", "").toLowerCase().trim());
                }
            }
        }
        in.close();
        return map;
    }

    public static Map<String, Integer> getAlignment(String aFileName) throws IOException {
        Map<String, Integer> map = new HashMap<String, Integer>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(aFileName)), "UTF-8"));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                Integer count = map.get(str.trim().toLowerCase());
                if (count == null) {
                    count = new Integer(0);
                }
                count = new Integer(count.intValue() + 1);
                map.put(str.trim().toLowerCase(), count);
            }
        }
        in.close();
        return map;
    }

    public static Map<String, Map<String, String>> getDictionary(String aFileName) throws IOException {
        Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(aFileName)), "UTF-8"));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                String values[] = str.split("\\s");
                if (values.length >= 2) {
                    String key = values[0].toLowerCase().trim().replaceAll("ï»¿", "").trim();
                    Map<String, String> valueList = map.get(key);
                    if (valueList == null) {
                        valueList = new HashMap<String, String>();
                    }
                    valueList.put(values[1].trim().toLowerCase(), "1");
                    map.put(key, valueList);
                }
            }
        }
        in.close();
        return map;
    }

    public static Map<String, Integer> getDictionary2(String aFileName, Map<String, String> enVcb, Map<String, String> roVcb) throws IOException {
        Map<String, Integer> map = new HashMap<String, Integer>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(aFileName)), "UTF-8"));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                String values[] = str.split("\\s");
                if (values.length >= 2) {

                    String key = values[0].toLowerCase().replaceAll("ï»¿", "").trim();
                    String key1 = values[0].toLowerCase().replaceAll("ï»¿", "").trim();
                    String romanWord = roVcb.get(key);
                    String enWord = enVcb.get(key1);
                    String pair = enWord + " " + romanWord;
                    Integer valueList = map.get(pair);
                    if (valueList == null) {
                        valueList = new Integer(0);
                    }
                    valueList = new Integer(valueList.intValue() + 1);
                    map.put(pair, valueList);
                }
            }
        }
        in.close();
        return map;
    }

    public static Map<String, String> getFileContentAsMapForPattern(String aFileName, String aDivider) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim()) && str.contains("#")) {
                String values[] = str.split(aDivider);
                if (values.length == 2) {
                    if (values[1].contains("space")) {
                        values[1] = " ";
                    }
                    map.put(values[0].trim().replaceAll("ï»¿", "").trim(), values[1].trim());
                }
            }
        }
        in.close();
        return map;
    }

    public static Map<String, Integer> getFileContentAsMapInt(String aFileName, String aDivider) throws IOException {
        Map<String, Integer> map = new HashMap<String, Integer>();
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                String values[] = str.split(aDivider);
                if (values[1].contains("space")) {
                    values[1] = " ";
                }
                map.put(values[0].trim().replaceAll("ï»¿", "").trim(), Integer.parseInt(values[1].trim()));
            }
        }
        in.close();
        return map;
    }

    public static Map<String, String> getFileContentAsMapInLowerCase(String aFileName, String aDivider) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                String values[] = str.split(aDivider);
                map.put(values[0].trim().toLowerCase().replaceAll("ï»¿", "").trim(), values[1].trim().toLowerCase());
            }
        }
        in.close();
        return map;
    }

    public static Map<String, String> getFileContentAsMapInLowerCaseLocation(String aFileName, String aDivider, Map<String, String> mapFirstName, Map<String, String> mapLastName) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                String values[] = str.split(aDivider);
                if (mapFirstName.get(values[0].trim().toLowerCase()) == null && mapLastName.get(values[0].trim().toLowerCase()) == null) {
                    map.put(values[0].trim().toLowerCase().replaceAll("ï»¿", "").trim(), values[1].trim().toLowerCase());
                }
            }
        }
        in.close();
        return map;
    }

    public static Map<String, String> getFileContentAsMapInLowerCaseExpand(String aFileName, Map<String, String> map, Map<String, String> mapFirstName, Map<String, String> mapLastName) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                str = str.replaceAll("POINT.*", "");
                String values[] = str.split(";");
                if (mapFirstName.get(values[0].trim().toLowerCase()) == null && mapLastName.get(values[0].trim().toLowerCase()) == null) {
                    map.put(values[0].trim().toLowerCase().replaceAll("ï»¿", "").trim(), values[0].trim().toLowerCase());
                }
                if (mapFirstName.get(values[1].trim().toLowerCase()) == null && mapLastName.get(values[1].trim().toLowerCase()) == null) {
                    map.put(values[1].trim().toLowerCase().replaceAll("ï»¿", "").trim(), values[1].trim().toLowerCase());
                }
            }
        }
        in.close();
        return map;
    }

    public static Map<String, String> getFileContentAsMapInLowerCaseForNames(String aFileName, String aDivider) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                String values[] = str.split(aDivider);
                map.put(values[0].trim().toLowerCase().replaceAll("ï»¿", "").trim(), values[0].trim().toLowerCase());
            }
        }
        in.close();
        return map;
    }

    public static Map<String, String> getFileContentAsMapInLowerCaseForNamesExpand(String aFileName, int index, Map<String, String> map) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                String values[] = str.split(",");
                map.put(values[index].trim().toLowerCase().replaceAll("ï»¿", "").trim(), values[index].trim().toLowerCase());
            }
        }

        in.close();
        return map;
    }

    public static Map<String, String> getFileContentAsMapConsiderDuplicates(String aFileName, String aDivider) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                String values[] = str.split(aDivider);
                if (!values[1].contains("#")) {
                    continue;
                }
                if (map.containsKey(values[0].trim())) {
                    String value = map.get(values[0].trim());
                    value = value.replaceAll("#.*", "").trim();
                    String value2 = values[1].replaceAll("#.*", "").trim();
                    String type = values[1].replaceAll(".*#", "").trim();
                    Integer count = new Integer(Integer.parseInt(value) + Integer.parseInt(value2));
                    map.put(values[0].trim().replaceAll("ï»¿", "").trim(), count.toString() + "#" + type);
                } else {
                    map.put(values[0].trim().replaceAll("ï»¿", "").trim(), values[1].trim());
                }
            }
        }
        in.close();
        return map;
    }

    public static Map<String, String> getFileContentAsMapTakeUnderScoreAway(String aFileName, String aDivider) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                String posTag = str.replaceAll(".*_", "").replaceAll("=.*", "");
                if (posTag.startsWith("V")) {
                    String values[] = str.split(aDivider);
                    map.put(values[0].trim().replaceAll("_.*", "").replaceAll("ï»¿", "").trim(), values[1].trim());
                }
            }
        }
        in.close();
        return map;
    }

    public static Map<String, Integer> getFileContentAsMapInIntValue(String aFileName, String aDivider) throws IOException {
        Map<String, Integer> map = new HashMap<String, Integer>();
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            if (!"".equals(str.trim())) {
                String values[] = str.split(aDivider);
                try {
                    map.put(values[0].trim(), new Integer(values[1].trim()));
                } catch (Exception e) {
                }
            }
        }
        in.close();
        return map;
    }

    public static Map<String, String> getFileContentAsMap(String aFileName, String aDivider, String aValue) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str;
        while ((str = in.readLine()) != null) {
            String values[] = str.trim().split(aDivider);
            for (int i = 0; i < values.length; i++) {
                map.put(values[i].trim(), aValue);
            }
        }
        in.close();
        return map;
    }

    public static StringBuffer getFileContentAsBuffer(String aFileName) throws IOException {
        StringBuffer buffer = new StringBuffer();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(aFileName)), "UTF-8"));
        } catch (Exception e) {
            in = new BufferedReader(new FileReader(aFileName));
        }
        String str;
        while ((str = in.readLine()) != null) {
            buffer.append(str.trim()).append(" \n");
        }
        in.close();
        return buffer;
    }

    public static StringBuffer getFileContentAsBufferWithoutBreak(String aFileName) throws IOException {
        StringBuffer buffer = new StringBuffer();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(aFileName)), "UTF-8"));
        } catch (Exception e) {
            in = new BufferedReader(new FileReader(aFileName));
        }
        String str;
        while ((str = in.readLine()) != null) {
            buffer.append(str.trim()).append(" ");
        }
        in.close();
        return buffer;
    }

    public static Map<String, String> getContentAsMap(Vector<String> aList) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < aList.size(); i++) {
            String line = aList.get(i);
            String entries[] = line.split("=");
            map.put(entries[0], entries[1]);
        }
        return map;
    }

    public static Vector<String> getFileContentAsSingleWords(String aFileName) throws IOException {
        Vector<String> toponyms = new Vector<String>();
        Vector<String> lines = getFileContentAsVector(aFileName);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).toString();
            String lineArray[] = line.split(",");
            for (int j = 0; j < lineArray.length; j++) {
                String word = lineArray[j];
                if (!"".equals(word.trim()) && !toponyms.contains(word.trim())) {
                    toponyms.add(word.trim());
                }
            }
        }
        return toponyms;
    }

    /**
     * Saves the text saved in the StringBuffer object to the given fileName
     *
     * @param aFileName: java.lang.String
     * @param aStringToSave: java.lang.Strin
     * @throws IOException
     */
    public static void doSave(String aFileName, String aStringToSave) throws IOException {
        File file = new File(aFileName);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fos = new FileOutputStream(aFileName);
        fos.write(aStringToSave.getBytes());
        fos.flush();
        fos.close();
    }

    /**
     * Saves the text saved in the StringBuffer object to the given fileName
     *
     * @param aFileName: java.lang.String
     * @param aStringToSave: java.lang.Strin
     * @throws IOException
     */
    public static void doSave(String aFileName, StringBuffer aStringToSave) throws IOException {
        doSave(aFileName, aStringToSave.toString());
    }

    /**
     * Saves the text saved in the StringBuffer object to the given fileName
     *
     * @param aFileName: java.lang.String
     * @param aStringToSave: java.lang.Strin
     * @throws IOException
     */
    public static void doSaveUTF2(String aFileName, String aStringToSave) throws IOException {
        try {
            File file = new File(aFileName);
            if (file.exists()) {
                file.delete();
            }
            PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF-8"));
            out.write(aStringToSave);
            out.flush();
            out.close();
        } catch (Exception e) {
            doSave(aFileName, aStringToSave);
        }
    }

    public static void doSaveUTF(String aFileName, String aStringToSave) throws IOException {
        try {
            doSaveUTF2(aFileName, aStringToSave);
        } catch (Exception e) {
            File file = new File(aFileName);
            if (file.exists()) {
                file.delete();
            }
            DataOutputStream stream = new DataOutputStream(new FileOutputStream(aFileName));
            stream.writeUTF(aStringToSave);
            stream.flush();
            stream.close();
        }
    }

    public static void doSave1(String aFileNameToSave, Map<String, StringBuffer> aMap, String aSeparator) throws IOException {
        Set<String> keys = aMap.keySet();
        Iterator<String> it = keys.iterator();
        StringBuffer buffer = new StringBuffer();
        while (it.hasNext()) {
            String key = it.next();
            buffer.append(key).append(aSeparator).append(aMap.get(key)).append("\n");
        }
        doSave(aFileNameToSave, buffer.toString());
        buffer = null;
    }

    public static void doSave3(String aFileNameToSave, Map<String, Object> aMap, String aSeparator) throws IOException {
        Set<String> keys = aMap.keySet();
        Iterator<String> it = keys.iterator();
        StringBuffer buffer = new StringBuffer();
        while (it.hasNext()) {
            String key = it.next();
            buffer.append(key).append(aSeparator).append(aMap.get(key)).append("\n");
        }
        doSave(aFileNameToSave, buffer.toString());
        buffer = null;
    }

    public static void doSave4(String aFileNameToSave, Map<String, String> aMap, String aSeparator) throws IOException {
        Set<String> keys = aMap.keySet();
        Iterator<String> it = keys.iterator();
        StringBuffer buffer = new StringBuffer();
        while (it.hasNext()) {
            String key = it.next();
            buffer.append(key).append(aSeparator).append(aMap.get(key)).append("\n");
        }
        doSave(aFileNameToSave, buffer.toString());
        buffer = null;
    }

    public static void doSave2(String aFileNameToSave, Map<String, Integer> aMap, String aSeparator) throws IOException {
        Object keys[] = aMap.keySet().toArray();
        StringBuffer tf = new StringBuffer();
        for (int i = 0; i < keys.length; i++) {
            tf.append(keys[i]).append(aSeparator).append(aMap.get(keys[i]).intValue()).append("\n");
            aMap.remove(keys[i]);
        }
        doSave(aFileNameToSave, tf.toString());
        tf = null;
    }

    public static void doSave6(String aFileNameToSave, Map<String, Integer> aMap) throws IOException {
        Object keys[] = aMap.keySet().toArray();
        StringBuffer tf = new StringBuffer();
        for (int i = 0; i < keys.length; i++) {
            tf.append(keys[i]).append("\n");
            aMap.remove(keys[i]);
        }
        doSaveUTF(aFileNameToSave, tf.toString());
        tf = null;
    }

    public static void doSave7(String aFileNameToSave, Map<String, String> aMap) throws IOException {
        Object keys[] = aMap.keySet().toArray();
        StringBuffer tf = new StringBuffer();
        for (int i = 0; i < keys.length; i++) {
            tf.append(keys[i]).append("\n");
            aMap.remove(keys[i]);
        }
        doSaveUTF(aFileNameToSave, tf.toString());
        tf = null;
    }

    public static void doSave2UTF(String aFileNameToSave, Map<String, Integer> aMap, String aSeparator) throws IOException {
        Object keys[] = aMap.keySet().toArray();
        StringBuffer tf = new StringBuffer();
        for (int i = 0; i < keys.length; i++) {
            tf.append(keys[i]).append(aSeparator).append(aMap.get(keys[i]).intValue()).append("\n");
            aMap.remove(keys[i]);
        }
        doSaveUTF(aFileNameToSave, tf.toString());
        tf = null;
    }

    public static void doSave5(String aFileNameToSave, Map<String, Double> aMap, String aSeparator) throws IOException {
        Object keys[] = aMap.keySet().toArray();
        StringBuffer tf = new StringBuffer();
        for (int i = 0; i < keys.length; i++) {
            tf.append(keys[i]).append(aSeparator).append(aMap.get(keys[i]).doubleValue()).append("\n");
        }
        doSave(aFileNameToSave, tf.toString());
        tf = null;
    }

    public static void doSave5UTF(String aFileNameToSave, Map<String, Double> aMap, String aSeparator) throws IOException {
        Object keys[] = aMap.keySet().toArray();
        StringBuffer tf = new StringBuffer();
        for (int i = 0; i < keys.length; i++) {
            tf.append(keys[i]).append(aSeparator).append(aMap.get(keys[i]).doubleValue()).append("\n");
        }
        doSaveUTF(aFileNameToSave, tf.toString());
        tf = null;
    }

    public static void doSave8(String aFileNameToSave, Map<String, Integer> aMap, String aSeparator) throws IOException {
        Object keys[] = aMap.keySet().toArray();
        StringBuffer tf = new StringBuffer();
        for (int i = 0; i < keys.length; i++) {
            tf.append(keys[i]).append(aSeparator).append(aMap.get(keys[i]).doubleValue()).append("\n");
        }
        doSave(aFileNameToSave, tf.toString());
        tf = null;
    }

    public static StringBuffer getMapInStringBuffer(Map<String, Integer> aMap, String aSeparator) throws IOException {
        Object keys[] = aMap.keySet().toArray();
        StringBuffer tf = new StringBuffer();
        for (int i = 0; i < keys.length; i++) {
            tf.append(keys[i]).append(aSeparator).append(aMap.get(keys[i]).intValue()).append("\n");
            aMap.remove(keys[i]);
        }
        //doSaveUTF2(aFileNameToSave, tf.toString());
        //tf = null;
        return tf;
    }

    public static void doSave2NotRemover(String aFileNameToSave, Map<String, Integer> aMap, String aSeparator) throws IOException {
        Object keys[] = aMap.keySet().toArray();
        StringBuffer tf = new StringBuffer();
        for (int i = 0; i < keys.length; i++) {
            tf.append(keys[i]).append(aSeparator).append(aMap.get(keys[i]).intValue()).append("\n");
        }
        doSave(aFileNameToSave, tf.toString());
        tf = null;
    }

    public static Map sortByValue(Map map, final boolean aGreaterToSmall) {
        List<String> list = new LinkedList<String>(map.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                if (aGreaterToSmall) {
                    return ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue());
                } else {
                    return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
                }
            }
        });
        Map<Object, Object> result = new LinkedHashMap<Object, Object>();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry<Object, Object> entry = (Map.Entry) it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static Map sortByValueDeleteAfter100(Map map, final boolean aGreaterToSmall, int filterAfter) {
        List<String> list = new LinkedList<String>(map.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                if (aGreaterToSmall) {
                    return ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue());
                } else {
                    return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
                }
            }
        });
        Map<Object, Object> result = new LinkedHashMap<Object, Object>();
        int i = 0;
        for (Iterator it = list.iterator(); it.hasNext() && i < filterAfter;) {
            Map.Entry<Object, Object> entry = (Map.Entry) it.next();
            result.put(entry.getKey(), entry.getValue());
            i++;
        }

        return result;
    }

    public static Map sortByKey(Map map, final boolean aGreaterToSmall) {
        List<String> list = new LinkedList<String>(map.keySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                if (aGreaterToSmall) {
                    return ((Comparable) ((o2))).compareTo((o1));
                } else {
                    return ((Comparable) ((o1))).compareTo(((o2)));
                }
            }
        });
        Map<Object, Object> result = new LinkedHashMap<Object, Object>();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Object key = it.next();
            result.put(key, map.get(key));
        }
        return result;
    }

    public static void sort(Vector<Long> list, final boolean aGreaterToSmall) {
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                if (aGreaterToSmall) {
                    return ((Comparable) ((o2))).compareTo((o1));
                } else {
                    return ((Comparable) ((o1))).compareTo(((o2)));
                }
            }
        });
    }

    public static void sortDouble(Vector<Double> list, final boolean aGreaterToSmall) {
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                if (aGreaterToSmall) {
                    return ((Comparable) ((o2))).compareTo((o1));
                } else {
                    return ((Comparable) ((o1))).compareTo(((o2)));
                }
            }
        });
    }

    public static void sortInt(Vector<Integer> list, final boolean aGreaterToSmall) {
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                if (aGreaterToSmall) {
                    return ((Comparable) ((o2))).compareTo((o1));
                } else {
                    return ((Comparable) ((o1))).compareTo(((o2)));
                }
            }
        });
    }

    /**
     * Copies aFromFile to aToFile
     *
     * @param aFromFile
     * @param aToFile
     * @throws IOException
     */
    public static void copy(File aFromFile, File aToFile) throws IOException {
        FileChannel srcChannel = new FileInputStream(aFromFile).getChannel();
        FileChannel dstChannel = new FileOutputStream(aToFile).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
    }

    public static Properties readPropertyFile(String aFileName) throws FileNotFoundException, IOException {
        Properties properties = new Properties();
        properties.load(new InputStreamReader(new FileInputStream(new File(aFileName)), "UTF-8"));
        return properties;
    }

    public static Properties createPropertyNonProperPropertyFile(String aFileName, String aSeparator) throws IOException {
        Properties properties = new Properties();
        BufferedReader in = new BufferedReader(new FileReader(aFileName));
        String str = in.readLine();
        properties.setProperty("total", str.trim());
        while ((str = in.readLine()) != null) {
            String array[] = str.split(aSeparator);
            properties.setProperty(array[0].trim(), array[1].trim());
        }
        in.close();
        return properties;
    }

    public static void writePropertyFile(String aFileNameToSave, Properties aPropertyFile) throws FileNotFoundException, IOException {
        aPropertyFile.store(new FileOutputStream(aFileNameToSave), null);
    }
//    public static void main(String[] args) throws IOException {
//		Util.doSaveUTF("testSaveDoSaveUTF.txt", "Belém Tower");
//		Util.doSave("testSaveDoSave.txt", "Belém Tower");
//		Util.doSaveUTF("testSaveDoSaveUTFAfterReading", Util.getFileContentAsBuffer("testSaveDoSaveUTF.txt").toString());
//	}
}

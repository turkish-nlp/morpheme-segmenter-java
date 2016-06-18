package test;

import tries.TrieST;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by ahmet on 18.06.2016.
 */
public class FreqTest {

    public static void main(String[] args) throws IOException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(args[0]));

        Map<String, Integer> nodeList = new TreeMap<>();
        Map<String, Integer> morphemeFreq = new TreeMap<>();

        String line;
        while ((line = reader.readLine()) != null) {
            String space = " ";
            StringTokenizer st = new StringTokenizer(line, space);

            String word = st.nextToken();
            int freq = Integer.parseInt(st.nextToken());

            nodeList.put(word, freq);
        }

        Set<String> boundaries = new TreeSet<>();

        for (String s : nodeList.keySet()) {
            if (nodeList.get(s) >= 3) {
                boundaries.add(s);
            }
        }

        calcuateFrequency(nodeList, boundaries, morphemeFreq);

        for (String s : morphemeFreq.keySet()) {
            System.out.println(s + " --> " + morphemeFreq.get(s));
        }
    }

    private static void calcuateFrequency(Map<String, Integer> nodeList, Set<String> boundaries, Map<String, Integer> morphemeFreq) {

        Set<String> wordList = new TreeSet<>();

        for (String boundary : boundaries) {
            nodeList.put(boundary + "$", 1);
        }

        for (String node : nodeList.keySet()) {
            if (node.endsWith("$")) {
                String current = "";
                boolean found = false;
                for (String boundary : boundaries) {
                    if (node.startsWith(boundary) && !node.equals(boundary + "$")) {
                        current = boundary;
                        found = true;
                    }
                }

                String morpheme = node.substring(current.length(), node.length() - 1);

                if (morphemeFreq.containsKey(morpheme)) {
                    morphemeFreq.put(morpheme, morphemeFreq.get(morpheme) + 1);
                } else {
                    morphemeFreq.put(morpheme, 1);
                }
            }
        }
    }

}

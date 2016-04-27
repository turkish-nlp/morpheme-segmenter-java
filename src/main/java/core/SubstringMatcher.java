package core;

/**
 * Created by ahmetu on 25.04.2016.
 */

import java.io.*;
import java.util.*;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.jboss.logging.Logger;

public class SubstringMatcher {

    private Map<String, Integer> segments = new TreeMap<String, Integer>();
    private Map<String, Integer> affixes = new TreeMap<String, Integer>();
    private Map<String, Integer> results = new TreeMap<String, Integer>();

    private String fileSegmentationInput;

    private WordVectors vectors;

    public Map<String, Integer> getSegments() {
        return segments;
    }

    public Map<String, Integer> getAffixes() {
        return affixes;
    }

    public Map<String, Integer> getResults() {
        return results;
    }

    public SubstringMatcher(String fileVectorInput, String fileSegmentationInput) {
        try {
            vectors = WordVectorSerializer.loadTxtVectors(new File(fileVectorInput));
            this.fileSegmentationInput = fileSegmentationInput;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void findMostFrequentLongestSubsequence(String word, int freq, int numberOfneighboors) {
        try {

            Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
            boolean notFound = false;

            if (neighboors.isEmpty()){
                notFound = true;
            }

            int max_f = 0;
            String segment = word;
            String affix = "NLL";

            // In order to limit the control lenght limit; i<(word.lenght()-limit+1) can be used.
            for (int i = 0; i < word.length()-2; i++) {

                if (notFound){
                    break;
                }

                int f = 0;
                for (String n : neighboors) {
                    if (n.startsWith(word.substring(0, word.length() - i))) {
                        f++;
                    }
                }
                if (f > max_f) {
                    max_f = f;
                    segment = word.substring(0, word.length() - i);
                    affix = word.substring(word.length() - i, word.length());
                }
            }

            if (segments.containsKey(segment)) {
                segments.put(segment, segments.get(segment) + freq);
            } else {
                segments.put(segment, freq);
            }

            if (affixes.containsKey(affix)) {
                affixes.put(affix, affixes.get(affix) + freq);
            } else {
                affixes.put(affix, freq);
            }

            String result = segment + "+" + affix;
            if (results.containsKey(result)) {
                results.put(result, results.get(result) + freq);
            } else {
                results.put(result, freq);
            }

        } catch (Exception ex) {
            Logger.getLogger(SubstringMatcher.class).log(Logger.Level.ERROR, ex.getLocalizedMessage());
        }
    }

    public void findSegmentsAndAffixes() {
        try {

            BufferedReader reader = null;
            reader = new BufferedReader(new FileReader(fileSegmentationInput));

            String line;
            while ((line = reader.readLine()) != null) {
                String space = " ";
                StringTokenizer st = new StringTokenizer(line, space);

                int freq = Integer.parseInt(st.nextToken());
                String word = st.nextToken();

                findMostFrequentLongestSubsequence(word, freq, 20);

            }
        } catch (Exception ex) {
            Logger.getLogger(SubstringMatcher.class).log(Logger.Level.ERROR, ex.getLocalizedMessage());
        }
    }

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        SubstringMatcher ssm = new SubstringMatcher(args[0], args[1]);
        ssm.findSegmentsAndAffixes();

        Map<String, Integer> s = ssm.getSegments();
        Map<String, Integer> a = ssm.getAffixes();
        Map<String, Integer> r = ssm.getResults();

        PrintWriter writer_seg = new PrintWriter("outputs/segments", "UTF-8");
        PrintWriter writer_af = new PrintWriter("outputs/affixes", "UTF-8");
        PrintWriter writer_res = new PrintWriter("outputs/results", "UTF-8");

        for (Map.Entry<String,Integer> entry : s.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_seg.println(line);
        }
        writer_seg.close();

        for (Map.Entry<String,Integer> entry : a.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_af.println(line);
        }
        writer_af.close();

        for (Map.Entry<String,Integer> entry : r.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_res.println(line);
        }
        writer_seg.close();
        writer_af.close();
        writer_res.close();

    }
}

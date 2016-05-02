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

    private Map<String, Double> stems = new TreeMap<String, Double>();
    private Map<String, Double> affixes = new TreeMap<String, Double>();
    private Map<String, Double> results = new TreeMap<String, Double>();

    private String fileSegmentationInput;

    private WordVectors vectors;

    public Map<String, Double> getStems() {
        return stems;
    }

    public Map<String, Double> getAffixes() {
        return affixes;
    }

    public Map<String, Double> getResults() {
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

    private void findMostFrequentLongestSubsequence(String word, double freq, int numberOfneighboors) {
        try {

            Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
            boolean notFound = false;

            if (neighboors.isEmpty()) {
                notFound = true;
            }

            int max_f = 0;
            String segment = word;
            String affix = "NLL";

            // In order to limit the control lenght limit; i<(word.lenght()-limit+1) can be used.
            for (int i = 0; i < word.length() - 2; i++) {

                if (notFound) {
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

            if (stems.containsKey(segment)) {
                stems.put(segment, stems.get(segment) + freq);
            } else {
                stems.put(segment, freq);
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

    private void findSegmentsAndAffixes() {
        try {

            BufferedReader reader = null;
            reader = new BufferedReader(new FileReader(fileSegmentationInput));

            String line;
            while ((line = reader.readLine()) != null) {
                String space = " ";
                StringTokenizer st = new StringTokenizer(line, space);

                double freq = Double.parseDouble(st.nextToken());
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

        Map<String, Double> s = ssm.getStems();
        Map<String, Double> a = ssm.getAffixes();
        Map<String, Double> r = ssm.getResults();

        PrintWriter writer_seg = new PrintWriter("outputs/stems", "UTF-8");
        PrintWriter writer_af = new PrintWriter("outputs/affixes", "UTF-8");
        PrintWriter writer_res = new PrintWriter("outputs/results", "UTF-8");

        for (Map.Entry<String, Double> entry : s.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_seg.println(line);
        }
        writer_seg.close();

        for (Map.Entry<String, Double> entry : a.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_af.println(line);
        }
        writer_af.close();

        for (Map.Entry<String, Double> entry : r.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_res.println(line);
        }
        writer_seg.close();
        writer_af.close();
        writer_res.close();

    }
}

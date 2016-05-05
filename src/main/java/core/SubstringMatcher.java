package core;

/**
 * Created by ahmetu on 25.04.2016.
 */

import java.io.*;
import java.util.*;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.jboss.logging.Logger;
import tree.MorphemeGraph;

public class SubstringMatcher {

    private Map<String, Double> stems = new TreeMap<String, Double>();
    private Map<String, Double> affixes = new TreeMap<String, Double>();
    private Map<String, Double> results = new TreeMap<String, Double>();
    private Map<String, MorphemeGraph> graphList = new TreeMap<>();

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

    public Map<String, MorphemeGraph> getGraphList() {
        return graphList;
    }

    public SubstringMatcher(String fileVectorInput, String fileSegmentationInput) throws FileNotFoundException {
        vectors = WordVectorSerializer.loadTxtVectors(new File(fileVectorInput));
        this.fileSegmentationInput = fileSegmentationInput;
    }

    private void findMostFrequentLongestSubsequence(String word, double freq, int numberOfneighboors) {

        Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
        boolean notFound = false;

        if (neighboors.isEmpty()) {
            notFound = true;
        }

        int max_f = 0;
        String stem = word;
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
                stem = word.substring(0, word.length() - i);
                affix = word.substring(word.length() - i, word.length());
            }
        }

        MorphemeGraph graph = new MorphemeGraph(stem, vectors);

        String suffixFar = "";
        String suffixClose = "";
        for (String n : neighboors) {

            if (word.equals(stem)) {

                if (n.startsWith(stem)) {

                    graph.add(n, freq);

                    suffixClose = n.substring(stem.length());

                    if (affixes.containsKey(suffixClose)) {
                        affixes.put(suffixClose, affixes.get(suffixClose) + freq);
                    } else {
                        affixes.put(suffixClose, freq);
                    }
                }

            } else {

                graph.add(word, freq);

                if (n.startsWith(word)) {

                    graph.add(n, freq);

                    suffixFar = n.substring(word.length());
                    suffixClose = n.substring(stem.length(), word.length());

                    if (affixes.containsKey(suffixClose)) {
                        affixes.put(suffixClose, affixes.get(suffixClose) + freq);
                    } else {
                        affixes.put(suffixClose, freq);
                    }

                    if (affixes.containsKey(suffixFar)) {
                        affixes.put(suffixFar, affixes.get(suffixFar) + freq);
                    } else {
                        affixes.put(suffixFar, freq);
                    }
                } else if (n.startsWith(stem)) {

                    graph.add(n, freq);

                    suffixClose = n.substring(stem.length());

                    if (affixes.containsKey(suffixClose)) {
                        affixes.put(suffixClose, affixes.get(suffixClose) + freq);
                    } else {
                        affixes.put(suffixClose, freq);
                    }
                }
            }
        }


        if (stems.containsKey(stem)) {
            stems.put(stem, stems.get(stem) + freq);
        } else {
            stems.put(stem, freq);
        }

        /*
        if (affixes.containsKey(affix)) {
            affixes.put(affix, affixes.get(affix) + freq);
        } else {
            affixes.put(affix, freq);
        }
        */

        String result = stem + "+" + affix;
        if (results.containsKey(result)) {
            results.put(result, results.get(result) + freq);
        } else {
            results.put(result, freq);
        }
        graph.print();
        graphList.put(word, graph);

    }

    private void findSegmentsAndAffixes() throws IOException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(fileSegmentationInput));

        String line;
        while ((line = reader.readLine()) != null) {
            String space = " ";
            StringTokenizer st = new StringTokenizer(line, space);

            double freq = Double.parseDouble(st.nextToken());
            String word = st.nextToken();

            findMostFrequentLongestSubsequence(word, freq, 50);

        }
    }

    public static void main(String[] args) throws IOException {
        SubstringMatcher ssm = new SubstringMatcher(args[0], "outputs/substringtest.txt");
        ssm.findSegmentsAndAffixes();

        /*
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
        */
    }
}

package core;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.jboss.logging.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * Created by ahmetu on 29.04.2016.
 */
public class NestedSegmenter {

    private Map<String, Double> stems = new HashMap<>();
    private Map<String, Double> affixes = new HashMap<>();
    private Map<String, Double> results = new HashMap<>();
    private Map<String, Double> notFound = new HashMap<>();

    private String fileSegmentationInput;

    private WordVectors vectors;

    public void setVectors(WordVectors vectors) {
        this.vectors = vectors;
    }

    public Map<String, Double> getStems() {
        return stems;
    }

    public Map<String, Double> getAffixes() {
        return affixes;
    }

    public Map<String, Double> getResults() {
        return results;
    }

    public Map<String, Double> getNotFound() {
        return notFound;
    }

    public NestedSegmenter(String fileVectorInput, String fileSegmentationInput) throws FileNotFoundException {
        vectors = WordVectorSerializer.loadTxtVectors(new File(fileVectorInput));
        this.fileSegmentationInput = fileSegmentationInput;
    }

    private void doNested(String word, double frequency, double treshold) {

        Stack<String> localSuffixes = new Stack<String>();
        String stem = word;

        if (!vectors.hasWord(word)) {
            if (notFound.containsKey(word)) {
                notFound.put(word, notFound.get(word) + frequency);
            } else {
                notFound.put(word, frequency);
            }
        } else {
            if (word.length() < 3) {
                if (stems.containsKey(word)) {
                    stems.put(word, stems.get(word) + frequency);
                } else {
                    stems.put(word, frequency);
                }
            } else {
                int count = 0;
                for (int i = 0; i < word.length() - 2; i++) {
                    String candidate = stem.substring(0, stem.length() - count);
                    double cosine = vectors.similarity(stem, candidate);
                    if (cosine > treshold && cosine < 1d) {
                        String affix = stem.substring(stem.length() - count, stem.length());

                        localSuffixes.push(affix);

                        if (affixes.containsKey(affix)) {
                            affixes.put(affix, affixes.get(affix) + frequency);
                        } else {
                            affixes.put(affix, frequency);
                        }

                        stem = candidate;
                        count = 0;
                    }
                    count = count + 1;
                }

                if (stems.containsKey(stem)) {
                    stems.put(stem, stems.get(stem) + frequency);
                } else {
                    stems.put(stem, frequency);
                }

                String result = stem;
                int suffixNo = localSuffixes.size();
                for (int j = 0; j < suffixNo; j++) {
                    result = result + "+" + localSuffixes.pop();
                }

                if (results.containsKey(result)) {
                    results.put(result, results.get(result) + frequency);
                } else {
                    results.put(result, frequency);
                }

                /*
                * Results keeps the word and its segmentation above line.
                *
                results.put(word, result);
                */
            }
        }
    }

    public void findSegmentsAndAffixes() throws IOException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(fileSegmentationInput));

        String line;
        while ((line = reader.readLine()) != null) {
            String space = " ";
            StringTokenizer st = new StringTokenizer(line, space);

            double freq = Double.parseDouble(st.nextToken());
            String word = st.nextToken();

            doNested(word, freq, 0.25);
        }
    }

    public static void main(String[] args) throws IOException {
        NestedSegmenter ns = new NestedSegmenter(args[0], args[1]);
        ns.findSegmentsAndAffixes();

        Map<String, Double> s = ns.getStems();
        Map<String, Double> a = ns.getAffixes();
        Map<String, Double> r = ns.getResults();
        Map<String, Double> n = ns.getNotFound();

        PrintWriter writer_seg = new PrintWriter("C:\\Users\\ahmetu\\Desktop\\Morphology Projects\\cleared_result\\stems", "UTF-8");
        PrintWriter writer_af = new PrintWriter("C:\\Users\\ahmetu\\Desktop\\Morphology Projects\\cleared_result\\affixes", "UTF-8");
        PrintWriter writer_res = new PrintWriter("C:\\Users\\ahmetu\\Desktop\\Morphology Projects\\cleared_result\\results", "UTF-8");
        PrintWriter writer_noF = new PrintWriter("C:\\Users\\ahmetu\\Desktop\\Morphology Projects\\cleared_result\\absent", "UTF-8");

        for (Map.Entry<String, Double> entry : s.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_seg.println(line);
        }

        for (Map.Entry<String, Double> entry : a.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_af.println(line);
        }

        for (Map.Entry<String, Double> entry : r.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_res.println(line);
        }

        for (Map.Entry<String, Double> entry : n.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_noF.println(line);
        }

        writer_seg.close();
        writer_af.close();
        writer_res.close();
        writer_noF.close();
    }
}

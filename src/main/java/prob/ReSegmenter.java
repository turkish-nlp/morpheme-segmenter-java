package prob;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by ahmetu on 04.05.2016.
 */
public class ReSegmenter {

    private Map<String, Double> stems;
    private Map<String, Double> affixes;

    private Map<String, Double> results;
    private Map<String, Double> notFound;

    private Map<String, Map<String, Double>> morphemeBiagramProbabilities;

    private String fileSegmentationInput;

    String startMorpheme = "STR";
    String endMorphmeme = "END";

    public Map<String, Double> getResults() {
        return results;
    }

    public Map<String, Double> getNotFound() {
        return notFound;
    }

    public void setStems(Map<String, Double> stems) {
        this.stems = stems;
    }

    public void setAffixes(Map<String, Double> affixes) {
        this.affixes = affixes;
    }

    public void setResults(Map<String, Double> results) {
        this.results = results;
    }

    public void setNotFound(Map<String, Double> notFound) {
        this.notFound = notFound;
    }

    public void setMorphemeBiagramProbabilities(Map<String, Map<String, Double>> morphemeBiagramProbabilities) {
        this.morphemeBiagramProbabilities = morphemeBiagramProbabilities;
    }

    public ReSegmenter(String fileSegmentationInput, Map<String, Double> stems, Map<String, Double> affixes, Map<String, Map<String, Double>> morphemeBiagramProbabilities) {
        this.fileSegmentationInput = fileSegmentationInput;
        this.stems = stems;
        this.affixes = affixes;
        results = new HashMap<>();
        notFound = new HashMap<>();
        this.morphemeBiagramProbabilities = morphemeBiagramProbabilities;
    }

    private void reSegment(String word, double frequency) {

        /*
        Prior information must be added to the production due to prevent undersegmentation.
         */

        List<String> segmentations = Utilities.getPossibleSegmentations(word, stems.keySet(), affixes.keySet());
        if (segmentations.isEmpty()) {
            if (notFound.containsKey(word)) {
                notFound.put(word, notFound.get(word) + frequency);
            } else {
                notFound.put(word, frequency);
            }
        } else {

            double max = Double.MIN_VALUE;
            String argmax = word;

            for (String segmentation : segmentations) {
                String seperator = "+";
                StringTokenizer st = new StringTokenizer(segmentation, seperator);

                String stem = st.nextToken();
                String curr = startMorpheme;
                String next = null;

                double probability = 1d;

                /*
                Production must be converted in logarithm domain
                 */
                while (st.hasMoreTokens()) {
                    next = st.nextToken();
                    probability = probability * morphemeBiagramProbabilities.get(curr).get(next);
                    curr = next;
                }

                next = endMorphmeme;
                probability = probability * morphemeBiagramProbabilities.get(curr).get(next);

                if (probability > max) {
                    max = probability;
                    argmax = segmentation;
                }
            }

            if (results.containsKey(argmax)) {
                results.put(argmax, results.get(argmax) + frequency);
            } else {
                results.put(argmax, frequency);
            }
        }
    }

    public void doItForFile() throws IOException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(fileSegmentationInput));

        String line;
        while ((line = reader.readLine()) != null) {
            String space = " ";
            StringTokenizer st = new StringTokenizer(line, space);

            double freq = Double.parseDouble(st.nextToken());
            String word = st.nextToken();

            reSegment(word, freq);
        }
    }
}

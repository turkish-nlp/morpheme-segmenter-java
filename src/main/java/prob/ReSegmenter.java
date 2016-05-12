package prob;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

import java.io.BufferedReader;
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
    private Map<String, Double> notFounds;

    private Map<String, Double> stemProbabilities;
    private Map<String, Map<String, Double>> morphemeBiagramProbabilities;

    private String fileSegmentationInput;

    MongoClient mongo;
    DB db;
    DBCollection collection;

    String startMorpheme = "STR";
    String endMorphmeme = "END";

    public Map<String, Double> getResults() {
        return results;
    }

    public Map<String, Double> getNotFounds() {
        return notFounds;
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

    public void setNotFounds(Map<String, Double> notFounds) {
        this.notFounds = notFounds;
    }

    public void setMorphemeBiagramProbabilities(Map<String, Map<String, Double>> morphemeBiagramProbabilities) {
        this.morphemeBiagramProbabilities = morphemeBiagramProbabilities;
    }

    public ReSegmenter(String fileSegmentationInput, Map<String, Double> stems, Map<String, Double> affixes, Map<String, Map<String, Double>> morphemeBiagramProbabilities, Map<String, Double> stemProbabilities) {
        this.fileSegmentationInput = fileSegmentationInput;
        this.stems = stems;
        this.affixes = affixes;
        this.stemProbabilities = stemProbabilities;
        this.morphemeBiagramProbabilities = morphemeBiagramProbabilities;

        results = new HashMap<>();
        notFounds = new HashMap<>();

    }

    public ReSegmenter(String fileSegmentationInput, Map<String, Double> stems, Map<String, Double> affixes, Map<String, Double> stemProbabilities, String collectionName) {
        this.fileSegmentationInput = fileSegmentationInput;
        this.stems = stems;
        this.affixes = affixes;
        this.stemProbabilities = stemProbabilities;

        mongo = new MongoClient("localhost", 27017);
        db = mongo.getDB("nlp-db");
        collection = db.getCollection(collectionName);

        results = new HashMap<>();
        notFounds = new HashMap<>();
    }

    public ReSegmenter(String fileSegmentationInput, Map<String, Double> stems, Map<String, Double> affixes, Map<String, Double> stemProbabilities,
                       Map<String, Double> results, Map<String, Double> notFounds, String collectionName) {
        this.fileSegmentationInput = fileSegmentationInput;
        this.stems = stems;
        this.affixes = affixes;
        this.stemProbabilities = stemProbabilities;

        mongo = new MongoClient("localhost", 27017);
        db = mongo.getDB("nlp-db");
        collection = db.getCollection(collectionName);

        this.results = results;
        this.notFounds = notFounds;
    }

    public void reSegmentWithMap(String word, double frequency, boolean withStemProbability) {

        /*
        ** Prior information must be added to the production due to prevent undersegmentation.
        ** Affix lenght can be used for prior with coefficient of n in the equation (1/29)^n
         */

        List<String> segmentations = Utilities.getPossibleSegmentations(word, stems.keySet(), affixes.keySet());
        if (segmentations.isEmpty()) {
            if (notFounds.containsKey(word)) {
                notFounds.put(word, notFounds.get(word) + frequency);
            } else {
                notFounds.put(word, frequency);
            }
        } else {

            double max = -1 * Double.MAX_VALUE;
            String argmax = word;

            for (String segmentation : segmentations) {
                String seperator = "+";
                StringTokenizer st = new StringTokenizer(segmentation, seperator);

                String stem = st.nextToken();
                String curr = startMorpheme;
                String next = null;

                double probability = 0d;
                if (withStemProbability) {
                    probability = probability + Math.log(stemProbabilities.get(stem));
                }

                while (st.hasMoreTokens()) {
                    next = st.nextToken();
                    probability = probability + Math.log(morphemeBiagramProbabilities.get(curr).get(next));
                    curr = next;
                }

                next = endMorphmeme;
                probability = probability + Math.log(morphemeBiagramProbabilities.get(curr).get(next));

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

    public void reSegmentWithDB(String word, double frequency, boolean withStemProbability) {

        /*
        ** Prior information must be added to the production due to prevent undersegmentation.
        ** Affix lenght can be used for prior with coefficient of n in the equation (1/29)^n
         */

        List<String> segmentations = Utilities.getPossibleSegmentations(word, stems.keySet(), affixes.keySet());
        if (segmentations.isEmpty()) {
            if (notFounds.containsKey(word)) {
                notFounds.put(word, notFounds.get(word) + frequency);
            } else {
                notFounds.put(word, frequency);
            }
        } else {

            double max = -1 * Double.MAX_VALUE;
            String argmax = word;

            for (String segmentation : segmentations) {
                String seperator = "+";
                StringTokenizer st = new StringTokenizer(segmentation, seperator);

                String stem = st.nextToken();
                String curr = startMorpheme;
                String next = null;

                double probability = 0d;
                if (withStemProbability) {
                    probability = probability + Math.log(stemProbabilities.get(stem));
                }

                while (st.hasMoreTokens()) {
                    next = st.nextToken();
                    probability = probability + Math.log(Utilities.getProbabilityForBigram(collection, curr, next));
                    curr = next;
                }

                next = endMorphmeme;
                probability = probability + Math.log(Utilities.getProbabilityForBigram(collection, curr, next));

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

    public void reSegmentWithDBforAllomorphs(String word, double frequency, boolean withStemProbability) {

        /*
        ** Prior information must be added to the production due to prevent undersegmentation.
        ** Affix lenght can be used for prior with coefficient of n in the equation (1/29)^n
         */

        List<String> segmentations = Utilities.getPossibleSegmentations(word, stems.keySet(), affixes.keySet());
        if (segmentations.isEmpty()) {
            if (notFounds.containsKey(word)) {
                notFounds.put(word, notFounds.get(word) + frequency);
            } else {
                notFounds.put(word, frequency);
            }
        } else {

            double max = -1 * Double.MAX_VALUE;
            String argmax = word;

            for (String segmentation : segmentations) {
                String seperator = "+";
                StringTokenizer st = new StringTokenizer(segmentation, seperator);

                String stem = st.nextToken();
                String curr = startMorpheme;
                String next = null;

                double probability = 0d;
                if (withStemProbability) {
                    probability = probability + Math.log(stemProbabilities.get(stem));
                }

                while (st.hasMoreTokens()) {

                    next = st.nextToken();
                    if (!next.equals("ken")) {
                        next = next.replaceAll("a|e|ı|i", "H");
                        next = next.replaceAll("t|d", "D");
                        next = next.replaceAll("c|ç", "C");
                        next = next.replaceAll("k|ğ", "G");
                    }

                    probability = probability + Math.log(Utilities.getProbabilityForBigram(collection, curr, next));
                    curr = next;
                }

                next = endMorphmeme;
                probability = probability + Math.log(Utilities.getProbabilityForBigram(collection, curr, next));

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

    public void doItForFile(boolean withStemProbabilities) throws IOException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(fileSegmentationInput));

        String line;
        while ((line = reader.readLine()) != null) {
            String space = " ";
            StringTokenizer st = new StringTokenizer(line, space);

            double freq = Double.parseDouble(st.nextToken());
            String word = st.nextToken();

            reSegmentWithDBforAllomorphs(word, freq, withStemProbabilities);
        }
    }
}

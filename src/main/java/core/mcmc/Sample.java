package core.mcmc;

import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import tries.TrieST;

import java.util.ArrayList;

/**
 * Created by ahmetu on 28.09.2016.
 */
public class Sample {

    private TrieST inTrie;
    private double similarityScore;
    private double poissonScore;
    private String word;
    private String segmentation;
    private double presenceScore;
    private boolean isCalculated;

    public boolean isCalculated() {
        return isCalculated;
    }

    public void setCalculated(boolean calculated) {
        isCalculated = calculated;
    }

    public double getPresenceScore() {
        return presenceScore;
    }

    public void setPresenceScore(double presenceScore) {
        this.presenceScore = presenceScore;
    }

    public TrieST getInTrie() {
        return inTrie;
    }

    public void setInTrie(TrieST inTrie) {
        this.inTrie = inTrie;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public double getPoissonScore() {
        return poissonScore;
    }

    public void setPoissonScore(double poissonScore) {
        this.poissonScore = poissonScore;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getSegmentation() {
        return segmentation;
    }

    public void setSegmentation(String segmentation) {
        this.segmentation = segmentation;
    }

    public void update(String segmentation, double poissonScore, double similarityScore, double presenceScore) {
        this.segmentation = segmentation;
        this.poissonScore = poissonScore;
        this.similarityScore = similarityScore;
        this.presenceScore = presenceScore;
    }

    public Sample(String word, String segmentation, TrieST inTrie) {
        this.word = word;
        this.segmentation = segmentation;
        this.inTrie = inTrie;
        this.isCalculated = false;

        /*
        ArrayList<String> segments = Operations.getSegments(segmentation);
        this.poissonScore = calculatePoisson(segments);
        this.similarityScore = calculateSimilarity(segments);
        this.presenceScore = calculatePresenceScore(segments);
        */
    }

    public ArrayList<Double> calculateScores(String segmentation) {
        //0:poisson, 1:similarity, 2:presence
        ArrayList<Double> scores = new ArrayList<>();

        ArrayList<String> segments = Operations.getSegments(segmentation);
        double poissonScore = calculatePoisson(segments);
        double similarityScore = calculateSimilarity(segments);
        double presenceScore = calculatePresenceScore(segments);

        scores.add(poissonScore);
        scores.add(similarityScore);
        scores.add(presenceScore);

        return scores;
    }

    private double calculatePoisson(ArrayList<String> segments) {
        double totalPoisson = 0;
        for (String s : segments) {
            totalPoisson = totalPoisson + Math.log10(Operations.getPoissonScore(inTrie.getWordList().get(s), Constant.getLambda()));
        }
        return totalPoisson;
    }

    private double calculateSimilarity(ArrayList<String> segments) {

        double similarityScore = 0;

        String w1 = segments.get(0);
        String w2 = "";
        for (int i = 1; i < segments.size(); i++) {
            w2 = segments.get(i);
            double cosine = Operations.getCosineScore(w1, w2);
            similarityScore = similarityScore + Math.log10(cosine);
            w1 = w2;
        }

        return similarityScore;
    }

    private double calculatePresenceScore(ArrayList<String> segments) {

        double presenceScore = 0;

        for (String s : segments) {
            presenceScore = presenceScore + Math.log10(Constant.getNewCorpus().get(s) / Constant.getNewCorpusSize());
        }

        return presenceScore;
    }
}

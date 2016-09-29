package core.mcmc;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Murathan on 29-Sep-16.
 */
public class Inference {

    private Map<String, Integer> frequencyTable = new ConcurrentHashMap<>();
    private CopyOnWriteArrayList<Sample> samples = new CopyOnWriteArrayList<>();
    private int noOfIteration;
    private int sizeOfTable = 0;

    public Inference(String triesDir, String vectorDir, String wordListDir, double lambda, int noOfIteration) throws IOException, ClassNotFoundException {
        Constant baseline = new Constant(triesDir, vectorDir, wordListDir, lambda);
        this.noOfIteration = noOfIteration;
        this.frequencyTable = new ConcurrentHashMap<>(baseline.getMorphemeFreq());
        this.samples = new CopyOnWriteArrayList<>(baseline.getSampleList());

        for (String str : frequencyTable.keySet()) {
            sizeOfTable = sizeOfTable + frequencyTable.get(str);
        }
    }

    public void doSampling() {
        while (noOfIteration > 0) {
            Collections.shuffle(samples);
            for (Sample sample : samples) {

                ArrayList<Double> oldPriors;
                if (!sample.isCalculated()) {
                    oldPriors = sample.calculateScores(sample.getSegmentation());
                    sample.update(sample.getSegmentation(), oldPriors.get(0), oldPriors.get(1), oldPriors.get(2));
                    sample.setCalculated(true);
                } else {
                    oldPriors = new ArrayList<>();
                    oldPriors.add(sample.getPoissonScore());
                    oldPriors.add(sample.getSimilarityScore());
                    oldPriors.add(sample.getPresenceScore());
                }

                int deleteNo = deleteFromTable(sample.getSegmentation());
                sizeOfTable = sizeOfTable - deleteNo;

                ArrayList<Double> likelihoods = calculateLikelihoodsWithDP();

                String newSegmentation = Operations.randomSplitB(sample.getWord());
                ArrayList<Double> newPriors = sample.calculateScores(newSegmentation);

                double oldJointProbability = likelihoods.get(0) + oldPriors.get(0) + oldPriors.get(1) + oldPriors.get(2);
                double newJointProbability = likelihoods.get(1) + newPriors.get(0) + newPriors.get(1) + newPriors.get(2);

                boolean accept = isAccepted(newJointProbability, oldJointProbability);

                if (accept) {
                    sample.update(newSegmentation, newPriors.get(0), newPriors.get(1), newPriors.get(2));
                    int insertedNo = insertToTable(newSegmentation);
                    sizeOfTable = sizeOfTable + insertedNo;
                } else {
                    int insertedNo = insertToTable(sample.getSegmentation());
                    sizeOfTable = sizeOfTable + insertedNo;
                }
            }
            noOfIteration--;
        }
    }

    private ArrayList<Double> calculateLikelihoodsWithDP() {
        ArrayList<Double> likelihoods = new ArrayList<>();
        //0:oldLikelihood, 1:newLikelihood

        //Formül gelecek
        return likelihoods;
    }

    private boolean isAccepted(double newJointProbability, double oldJointProbability) {
        boolean accept = false;
        if (newJointProbability > oldJointProbability) {
            accept = true;
        } else {
            double acceptProb = newJointProbability - oldJointProbability;
            acceptProb = Math.pow(10, acceptProb);
            Random rand = new Random();
            double randProb = rand.nextDouble();

            if (randProb < acceptProb) {
                accept = true;
            }
        }
        return accept;
    }

    private int deleteFromTable(String segmentation) {
        StringTokenizer tokenizer = new StringTokenizer(segmentation, "+");
        int deletedNo = 0;
        while (tokenizer.hasMoreTokens()) {
            String morpheme = tokenizer.nextToken();
            frequencyTable.put(morpheme, frequencyTable.get(morpheme) - 1);
            deletedNo++;
        }
        return deletedNo;
    }

    private int insertToTable(String segmentation) {
        StringTokenizer tokenizer = new StringTokenizer(segmentation, "+");
        int insertedNo = 0;
        while (tokenizer.hasMoreTokens()) {
            String morpheme = tokenizer.nextToken();
            frequencyTable.put(morpheme, frequencyTable.get(morpheme) + 1);
            insertedNo++;
        }
        return insertedNo;
    }

}

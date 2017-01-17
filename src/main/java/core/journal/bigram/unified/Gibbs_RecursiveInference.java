package core.journal.bigram.unified;

import core.journal.SerializableModel;
import core.journal.SerializableModel_bigram;
import core.journal.bigram.unified.Operations;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Murathan on 29-Sep-16.
 */
public class Gibbs_RecursiveInference {

    private Map<String, Integer> frequencyTable = new ConcurrentHashMap<>();
    private Map<String, HashMap<String, Integer>> bigramFreq = new HashMap<>();
    private CopyOnWriteArrayList<Sample> samples = new CopyOnWriteArrayList<>();
    private int noOfIteration;
    private int noOfIterationCopy;

    private int sizeOfTable = 0;
    private static boolean[] featuresBooleanList = new boolean[4]; //0:poisson, 1:similarity, 2:presence, 3: length
    private String featString = "";
    private int heuristic;
    private double alpha = 0.01;
    private double gamma = 0.037;
    private String resultsDir;
    private String bayes;

    public String generateFeatureString() {
        if (featuresBooleanList[0] == true) {
            featString = featString + "_Pois";
        }
        if (featuresBooleanList[1] == true) {
            featString = featString + "_Sim";
        }
        if (featuresBooleanList[2] == true) {
            featString = featString + "_Pres";
        }
        if (featuresBooleanList[3] == true) {
            featString = featString + "_Len";
        }

        return featString;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        // <outputDir> <wordListDir> <>
        Gibbs_RecursiveInference i = new Gibbs_RecursiveInference(args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]),
                Boolean.valueOf(args[4]), Boolean.valueOf(args[5]), Boolean.valueOf(args[6]), Boolean.valueOf(args[7]), Boolean.valueOf(args[8]),
                Integer.parseInt(args[9]), Double.parseDouble(args[10]), Double.parseDouble(args[11]), args[12], args[13]);

        i.featString = i.generateFeatureString();

        System.out.println("-----BASELINE SEGMENTATIONS-------");
        for (Sample s : i.samples) {
            System.out.println(s.getWord() + "--> " + s.getSegmentation());
        }
        System.out.println("-----END OF BASELINE SEGMENTATIONS-------");
        System.out.println("-----SAMPLING-------");
        i.doSampling();
        System.out.println("-----END OF SAMPLING-------");

        System.out.println("-----SEGMENTATIONS: AFTER SAMPLING-------");
        for (Sample s : i.samples) {
            System.out.println(s.getWord() + "--> " + s.getSegmentation());
        }
    }

    public Gibbs_RecursiveInference(String outputDir, String wordListDir,
            int noOfIteration, int freqThreshold, boolean includeFreq, boolean poisson,
            boolean sim, boolean presence, boolean length, int heuristic, double simUnsegmentedArg, double simUnfoundArg, String resultsDir, String bayes) throws IOException, ClassNotFoundException {

        Constant baseline = new Constant(outputDir, wordListDir, heuristic, simUnsegmentedArg, simUnfoundArg, freqThreshold, includeFreq);
        this.resultsDir = resultsDir;
        this.bayes = bayes;
        this.heuristic = heuristic;
        this.noOfIteration = noOfIteration;
        this.noOfIterationCopy = noOfIteration;
        this.frequencyTable = new HashMap<>(baseline.getMorphemeFreq());
        this.bigramFreq = new HashMap(baseline.getBigramFreq());
        this.samples = new CopyOnWriteArrayList<>(baseline.getSampleList());
        for (String str : frequencyTable.keySet()) {
            sizeOfTable = sizeOfTable + frequencyTable.get(str);
        }
        featuresBooleanList[0] = poisson;
        featuresBooleanList[1] = sim;
        featuresBooleanList[2] = presence;
        featuresBooleanList[3] = length;
    }

    public void doSampling() throws IOException {

        while (noOfIteration > 0) {

            System.out.println("Iter: " + noOfIteration);

            Collections.shuffle(samples);
            for (Sample sample : samples) {

                int deleteNo = deleteFromTable(sample.getSegmentation());
                sizeOfTable = sizeOfTable - deleteNo;

                deleteFromBigramMap(sample.getSegmentation());

                //     System.out.print("Selected item: " + sample.getSegmentation() + "     ");
                //         System.out.println("---> Recursive operation started..");
                //      System.out.printf("%s%13s%13s%13s%13s%13s", "Split", "Dp Score", "poisson", "similarity", "presence", "length");
                //        System.out.println();
                sample.setSegmentation("");
                recursiveSplit(sample, sample.getWord());

                if (!sample.getSegmentation().contains("+")) {
                    frequencyTable.put("$", frequencyTable.get("$") + 1);
                    sizeOfTable++;
                }
                insertToBigramMap(sample.getSegmentation());

                //         System.out.println("Selected segmentation: " + sample.getSegmentation());
            }
            noOfIteration--;

        }
        printModel();
        //saveModel();
        //  saveSimiliarityValues();
    }

    public void printModel() throws FileNotFoundException {

        String f = Constant.getIncludeFrequency() ? "f" : "nf";

        String sim = featuresBooleanList[1] ? "s" : "ns";

        PrintWriter results = new PrintWriter(resultsDir + "/" + bayes + "_results_" + f + "_" + sim);

        for (Sample s : samples) {
            results.println(s.getWord().toLowerCase().replaceAll("ö", "O").replaceAll("ç", "C").replaceAll("ü", "U").replaceAll("ı", "I").replaceAll("ğ", "G").replaceAll("ü", "U").replaceAll("ş", "S")
                    + "\t" + s.getSegmentation().toLowerCase().replaceAll("\\+", " ").replaceAll("ö", "O").
                    replaceAll("ç", "C").replaceAll("ü", "U").replaceAll("ı", "I").replaceAll("ğ", "G").replaceAll("ü", "U").replaceAll("ş", "S"));
        }
        results.close();

        PrintWriter stems = new PrintWriter(resultsDir + "/" + bayes + "_morpheme_" + f + "_" + sim);
        for (String stem : frequencyTable.keySet()) {
            stems.println(stem + ":" + frequencyTable.get(stem));
        }
        stems.close();

        PrintWriter bigrams = new PrintWriter(resultsDir + "/" + bayes + "_bigrams_" + f + "_" + sim);
        for (String current : bigramFreq.keySet()) {
            HashMap<String, Integer> transition = bigramFreq.get(current);
            for (String next : transition.keySet()) {
                bigrams.println(current + ">" + next + ":" + transition.get(next));
            }
        }
        bigrams.close();
    }

    private String recursiveSplit(Sample sample, String word) {

        ArrayList<String> possibleSplits = Operations.getPossibleBinarySplits(word, Constant.getHeuristic());
        ArrayList<Double> scores = new ArrayList<>();

        double forNormalize = 0.0;
        double dpScore = 0.0;
        for (String split : possibleSplits) {
            ArrayList<Double> priors = sample.calculateScores(split, featuresBooleanList);  // //0:poisson, 1:similarity, 2:presence, 3: length
            if (bayes.equalsIgnoreCase("ml")) {
                dpScore = calculateBigramLikelihoods(split);
            } else {
                dpScore = calculateBigramLikelihoodsWithDP(split);
            }

            double total = dpScore + priors.get(0) + priors.get(1) + priors.get(2) + priors.get(3);

            //       System.out.printf("%s%13f%13f%13f%13f%13f", split, dpScore, priors.get(0), priors.get(1), priors.get(2), priors.get(3));
            //      System.out.println();
            double nonlog_total = Math.pow(10, total);
            forNormalize = forNormalize + nonlog_total;
            //       System.out.println("nonlog_total: " + nonlog_total);
            scores.add(nonlog_total);
        }
        //  System.out.println("-------------");
        //     System.out.println("forNormalize: " + forNormalize);
        ArrayList<Double> sortedScores = new ArrayList<>(scores);
        Collections.sort(sortedScores);

        double normalizationConst = 1 / forNormalize;
        //    System.out.println("normalizationConst: " + normalizationConst);

        Random rand = new Random();
        double rndSample = rand.nextDouble();

        double s_value = 0;
        double value = 0;
        for (double i_value : sortedScores) {
            value = value + i_value * normalizationConst;
            if (value > rndSample) {
                s_value = i_value;
                break;
            }
        }
        //   System.out.println("s_value: " + s_value);
        String selected = null;
        try {
            selected = possibleSplits.get(scores.indexOf(s_value));
        } catch (Exception e) {
            System.out.println();
        }

        if (!selected.equals(word)) {

            StringTokenizer tokenizer = new StringTokenizer(selected, "+");
            String leftMorpheme = tokenizer.nextToken();
            String rightMorpheme = tokenizer.nextToken();

            if (frequencyTable.containsKey(rightMorpheme)) {
                frequencyTable.put(rightMorpheme, frequencyTable.get(rightMorpheme) + 1);
            } else {
                frequencyTable.put(rightMorpheme, 1);
            }
            sizeOfTable = sizeOfTable + 1;

            if (sample.getSegmentation().equalsIgnoreCase("")) {
                sample.setSegmentation(rightMorpheme);
            } else {
                sample.setSegmentation(rightMorpheme + "+" + sample.getSegmentation());
            }

            return recursiveSplit(sample, leftMorpheme);

        } else {
            if (frequencyTable.containsKey(word)) {
                frequencyTable.put(word, frequencyTable.get(word) + 1);
            } else {
                frequencyTable.put(word, 1);
            }
            sizeOfTable = sizeOfTable + 1;

            if (sample.getSegmentation().equalsIgnoreCase("")) {
                sample.setSegmentation(word);
            } else {
                sample.setSegmentation(word + "+" + sample.getSegmentation());
            }

            return word;
        }
    }

    private Double calculateBigramLikelihoods(String newSegmentation) {
        int size = sizeOfTable;
        double newLikelihood = 0;
        String uSymbol = "$";

        StringTokenizer newSegments = new StringTokenizer(newSegmentation, "+");
        String stem = newSegments.nextToken();
        String suffix = (newSegments.hasMoreTokens()) ? newSegments.nextToken() : uSymbol;

        double umCount = (double) getUnigramCount(stem);
        if (umCount != 0) {
            newLikelihood = newLikelihood + Math.log10(umCount / size);
            frequencyTable.put(stem, (int) umCount + 1);
            size++;

            double beCount = (double) getBigramCount(stem, suffix);
            double ueCount = (double) getUnigramCount(suffix);
            if (beCount != 0) {
                newLikelihood = newLikelihood + Math.log10(beCount / umCount);
                frequencyTable.put(suffix, (int) ueCount + 1);
                size++;

            } else {
                newLikelihood = newLikelihood + Math.log10(Constant.getBigramSmoothingCoefficient() / umCount);
            }

            /*else if (ueCount != 0) {
                newLikelihood = newLikelihood + Math.log10(ueCount / size);
                frequencyTable.put(suffix, (int) ueCount + 1);
                size++;
            } else {
                newLikelihood = newLikelihood + Math.log10(Constant.getSmoothingCoefficient() / size);
                frequencyTable.put(suffix, 1);
                size++;
            }*/
        } else {

            newLikelihood = newLikelihood + Math.log10(Constant.getSmoothingCoefficient() / size);
            frequencyTable.put(stem, 1);
            size++;
            double ueCount = (double) getUnigramCount(suffix);
            if (ueCount != 0) {
                newLikelihood = newLikelihood + Math.log10(ueCount / size);
                frequencyTable.put(suffix, (int) ueCount + 1);
                size++;
            } else {
                newLikelihood = newLikelihood + Math.log10(Constant.getSmoothingCoefficient() / size);
                frequencyTable.put(suffix, 1);
                size++;
            }

        }

        deleteFromTable(newSegmentation);

        return newLikelihood;
    }

    private Double calculateBigramLikelihoodsWithDP(String newSegmentation) {
        int size = sizeOfTable;
        double newLikelihood = 0;
        String uSymbol = "$";

        StringTokenizer newSegments = new StringTokenizer(newSegmentation, "+");
        String stem = newSegments.nextToken();
        String suffix = (newSegments.hasMoreTokens()) ? newSegments.nextToken() : uSymbol;
        int suffixLenght = (suffix.equals("$")) ? 0 : suffix.length();

        double umCount = (double) getUnigramCount(stem);
        if (umCount != 0) {
            newLikelihood = newLikelihood + Math.log10(umCount / (size + alpha));
            frequencyTable.put(stem, (int) umCount + 1);
            size++;

            double beCount = (double) getBigramCount(stem, suffix);
            double ueCount = (double) getUnigramCount(suffix);
            if (beCount != 0) {
                newLikelihood = newLikelihood + Math.log10(beCount / umCount + alpha);
                frequencyTable.put(suffix, (int) ueCount + 1);
                size++;
            } else if (ueCount != 0) {
                newLikelihood = newLikelihood + Math.log10(alpha * (ueCount / (size + alpha)));
                frequencyTable.put(suffix, (int) ueCount + 1);
                size++;
            } else {
                newLikelihood = newLikelihood + Math.log10(alpha * (alpha * Math.pow(gamma, suffixLenght + 1) / ((double) size + alpha)));
                frequencyTable.put(suffix, 1);
                size++;
            }

        } else {

            newLikelihood = newLikelihood + Math.log10(alpha * Math.pow(gamma, stem.length() + 1) / ((double) size + alpha));
            frequencyTable.put(stem, 1);
            size++;
            double ueCount = (double) getUnigramCount(suffix);
            if (ueCount != 0) {
                newLikelihood = newLikelihood + Math.log10(alpha * (ueCount / (size + alpha)));
                frequencyTable.put(suffix, (int) ueCount + 1);
                size++;
            } else {
                newLikelihood = newLikelihood + Math.log10(alpha * (alpha * Math.pow(gamma, suffixLenght + 1) / ((double) size + alpha)));
                frequencyTable.put(suffix, (int) ueCount + 1);
                size++;
            }

        }

        deleteFromTable(newSegmentation);

        return newLikelihood;
    }

    private int getBigramCount(String current, String next) {
        if (bigramFreq.containsKey(current)) {
            HashMap<String, Integer> transitions = bigramFreq.get(current);
            if (transitions.containsKey(next)) {
                return transitions.get(next);
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    private int getUnigramCount(String morpheme) {
        if (frequencyTable.containsKey(morpheme)) {
            return frequencyTable.get(morpheme);
        } else {
            return 0;
        }
    }

    private boolean isAccepted(double newJointProbability, double oldJointProbability) {
        boolean accept = false;
        if (Double.isNaN(newJointProbability)) {
            System.out.println("here");
        }
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

        String uSymbol = "$";
        if (!segmentation.contains("+")) {
            frequencyTable.put(segmentation, frequencyTable.get(segmentation) - 1);
            frequencyTable.put(uSymbol, frequencyTable.get(uSymbol) - 1);
            return 2;
        } else {
            StringTokenizer tokenizer = new StringTokenizer(segmentation, "+");
            int deletedNo = 0;
            while (tokenizer.hasMoreTokens()) {
                String morpheme = tokenizer.nextToken();
                frequencyTable.put(morpheme, frequencyTable.get(morpheme) - 1);
                deletedNo++;
            }
            return deletedNo;
        }
    }

    private void deleteFromBigramMap(String segmentation) {
        String uSymbol = "$";
        if (!segmentation.contains("+")) {
            bigramFreq.get(segmentation).put(uSymbol, bigramFreq.get(segmentation).get(uSymbol) - 1);
        } else {
            StringTokenizer tokenizer = new StringTokenizer(segmentation, "+");
            String curr = tokenizer.nextToken();
            String next = null;
            while (tokenizer.hasMoreTokens()) {
                next = tokenizer.nextToken();
                bigramFreq.get(curr).put(next, bigramFreq.get(curr).get(next) - 1);
                curr = next;
            }
        }
    }

    private void insertToBigramMap(String segmentation) {
        String uSymbol = "$";
        if (!segmentation.contains("+")) {
            HashMap<String, Integer> transitions;
            if (bigramFreq.containsKey(segmentation)) {
                transitions = bigramFreq.get(segmentation);
                if (transitions.containsKey(uSymbol)) {
                    transitions.put(uSymbol, transitions.get(uSymbol) + 1);
                } else {
                    transitions.put(uSymbol, 1);
                }
            } else {
                transitions = new HashMap<>();
                transitions.put(uSymbol, 1);
            }
            bigramFreq.put(segmentation, transitions);
        } else {
            StringTokenizer tokenizer = new StringTokenizer(segmentation, "+");
            String curr = tokenizer.nextToken();
            String next = null;
            while (tokenizer.hasMoreTokens()) {
                next = tokenizer.nextToken();
                HashMap<String, Integer> transitions;
                if (bigramFreq.containsKey(curr)) {
                    transitions = bigramFreq.get(curr);
                    if (transitions.containsKey(next)) {
                        transitions.put(next, transitions.get(next) + 1);
                    } else {
                        transitions.put(next, 1);
                    }
                } else {
                    transitions = new HashMap<>();
                    transitions.put(next, 1);
                }
                bigramFreq.put(curr, transitions);
                curr = next;
            }
        }
    }

    private int insertToTable(String segmentation) {
        StringTokenizer tokenizer = new StringTokenizer(segmentation, "+");
        int insertedNo = 0;
        while (tokenizer.hasMoreTokens()) {
            String morpheme = tokenizer.nextToken();
            if (frequencyTable.containsKey(morpheme)) {
                frequencyTable.put(morpheme, frequencyTable.get(morpheme) + 1);
            } else {
                frequencyTable.put(morpheme, 1);
            }
            insertedNo++;
        }
        return insertedNo;
    }

    public void saveModel() throws IOException {

        Map<String, ArrayList<String>> segmentationsList = new HashMap<>();

        for (Sample s : this.samples) {
            if (segmentationsList.containsKey(s.getWord())) {
                segmentationsList.get(s.getWord()).add(s.getSegmentation());
            } else {
                ArrayList<String> segmentationsOfsample = new ArrayList<String>();
                segmentationsOfsample.add(s.getSegmentation());
                segmentationsList.put(s.getWord(), segmentationsOfsample);
            }
        }
        SerializableModel_bigram model = new SerializableModel_bigram(frequencyTable, segmentationsList, bigramFreq);

        // toByteArray
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] yourBytes = null;
        out = new ObjectOutputStream(bos);
        out.writeObject(model);
        yourBytes = bos.toByteArray();

        bos.close();
        out.close();

        FileUtils.writeByteArrayToFile(new File("bigram_DP-NOI_" + noOfIterationCopy + "-Feat" + featString + "-heuristic_" + heuristic + "-SimUNS_" + Constant.getSimUnsegmented()), yourBytes);
    }

}

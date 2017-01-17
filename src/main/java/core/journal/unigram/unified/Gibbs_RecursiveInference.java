package core.journal.unigram.unified;

import core.journal.SerializableModel;
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
        this.heuristic = heuristic;
        this.noOfIteration = noOfIteration;
        this.noOfIterationCopy = noOfIteration;
        this.frequencyTable = new ConcurrentHashMap<>(baseline.getMorphemeFreq());
        this.samples = new CopyOnWriteArrayList<>(baseline.getSampleList());
        this.resultsDir = resultsDir;
        this.bayes = bayes;

        for (String str : frequencyTable.keySet()) {
            sizeOfTable = sizeOfTable + frequencyTable.get(str);
        }
        featuresBooleanList[0] = poisson;
        featuresBooleanList[1] = sim;
        featuresBooleanList[2] = presence;
        featuresBooleanList[3] = length;
    }

    public void printModel() throws FileNotFoundException {

        String f = Constant.getIncludeFrequency() ? "f" : "nf";

        PrintWriter results = new PrintWriter(resultsDir + "/" + bayes + "_results_" + f);

        for (Sample s : samples) {
            results.println(s.getWord().toLowerCase().replaceAll("ö", "O").replaceAll("ç", "C").replaceAll("ü", "U").replaceAll("ı", "I").replaceAll("ğ", "G").replaceAll("ü", "U").replaceAll("ş", "S")
                    + "\t" + s.getSegmentation().toLowerCase().replaceAll("\\+", " ").replaceAll("ö", "O").
                    replaceAll("ç", "C").replaceAll("ü", "U").replaceAll("ı", "I").replaceAll("ğ", "G").replaceAll("ü", "U").replaceAll("ş", "S"));
        }
        results.close();

        PrintWriter stems = new PrintWriter(resultsDir + "/" + bayes + "_morphemes_" + f);
        for (String stem : frequencyTable.keySet()) {
            stems.println(stem + ":" + frequencyTable.get(stem));
        }
        stems.close();
    }

    public void doSampling() throws IOException {

        while (noOfIteration > 0) {

            System.out.println("Iter: " + noOfIteration);

            Collections.shuffle(samples);
            for (Sample sample : samples) {

                int deleteNo = deleteFromTable(sample.getSegmentation());
                sizeOfTable = sizeOfTable - deleteNo;

                //     System.out.print("Selected item: " + sample.getSegmentation() + "     ");
                //         System.out.println("---> Recursive operation started..");
                //      System.out.printf("%s%13s%13s%13s%13s%13s", "Split", "Dp Score", "poisson", "similarity", "presence", "length");
                //        System.out.println();
                sample.setSegmentation("");
                recursiveSplit(sample, sample.getWord());

                if (!sample.getSegmentation().contains("+")) {
                    String suffix = "$";
                    if (frequencyTable.containsKey(suffix)) {
                        frequencyTable.put(suffix, frequencyTable.get(suffix) + 1);
                    } else {
                        frequencyTable.put(suffix, 1);
                    }
                }

                //         System.out.println("Selected segmentation: " + sample.getSegmentation());
            }
            noOfIteration--;

        }
        printModel();
        //saveModel();
        //  saveSimiliarityValues();
    }

    private String recursiveSplit(Sample sample, String word) {

        ArrayList<String> possibleSplits = Operations.getPossibleBinarySplits(word, Constant.getHeuristic());
        ArrayList<Double> scores = new ArrayList<>();

        double forNormalize = 0.0;
        double dpScore = 0.0;
        for (String split : possibleSplits) {
            ArrayList<Double> priors = sample.calculateScores(split, featuresBooleanList);  // //0:poisson, 1:similarity, 2:presence, 3: length

            if (bayes.equalsIgnoreCase("ml")) {
                dpScore = calculateUnigramLikelihoods(split);
            } else {
                dpScore = calculateUnigramLikelihoodsWithDP(split);
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

    private int getUnigramCount(String morpheme) {
        if (frequencyTable.containsKey(morpheme)) {
            return frequencyTable.get(morpheme);
        } else {
            return 0;
        }
    }

    private Double calculateUnigramLikelihoods(String newSegmentation) {
        int size = sizeOfTable;
        double newLikelihood = 0;

        StringTokenizer newSegments = new StringTokenizer(newSegmentation, "+");
        String stem = newSegments.nextToken();
        String suffix = (newSegments.hasMoreTokens()) ? newSegments.nextToken() : "$";

        if (frequencyTable.containsKey(stem)) {
            if (frequencyTable.get(stem) > 0) {
                newLikelihood = newLikelihood + Math.log10((double) frequencyTable.get(stem) / (size));
                frequencyTable.put(stem, frequencyTable.get(stem) + 1);
                size++;
            } else {
                newLikelihood = newLikelihood + Math.log10((double) Constant.getSmoothingCoefficient() / (size));
                frequencyTable.put(stem, 1);
                size++;
            }
        } else {
            newLikelihood = newLikelihood + Math.log10((double) Constant.getSmoothingCoefficient() / (size));
            frequencyTable.put(stem, 1);
            size++;
        }

        if (frequencyTable.containsKey(suffix)) {
            if (frequencyTable.get(suffix) > 0) {
                newLikelihood = newLikelihood + Math.log10((double) frequencyTable.get(suffix) / (size));
                frequencyTable.put(suffix, frequencyTable.get(suffix) + 1);
                size++;
            } else {
                newLikelihood = newLikelihood + Math.log10((double) Constant.getSmoothingCoefficient() / (size));
                frequencyTable.put(suffix, 1);
                size++;
            }
        } else {
            newLikelihood = newLikelihood + Math.log10((double) Constant.getSmoothingCoefficient() / (size));
            frequencyTable.put(suffix, 1);
            size++;
        }

        deleteFromTable(newSegmentation);

        return newLikelihood;
    }

    private Double calculateUnigramLikelihoodsWithDP(String newSegmentation) {
        int size = sizeOfTable;
        double newLikelihood = 0;

        StringTokenizer newSegments = new StringTokenizer(newSegmentation, "+");
        String stem = newSegments.nextToken();
        String suffix = (newSegments.hasMoreTokens()) ? newSegments.nextToken() : "$";
        int suffixLenght = (suffix.equals("$")) ? 0 : suffix.length();

        if (frequencyTable.containsKey(stem)) {
            if (frequencyTable.get(stem) > 0) {
                newLikelihood = newLikelihood + Math.log10((double) frequencyTable.get(stem) / (size));
                frequencyTable.put(stem, frequencyTable.get(stem) + 1);
                size++;
            } else {
                newLikelihood = newLikelihood + Math.log10(alpha * Math.pow(gamma, stem.length() + 1) / ((double) size + alpha));
                frequencyTable.put(stem, 1);
                size++;
            }
        } else {
            newLikelihood = newLikelihood + Math.log10(alpha * Math.pow(gamma, stem.length() + 1) / ((double) size + alpha));
            frequencyTable.put(stem, 1);
            size++;
        }

        if (frequencyTable.containsKey(suffix)) {
            if (frequencyTable.get(suffix) > 0) {
                newLikelihood = newLikelihood + Math.log10((double) frequencyTable.get(suffix) / (size + alpha));
                frequencyTable.put(suffix, frequencyTable.get(suffix) + 1);
                size++;
            } else {
                newLikelihood = newLikelihood + Math.log10(alpha * Math.pow(gamma, suffixLenght + 1) / ((double) size + alpha));
                frequencyTable.put(suffix, 1);
                size++;
            }
        } else {
            newLikelihood = newLikelihood + Math.log10(alpha * Math.pow(gamma, suffixLenght + 1) / ((double) size + alpha));
            frequencyTable.put(suffix, 1);
            size++;
        }

        deleteFromTable(newSegmentation);

        return newLikelihood;
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
        StringTokenizer tokenizer = new StringTokenizer(segmentation, "+");
        int deletedNo = 0;

        if (tokenizer.countTokens() == 1) {
            String suffix = "$";
            frequencyTable.put(suffix, frequencyTable.get(suffix) - 1);
            deletedNo++;
        }

        String stem = tokenizer.nextToken();
        frequencyTable.put(stem, frequencyTable.get(stem) - 1);
        deletedNo++;

        while (tokenizer.hasMoreTokens()) {
            String suffix = tokenizer.nextToken();
            frequencyTable.put(suffix, frequencyTable.get(suffix) - 1);
            deletedNo++;
        }
        return deletedNo;
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
        SerializableModel model = new SerializableModel(frequencyTable, segmentationsList, null);

        // toByteArray
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] yourBytes = null;
        out = new ObjectOutputStream(bos);
        out.writeObject(model);
        yourBytes = bos.toByteArray();

        bos.close();
        out.close();

        FileUtils.writeByteArrayToFile(new File("unigram_DP-NOI_" + noOfIterationCopy + "-Feat" + featString + "-heuristic_" + heuristic + "-SimUNS_" + Constant.getSimUnsegmented()), yourBytes);
    }

}

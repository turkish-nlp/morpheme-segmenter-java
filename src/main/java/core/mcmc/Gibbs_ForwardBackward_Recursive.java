package core.mcmc;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Murathan on 29-Sep-16.
 */
public class Gibbs_ForwardBackward_Recursive {

    private Map<String, Integer> frequencyTable = new ConcurrentHashMap<>();
    private CopyOnWriteArrayList<Sample> samples = new CopyOnWriteArrayList<>();
    private int noOfIteration;
    private int noOfIterationCopy;

    private int sizeOfTable = 0;
    private double alpha;
    private double gamma;
    private static boolean[] featuresBooleanList = new boolean[4]; //0:poisson, 1:similarity, 2:presence, 3: length
    private String featString = "";
    private int baselineBranchNo = Constant.baselineBranchNo;

    public String generateFeatureString() {
        if (featuresBooleanList[0] == true)
            featString = featString + "_Pois";
        if (featuresBooleanList[1] == true)
            featString = featString + "_Sim";
        if (featuresBooleanList[2] == true)
            featString = featString + "_Pres";
        if (featuresBooleanList[3] == true)
            featString = featString + "_Len";

        return featString;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {


        Gibbs_ForwardBackward_Recursive i = new Gibbs_ForwardBackward_Recursive(args[0], args[1], args[2], Double.parseDouble(args[3]), Integer.parseInt(args[4]), Double.parseDouble(args[5]),
                Double.parseDouble(args[6]), Boolean.valueOf(args[7]), Boolean.valueOf(args[8]), Boolean.valueOf(args[9]), Boolean.valueOf(args[10]));

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

    public Gibbs_ForwardBackward_Recursive(String triesDir, String vectorDir, String wordListDir, double lambda, int noOfIteration, double alpha, double gamma, boolean poisson,
                                           boolean sim, boolean presence, boolean length) throws IOException, ClassNotFoundException {
        Constant baseline = new Constant(triesDir, vectorDir, wordListDir, lambda);
        this.noOfIteration = noOfIteration;
        this.noOfIterationCopy = noOfIteration;
        this.frequencyTable = new ConcurrentHashMap<>(baseline.getMorphemeFreq());
        this.samples = new CopyOnWriteArrayList<>(baseline.getSampleList());
        this.alpha = alpha;
        this.gamma = gamma;
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
            Collections.shuffle(samples);
            for (Sample sample : samples) {

                int deleteNo = deleteFromTable(sample.getSegmentation());
                sizeOfTable = sizeOfTable - deleteNo;

                System.out.print("Selected item: " + sample.getSegmentation() + "     ");
                //            System.out.println("---> Recursive operation started..");
                //     System.out.printf("%s%13s%13s%13s%13s", "Split", "Dp Score", "poisson", "similarity", "presence");
                //       System.out.println();

                sample.setSegmentation("");
                recursiveSplit(sample, sample.getWord());

                System.out.println("Selected segmentation: " + sample.getSegmentation());
            }
            noOfIteration--;
        }
        saveModel();
    }

    private String recursiveSplit(Sample sample, String word) {

        ArrayList<String> possibleSplits = Operations.getPossibleBinarySplits(word, Constant.getHeristic());
        ArrayList<Double> scores = new ArrayList<>();

        double forNormalize = 0.0;
        double dpScore = 0.0;
        for (String split : possibleSplits) {
            ArrayList<Double> priors = sample.calculateScores(split, featuresBooleanList);  // //0:poisson, 1:similarity, 2:presence, 3: length
            /// add $ to unsegmented words ????
            // if (!split.contains("+")) {
            //  split = split + "+$";
            //  dpScore = calculateLikelihoodsWithDP(split);
            // } else
            dpScore = calculateLikelihoodsWithDP(split);
            double total = dpScore + priors.get(0) + priors.get(1) + priors.get(2);

            double nonlog_total = Math.pow(10, total);

            StringTokenizer stringTokenizer = new StringTokenizer(split, "+");
            nonlog_total = nonlog_total * leftRecursion(sample, stringTokenizer.nextToken(), Constant.getHeristic(), this.frequencyTable);

            //   System.out.printf("%s%13f%13f%13f%13f", split, dpScore , priors.get(0), priors.get(1),  priors.get(2));
            //      System.out.println();

            forNormalize = forNormalize + nonlog_total;
            scores.add(nonlog_total);
        }
        //      System.out.println("-------------");

        ArrayList<Double> sortedScores = new ArrayList<>(scores);
        Collections.sort(sortedScores);

        double normalizationConst = 1 / forNormalize;

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

        String selected = possibleSplits.get(scores.indexOf(s_value));


        if (!selected.equals(word)) {

            StringTokenizer tokenizer = new StringTokenizer(selected, "+");
            String leftMorpheme = tokenizer.nextToken();
            String rightMorpheme = tokenizer.nextToken();

            if (frequencyTable.containsKey(rightMorpheme))
                frequencyTable.put(rightMorpheme, frequencyTable.get(rightMorpheme) + 1);
            else
                frequencyTable.put(rightMorpheme, 1);
            sizeOfTable = sizeOfTable + 1;

            if (sample.getSegmentation().equalsIgnoreCase(""))
                sample.setSegmentation(rightMorpheme);
            else
                sample.setSegmentation(rightMorpheme + "+" + sample.getSegmentation());

            return recursiveSplit(sample, leftMorpheme);

        } else {
            if (frequencyTable.containsKey(word))
                frequencyTable.put(word, frequencyTable.get(word) + 1);
            else
                frequencyTable.put(word, 1);
            sizeOfTable = sizeOfTable + 1;

            if (sample.getSegmentation().equalsIgnoreCase(""))
                sample.setSegmentation(word);
            else
                sample.setSegmentation(word + "+" + sample.getSegmentation());

            return word;
        }
    }

    private Double calculateLikelihoodsWithDP(String newSegmentation) {
        int size = sizeOfTable;
        double newLikelihood = 0;
        StringTokenizer newSegments = new StringTokenizer(newSegmentation, "+");
        while (newSegments.hasMoreTokens()) {
            String morpheme = newSegments.nextToken();
            if (frequencyTable.containsKey(morpheme)) {
                if (frequencyTable.get(morpheme) > 0) {
                    newLikelihood = newLikelihood + Math.log10(frequencyTable.get(morpheme) / (size + alpha));
                    frequencyTable.put(morpheme, frequencyTable.get(morpheme) + 1);
                    size++;
                } else {
                    newLikelihood = newLikelihood + Math.log10(alpha * Math.pow(gamma, morpheme.length() + 1) / (size + alpha));
                    frequencyTable.put(morpheme, 1);
                    size++;
                }
            } else {
                newLikelihood = newLikelihood + Math.log10(alpha * Math.pow(gamma, morpheme.length() + 1) / (size + alpha));
                frequencyTable.put(morpheme, 1);
                size++;
            }
        }
        deleteFromTable(newSegmentation);

        return newLikelihood;
    }

    private Double calculateLikelihoodsWithDPForOneSplit(String newSegmentation, Map<String, Integer> frequencyTable) {

        int size = 0;

        for (String str : frequencyTable.keySet()) {
            size = size + frequencyTable.get(str);
        }

        double newLikelihood = 0;
        StringTokenizer newSegments = new StringTokenizer(newSegmentation, "+");
        while (newSegments.hasMoreTokens()) {
            String morpheme = newSegments.nextToken();
            if (frequencyTable.containsKey(morpheme)) {
                if (frequencyTable.get(morpheme) > 0) {
                    newLikelihood = newLikelihood + Math.log10(frequencyTable.get(morpheme) / (size + alpha));
                    frequencyTable.put(morpheme, frequencyTable.get(morpheme) + 1);
                    size++;
                } else {
                    newLikelihood = newLikelihood + Math.log10(alpha * Math.pow(gamma, morpheme.length() + 1) / (size + alpha));
                    frequencyTable.put(morpheme, 1);
                    size++;
                }
            } else {
                newLikelihood = newLikelihood + Math.log10(alpha * Math.pow(gamma, morpheme.length() + 1) / (size + alpha));
                frequencyTable.put(morpheme, 1);
                size++;
            }
        }
        deleteFromTable(newSegmentation);

        return newLikelihood;
    }

    private double leftRecursion(Sample sample, String left, int heuristic, Map<String, Integer> localFrequencyTable) {
        double total = 0;
        if (left.length() == heuristic) {
            total = 1;
            return total;
        } else {
            int k = 0;
            if (left.length() >= heuristic + 4) {
                k = 5;
            } else if (left.length() > heuristic - 1) {
                k = left.length() - heuristic + 1;
            } else {
                k = left.length();
            }

            Map<String, Integer> inscopeLocalFrequencyTable;

            // for unsegmented
            /*
            if (first) {
                inscopeLocalFrequencyTable = localFrequencyTable;
                total = total + calculateTotalScoreForOneSplit(sample, left, inscopeLocalFrequencyTable);
            }
            */

            //for other split
            for (int i = 1; i < k; i++) {
                String split = left.substring(0, left.length() - i) + "+" + left.substring(left.length() - i);

                inscopeLocalFrequencyTable = localFrequencyTable;
                double localValue = calculateTotalScoreForOneSplit(sample, split, inscopeLocalFrequencyTable);

                if (inscopeLocalFrequencyTable.containsKey(left.substring(left.length() - i)))
                    inscopeLocalFrequencyTable.put(left.substring(left.length() - i), inscopeLocalFrequencyTable.get(left.substring(left.length() - i)) + 1);
                else
                    inscopeLocalFrequencyTable.put(left.substring(left.length() - i), 1);

                double innerValue = leftRecursion(sample, left.substring(0, left.length() - i), heuristic, inscopeLocalFrequencyTable);
                total = total + localValue * innerValue;
            }
        }
        return total;
    }

    private double calculateTotalScoreForOneSplit(Sample sample, String split, Map<String, Integer> localFrequencyTable) {

        ArrayList<Double> priors = sample.calculateScores(split, featuresBooleanList);   // //0:poisson, 1:similarity, 2:presence, 3: length
        double dpScore = calculateLikelihoodsWithDPForOneSplit(split, localFrequencyTable);
        double total = dpScore + priors.get(0) + priors.get(1) + priors.get(2);

        return Math.pow(10, total);
        //return total;
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
            if (frequencyTable.containsKey(morpheme))
                frequencyTable.put(morpheme, frequencyTable.get(morpheme) + 1);
            else
                frequencyTable.put(morpheme, 1);
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

        SerializableModel model = new SerializableModel(frequencyTable, segmentationsList);

        // toByteArray
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] yourBytes = null;
        out = new ObjectOutputStream(bos);
        out.writeObject(model);
        yourBytes = bos.toByteArray();

        bos.close();
        out.close();

        FileUtils.writeByteArrayToFile(new File("gibbsMODEL-NOI_" + noOfIterationCopy + "-A_" + alpha + "-G_" + gamma + "-Feat" + featString + "-base_" + baselineBranchNo + "-SimUNS_" + Constant.getSimUnsegmented()), yourBytes);
    }

}

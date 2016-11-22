package core.mcmc.oldInference;

import core.mcmc.Constant;
import core.mcmc.Operations;
import core.mcmc.Sample;
import core.mcmc.SerializableModel;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Murathan on 29-Sep-16.
 */
public class RecursiveInference {

    private Map<String, Integer> frequencyTable = new ConcurrentHashMap<>();
    private CopyOnWriteArrayList<Sample> samples = new CopyOnWriteArrayList<>();
    private int noOfIteration;
    private int sizeOfTable = 0;
    private double alpha;
    private double gamma;
    private boolean[] featuresBooleanList = {true,true,true,false}; //0:poisson, 1:similarity, 2:presence, 3: length

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        /*
        System.out.println("Enter the parameters in the following order: triesDir, vectorDir, wordListDir, lambda, noOfIteration, alpha, gamma");
        Scanner scan = new Scanner(System.in);
        String parameters = scan.nextLine();
        String[] parameterList = parameters.split(" ");
        Inference i = new Inference(parameterList[0], parameterList[1], parameterList[2], Double.parseDouble(parameterList[3]), Integer.parseInt(parameterList[4]), Double.parseDouble(parameterList[5]), Double.parseDouble(parameterList[6]));
        */
        RecursiveInference i = new RecursiveInference(args[0], args[1], args[2], Double.parseDouble(args[3]), Integer.parseInt(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]));

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

    public RecursiveInference(String triesDir, String vectorDir, String wordListDir, double lambda, int noOfIteration, double alpha, double gamma) throws IOException, ClassNotFoundException {
        Constant baseline = new Constant(triesDir, vectorDir, wordListDir, lambda, 1); /// !!!!!!!!!!!!!!!!!!!!!!!!! BASELINEBRANCHNO
        this.noOfIteration = noOfIteration;
        this.frequencyTable = new ConcurrentHashMap<>(baseline.getMorphemeFreq());
        this.samples = new CopyOnWriteArrayList<>(baseline.getSampleList());
        this.alpha = alpha;
        this.gamma = gamma;

        for (String str : frequencyTable.keySet()) {
            sizeOfTable = sizeOfTable + frequencyTable.get(str);
        }
    }

    public void doSampling() throws IOException {
        while (noOfIteration > 0) {
            Collections.shuffle(samples);
            for (Sample sample : samples) {

                int deleteNo = deleteFromTable(sample.getSegmentation());
                sizeOfTable = sizeOfTable - deleteNo;

                System.out.println("\nSelected item: " + sample.getSegmentation());
                System.out.println("---> Recursive operation started..");

                sample.setSegmentation("");
                recursiveSplit(sample, sample.getWord());

                System.out.println("---> Selected segmentation: " + sample.getSegmentation());
            }
            noOfIteration--;
        }

        saveModel();
    }

    private String recursiveSplit(Sample sample, String word) {

        String newSegmentation = Operations.biasedBinarySplit(word);

        // if the random segmentation is equal to the unsegmented word !!
        if (newSegmentation.equalsIgnoreCase(word)) {
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

        System.out.println("---> proposed segmentetation: " + newSegmentation);

        ArrayList<Double> newPriors = sample.calculateScores(newSegmentation, featuresBooleanList);
        ArrayList<Double> oldPriors = sample.calculateScores(word, featuresBooleanList);

        ArrayList<Double> likelihoods = calculateLikelihoodsWithDP(word, newSegmentation);

        double oldJointProbability = likelihoods.get(0) + oldPriors.get(0) + Math.log10(0.000001);
        double newJointProbability = likelihoods.get(1) + newPriors.get(0) + newPriors.get(1);

        System.out.println("---> new poisson score: " + newPriors.get(0));
        System.out.println("---> new similarity score: " + newPriors.get(1));
        System.out.println("---> new DP score: " + likelihoods.get(1));
        System.out.println("---> new total: " + newJointProbability);

        System.out.println("-----> unsegmented poisson score: " + oldPriors.get(0));
        System.out.println("-----> unsegmented similarity score: " + Math.log10(0.0001));
        System.out.println("-----> unsegented DP score: " + likelihoods.get(0));
        System.out.println("-----> unsegmented total: " + oldJointProbability);

        boolean accept = isAccepted(newJointProbability, oldJointProbability);

        System.out.println("ACCEPT status: " + accept);

        StringTokenizer tokenizer = new StringTokenizer(newSegmentation, "+");
        String leftMorpheme = tokenizer.nextToken();
        String rightMorpheme = tokenizer.nextToken();

        if (accept) {
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

    private ArrayList<Double> calculateLikelihoodsWithDP(String oldSegmentation, String newSegmentation) {
        ArrayList<Double> likelihoods = new ArrayList<>();
        //0:oldLikelihood, 1:newLikelihood

        int size = sizeOfTable;
        double oldLikelihood = 0;
        StringTokenizer oldSegments = new StringTokenizer(oldSegmentation, "+");
        while (oldSegments.hasMoreTokens()) {
            String morpheme = oldSegments.nextToken();
            if (frequencyTable.containsKey(morpheme)) {
                if (frequencyTable.get(morpheme) > 0) {
                    oldLikelihood = oldLikelihood + Math.log10(frequencyTable.get(morpheme) / (size + alpha));
                    frequencyTable.put(morpheme, frequencyTable.get(morpheme) + 1);
                    size++;
                } else {
                    oldLikelihood = oldLikelihood + Math.log10(alpha * Math.pow(gamma, morpheme.length() + 1) / (size + alpha));
                    frequencyTable.put(morpheme, 1);
                    size++;
                }
            } else {
                oldLikelihood = oldLikelihood + Math.log10(alpha * Math.pow(gamma, morpheme.length() + 1) / (size + alpha));
                frequencyTable.put(morpheme, 1);
                size++;
            }
        }
        deleteFromTable(oldSegmentation);

        size = sizeOfTable;
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

        likelihoods.add(oldLikelihood);
        likelihoods.add(newLikelihood);
        return likelihoods;
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

        FileUtils.writeByteArrayToFile(new File("RecursiveInferenceModel_" + noOfIteration + "_" + alpha), yourBytes);
    }

}

package core.mcmc.oldInference;

import core.mcmc.withTries.Constant;
import core.mcmc.withTries.Operations;
import org.apache.commons.io.FileUtils;
import tries.TrieST;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by ahmetu on 17.10.2016.
 */
public class ManuelTest {

    private Map<String, Integer> frequencyTable = new HashMap<>();
    private int sizeOfTable = 0;
    private double alpha;
    private double gamma;
    private ArrayList<String> segments = new ArrayList<>();
    private HashMap<String, ArrayList<String>> segmentsForSimilarity = new HashMap<>();
    private HashMap<String, ArrayList<String>> morphemesForLenght = new HashMap<>();
    private ArrayList<String> morphemes = new ArrayList<>();
    private String inFile;
    private TrieST inTrie;

    public ManuelTest(String inputFile, String triesDir, String vectorDir, String wordListDir, double lambda, double alpha, double gamma, int baselineBranchNoArg, double simUnsegmented) throws IOException, ClassNotFoundException {
        this.alpha = alpha;
        this.gamma = gamma;
        inFile = inputFile;
        readSegmentations(triesDir);
        Constant baseline = new Constant(triesDir, vectorDir, wordListDir, lambda, baselineBranchNoArg, simUnsegmented);
        inTrie = baseline.getBaselineBoundaries().keySet().iterator().next();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ManuelTest mt = new ManuelTest(args[0], args[1], args[2], args[3], Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), Integer.parseInt(args[7]), Double.parseDouble(args[8]));
        mt.calculateJointProbability();
    }

    public void calculateJointProbability() throws IOException {
        System.out.println("--------------------------------------------------------");
        System.out.println("Calculating overall poisson score..");
        double totalPoisson = 0;
        for (String segment : segments) {
            double poisson = Math.log10(Operations.getPoissonScore(inTrie.getWordList().get(segment), Constant.getLambda()));
            totalPoisson = totalPoisson + poisson;

            //System.out.println("possion --> " + segment + " --> " + inTrie.getWordList().get(segment) + " " + poisson);
        }
        System.out.println("Overal poisson score: " + totalPoisson);

        System.out.println("--------------------------------------------------------");
        System.out.println("Calculating overall similarity score..");
        double similarityScore = 0;
        for (String segmentation : segmentsForSimilarity.keySet()) {
            String w1 = segmentsForSimilarity.get(segmentation).get(0);
            String w2 = "";
            for (int i = 1; i < segmentsForSimilarity.get(segmentation).size(); i++) {
                w2 = segmentsForSimilarity.get(segmentation).get(i);
                double cosine = Math.log10(Operations.getCosineScore(w1, w2));
                similarityScore = similarityScore + cosine;

                //System.out.println("similarity -->" + w1 + " vs " + w2 + " --> " + cosine);

                w1 = w2;
            }
        }
        System.out.println("Overall similarity score: " + similarityScore);

        System.out.println("--------------------------------------------------------");
        System.out.println("Calculating overall presence score..");
        double presenceScore = 0;
        for (String segment : segments) {
            double presence = Math.log10(Constant.getNewCorpus().get(segment) / Constant.getNewCorpusSize());
            presenceScore = presenceScore + presence;
            //    System.out.println("presence --> " + segment + " --> " + presence);
        }
        System.out.println("Overall presence score: " + presenceScore);


        System.out.println("--------------------------------------------------------");
        System.out.println("Calculating overall length score..");

        double lengthScore = 0;

        for (String segmentation : morphemesForLenght.keySet()) {
            ArrayList<String> ms = morphemesForLenght.get(segmentation);
            int length = 0;
            double localScore = 0;
            for (String m : ms) {
                length = length + m.length();
            }
            localScore = Math.log10(Math.pow(0.037, length / ms.size()));
            lengthScore = lengthScore + localScore;
        }

        System.out.println("Overall length score: " + lengthScore);


        System.out.println("--------------------------------------------------------");
        System.out.println("Calculating overall DP score..");
        double dpScore = 0;
        for (String morpheme : morphemes) {
            if (frequencyTable.containsKey(morpheme)) {
                double dp = Math.log10(frequencyTable.get(morpheme) / (sizeOfTable + alpha));
                dpScore = dpScore + dp;

                //System.out.println("dp --> " + morpheme + " --> " + frequencyTable.containsKey(morpheme) + " " + dp);

                frequencyTable.put(morpheme, frequencyTable.get(morpheme) + 1);
                sizeOfTable++;
            } else {
                double dp = Math.log10(alpha * Math.pow(gamma, morpheme.length() + 1) / (sizeOfTable + alpha));
                dpScore = dpScore + dp;

                //System.out.println("dp --> " + morpheme + " --> " + frequencyTable.containsKey(morpheme) + " " + dp);

                frequencyTable.put(morpheme, 1);
                sizeOfTable++;
            }
        }
        System.out.println("Overall DP score: " + dpScore);

        System.out.println("--------------------------------------------------------");
        System.out.println("Overall Joint Probability score: " + (totalPoisson + presenceScore + similarityScore + dpScore));
    }

    public void readSegmentations(String outTrieDir) throws IOException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(inFile));
        String line;
        System.out.println("--------------------------------------------------------");
        System.out.println("Reading segmentations..");

        TrieST st = new TrieST();

        while ((line = reader.readLine()) != null) {

            ArrayList<String> inSegments = Operations.getSegments(line);
            segments.addAll(inSegments);
            segmentsForSimilarity.put(line, inSegments);

            st.put(line.replaceAll("\\+", "") + "$");

            System.out.println("---> " + line);

            String separator = "+";
            StringTokenizer tokenizer = new StringTokenizer(line, separator);
            while (tokenizer.hasMoreTokens()) {
                String morpheme = tokenizer.nextToken();
                morphemes.add(morpheme);
            }

            tokenizer = new StringTokenizer(line, separator);
            ArrayList<String> ms = new ArrayList<>();
            while (tokenizer.hasMoreTokens()) {
                String morpheme = tokenizer.nextToken();
                ms.add(morpheme);
            }
            morphemesForLenght.put(line, ms);
        }
        serializeToFile(st, "testTrie", "trieData");
        System.out.println("--------------------------------------------------------");
    }

    private static void serializeToFile(TrieST st, String word, String dir) throws IOException {
        // toByteArray
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] yourBytes = null;
        out = new ObjectOutputStream(bos);
        out.writeObject(st);
        yourBytes = bos.toByteArray();

        bos.close();
        out.close();

        FileUtils.writeByteArrayToFile(new File(dir + "/" + word), yourBytes);
    }

}

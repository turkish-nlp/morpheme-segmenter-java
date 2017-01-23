package core.mcmc.withDeserializedMaps;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by ahmetu on 28.09.2016.
 */
public class Constant {

    private static double scoreLowSim = 0.001;

    public static double getScoreLowSim() {
        return  scoreLowSim;
    }
    public static void setScoreLowSim(double arg) {
        scoreLowSim = arg;
    }

    private static double lambda;
    private static HashMap<String, Double> newCorpus = new HashMap<>();
    private static double newCorpusSize = 0;
    private static double laplaceCoefficient = 0.0000001;
    private static double simUnfound = 0.1;
    private static double simUnsegmented;
    private static int heuristic = 2;
    private static double smoothingCoefficient = 0.01;

    private static HashMap<String, Double> cosineTable;
    private static HashMap<String, HashMap<String, Integer>> branchTable;
    private static HashMap<String, TreeSet<String>> trieTable;

    private Map<String, Set<String>> baselineBoundaries = new ConcurrentHashMap<>();
    private Map<String, Integer> morphemeFreq = new ConcurrentHashMap<>();
    private CopyOnWriteArrayList<Sample> sampleList = new CopyOnWriteArrayList<>();


    public static double getSimUnfound() {
        return simUnfound;
    }

    public static double getSmoothingCoefficient() {
        return smoothingCoefficient;
    }

    public static int getHeuristic() {
        return heuristic;
    }

    public static double getLaplaceCoefficient() {
        return laplaceCoefficient;
    }

    public static double getSimUnsegmented() {
        return Math.log10(simUnsegmented);
    }

    public Map<String, Set<String>> getBaselineBoundaries() {
        return baselineBoundaries;
    }

    public CopyOnWriteArrayList<Sample> getSampleList() {
        return sampleList;
    }

    public Map<String, Integer> getMorphemeFreq() {
        return morphemeFreq;
    }

    public static double getLambda() {
        return lambda;
    }

    public static HashMap<String, Double> getNewCorpus() {
        return newCorpus;
    }

    public static double getNewCorpusSize() {
        return newCorpusSize;
    }

    public static HashMap<String, TreeSet<String>> getTrieTable() {
        return trieTable;
    }

    public static HashMap<String, HashMap<String, Integer>> getBranchTable() {
        return branchTable;
    }

    public static HashMap<String, Double> getCosineTable() {
        return cosineTable;
    }

    public Constant(String mapDir, String wordListDir, double lambda, int heuristic, double simUnsegmentedArg, double simUnfound) throws IOException, ClassNotFoundException {

        this.lambda = lambda;
        this.heuristic = heuristic;
        this.simUnsegmented = simUnsegmentedArg;
        this.simUnfound = simUnfound;
        List<String> freqWords = null;
        try {
            freqWords = Files.readAllLines(new File(wordListDir).toPath(), Charset.forName("UTF-8"));
        } catch (MalformedInputException e)

        {
            System.out.println(e.getMessage());
            freqWords = Files.readAllLines(new File(wordListDir).toPath(), Charset.forName("ISO-8859-9"));
        }
        generateTrieList(mapDir + "//similarityScoresToSerialize", mapDir + "//branchFactors", mapDir + "//trieWords");

        trieTable.keySet().parallelStream().forEach((n) -> {
            this.calculateFrequencyForMorp(n);
        });

        for (String str : freqWords) {
            StringTokenizer tokens = new StringTokenizer(str, " ");
            String f = tokens.nextToken();
            String w = tokens.nextToken();
            newCorpus.put(w, Double.parseDouble(f));
        }

        for (String str : newCorpus.keySet()) {
            newCorpusSize = newCorpusSize + newCorpus.get(str);
        }

        //createSmoothCorpus(corpus);
        //corpus.clear();
    }

    public void generateTrieList(String nameOfsimilarityScoresFile, String nameOfBranchFactors, String nameOfTrieWords) throws IOException, ClassNotFoundException {

        File filesOfsimilarityScoresFile = new File(nameOfsimilarityScoresFile);
        File filesOfBranchFactors = new File(nameOfBranchFactors);
        File filesOfTrieWords = new File(nameOfTrieWords);

        FileInputStream fis = new FileInputStream(filesOfsimilarityScoresFile);
        ObjectInput in = null;
        Object o = null;
        in = new ObjectInputStream(fis);
        o = in.readObject();
        fis.close();
        in.close();

        cosineTable = (HashMap<String, Double>) o;

        fis = new FileInputStream(filesOfBranchFactors);
        in = null;
        o = null;
        in = new ObjectInputStream(fis);
        o = in.readObject();
        fis.close();
        in.close();

        branchTable = (HashMap<String, HashMap<String, Integer>>) o;

        fis = new FileInputStream(filesOfTrieWords);
        in = null;
        o = null;
        in = new ObjectInputStream(fis);
        o = in.readObject();
        fis.close();
        in.close();

        trieTable = (HashMap<String, TreeSet<String>>) o;

        generateBoundaryListforBaseline(3); /// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    }

/*
    private void createSmoothCorpusWithAddition(Map<String, Double> corpus) {

        trieList.parallelStream().forEach((n) -> {
            for (String str : n.getWordList().keySet()) {
                if (!str.endsWith("$"))
                    newCorpus.put(str, laplaceCoefficient);
            }
        });

        for (String str : corpus.keySet()) {
            double value = corpus.get(str);
            newCorpus.put(str, (value + laplaceCoefficient));
        }

        for (String str : newCorpus.keySet()) {
            newCorpusSize = newCorpusSize + newCorpus.get(str);
        }

        corpus.clear();
    }
*/

    public void generateBoundaryListforBaseline(int childLimit) {

        for (String trie : branchTable.keySet()) {
            Set<String> boundaryList = new TreeSet<>();
            // for baseline
            for (String s : branchTable.get(trie).keySet()) {
                if (branchTable.get(trie).get(s) >= childLimit) {
                    boundaryList.add(s);
                }
            }
            baselineBoundaries.put(trie, boundaryList);
        }
    }

    private void calculateFrequencyForMorp(String trie) {

        Set<String> boundaries = baselineBoundaries.get(trie);

        ArrayList<String> tokens = new ArrayList<String>(); // unique elements?? set??
        for (String node : trieTable.get(trie)) {

            Stack<String> morphemeStack = new Stack<>();

            String current = "";
            boolean found = false;
            for (String boundary : boundaries) {
                if (node.startsWith(boundary) && !node.equals(boundary + "$")) {
                    current = boundary;
                    found = true;
                }
            }

            String morpheme = node.substring(current.length(), node.length() - 1);
            morphemeStack.add(morpheme);

            String word = node.substring(0, current.length());
            doSegmentation(word, boundaries, morphemeStack);

            String segmentation = morphemeStack.pop();
            int a = morphemeStack.size();
            for (int i = 0; i < a; i++) {
                String popped = morphemeStack.pop();
                segmentation = segmentation + "+" + popped;
            }
            tokens.addAll(tokenSegmentation(segmentation));
            sampleList.add(new Sample(node.substring(0, node.length() - 1), segmentation, trie));
        }

        for (String morpheme : tokens) {
            if (morphemeFreq.containsKey(morpheme)) {
                morphemeFreq.put(morpheme, morphemeFreq.get(morpheme) + 1);
            } else {
                morphemeFreq.put(morpheme, 1);
            }
        }
    }

    private void doSegmentation(String node, Set<String> boundaries, Stack<String> morphmeStack) {

        if (!node.equals("")) {
            String current = "";
            boolean found = false;
            for (String boundary : boundaries) {
                if (node.startsWith(boundary) && !node.equals(boundary)) {
                    current = boundary;
                    found = true;
                }
            }
            String morpheme = node.substring(current.length(), node.length());
            morphmeStack.add(morpheme);

            String word = node.substring(0, current.length());

            doSegmentation(word, boundaries, morphmeStack);
        }
    }

    public ArrayList<String> tokenSegmentation(String segmentation) {
        ArrayList<String> segments = new ArrayList<String>();
        StringTokenizer tokens = new StringTokenizer(segmentation, "+");
        while (tokens.hasMoreTokens()) {
            segments.add(tokens.nextToken());
        }
        return segments;
    }
}

package core.mcmc.withTries;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import tries.TrieST;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by ahmetu on 28.09.2016.
 */
public class Constant {

    private static WordVectors vectors;
    private static double lambda;
    private static HashMap<String, Double> newCorpus = new HashMap<>();
    private static double newCorpusSize = 0;
    private static List<TrieST> trieList = new ArrayList<>();
    private static List<String> searchedWordList = new ArrayList<>();
    private static double laplaceCoefficient = 0.0000001;
    private static double simUnfound = 0.0000001;
    private static double simUnsegmented;
    private static int heristic = 2;
    private Map<TrieST, Set<String>> baselineBoundaries = new ConcurrentHashMap<>();
    private Map<String, Integer> morphemeFreq = new ConcurrentHashMap<>();
    private CopyOnWriteArrayList<Sample> sampleList = new CopyOnWriteArrayList<>();

    public static Map<String, Double> getWordPairSimilarityMap() {
        return wordPairSimilarityMap;
    }

    private static Map<String, Double> wordPairSimilarityMap = new HashMap<>();

    static int baselineBranchNo = 1;

    public static int getHeristic() {
        return heristic;
    }

    public static double getLaplaceCoefficient() {
        return laplaceCoefficient;
    }

    public static double getSimUnsegmented() {
        return Math.log10(simUnsegmented);
    }

    public Map<TrieST, Set<String>> getBaselineBoundaries() {
        return baselineBoundaries;
    }

    public CopyOnWriteArrayList<Sample> getSampleList() {
        return sampleList;
    }

    public static List<TrieST> getTrieList() {
        return trieList;
    }

    public static List<String> getSearchedWordList() {
        return searchedWordList;
    }

    public Map<String, Integer> getMorphemeFreq() {
        return morphemeFreq;
    }

    public static WordVectors getVectors() {
        return vectors;
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

    public static double getSimUnfound() {
        return simUnfound;
    }

    public Constant(String triesDir, String vectorDir, String wordListDir, double lambda, int baselineBranchNoArg, double simUnsegmentedArg) throws IOException, ClassNotFoundException {

        this.vectors = WordVectorSerializer.loadTxtVectors(new File(vectorDir));
        this.lambda = lambda;
        baselineBranchNo = baselineBranchNoArg;
        this.simUnsegmented = simUnsegmentedArg;
        List<String> freqWords = Files.readAllLines(new File(wordListDir).toPath(), Charset.forName("UTF-8"));

        //Map<String, Double> corpus = new HashMap<>();

        generateTrieList(triesDir);

        this.trieList.parallelStream().forEach((n) -> {
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

    public void generateTrieList(String dir) throws IOException, ClassNotFoundException {

        File[] files = new File(dir + "/").listFiles();

        for (File f : files) {
            FileInputStream fis = new FileInputStream(f);
            ObjectInput in = null;
            Object o = null;
            in = new ObjectInputStream(fis);
            o = in.readObject();
            fis.close();
            in.close();

            TrieST trie = (TrieST) o;
            trieList.add(trie);
            searchedWordList.add(f.getName());
        }
        generateBoundaryListforBaseline(baselineBranchNo);
    }

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

    public void generateBoundaryListforBaseline(int childLimit) {

        for (TrieST st : trieList) {

            Map<String, Integer> WordList = new TreeMap<>(st.getWordList());
            Set<String> boundaryList = new TreeSet<>();
            // for baseline
            for (String s : WordList.keySet()) {
                if (WordList.get(s) >= childLimit) {
                    boundaryList.add(s);
                }
            }
            baselineBoundaries.put(st, boundaryList);
        }
    }

    private void calculateFrequencyForMorp(TrieST st) {

        Set<String> boundaries = baselineBoundaries.get(st);
        Map<String, Integer> nodeList = new TreeMap<>(st.getWordList());

        ArrayList<String> tokens = new ArrayList<String>(); // unique elements?? set??
        for (String node : nodeList.keySet()) {
            if (node.endsWith("$")) {

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
                sampleList.add(new Sample(node.substring(0, node.length() - 1), segmentation, st));
            }
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

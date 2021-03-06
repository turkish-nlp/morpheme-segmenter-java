package core.test;

import org.apache.commons.io.FileUtils;
import org.canova.api.util.MathUtils;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.eclipse.jetty.util.ConcurrentHashSet;
import tries.TrieST;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ahmet on 18.06.2016.
 */
public class BaselineTest {
    int lambda;

    public List<String> searchedWordList = new ArrayList<String>();
    public List<TrieST> trieList = new ArrayList<TrieST>();
    public Map<TrieST, ArrayList<String>> trieSegmentations = new ConcurrentHashMap<>(); // unique elements?? set??
    public Map<TrieST, ArrayList<String>> trieSegmentationsEachWord = new ConcurrentHashMap<>();

    public Map<String, Integer> morphemeFreq = new ConcurrentHashMap<>();
    public Map<TrieST, Set<String>> baselineBoundaries = new ConcurrentHashMap<>();
    public Map<TrieST, Double> boundarySimiliarScores = new ConcurrentHashMap<>();
    private WordVectors vectors;

    public Map<TrieST, Double> triePoisson = new ConcurrentHashMap<>();
    public double overallPoisson = 0;
    public double overallSimilarityScore = 0;
    public boolean oneLetter = false;

    public BaselineTest(String dir, String vectorDir, int lambda, Set<String> boundaries) throws IOException, ClassNotFoundException {

        generateTrieList(dir, boundaries);
        if (!oneLetter) {
            Set<String> boundaryListTMP = new TreeSet<>();
            for (TrieST st : baselineBoundaries.keySet()) {
                boundaryListTMP = new TreeSet<>();
                for (String str : baselineBoundaries.get(st)) {
                    if (str.length() > 1)
                        boundaryListTMP.add(str);
                }
                baselineBoundaries.put(st, boundaryListTMP);
            }
        }
        this.lambda = lambda;
        vectors = WordVectorSerializer.loadTxtVectors(new File(vectorDir));

        this.trieList.parallelStream().forEach((n) -> {
            this.calculateFrequencyForMorp(n);
        });

        this.trieList.parallelStream().forEach((n) -> {
            this.determineSegmentation(n);
        });

        overallPoisson = calculatePoissonOverall();
        overallSimilarityScore = calculateBaselineSimilarityScore();
        System.out.println("baseline is completed");
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        ConcurrentHashSet<String> boundaries = new ConcurrentHashSet();
        boundaries.add("lise");
        boundaries.add("okul");

        BaselineTest b = new BaselineTest(args[0], args[1], Integer.parseInt(args[2]), boundaries);
        //    b.saveModel();
    }

    public double calculateDPforTest() {
        /*int originalSize = 0;
        for (String str : baseFreqMap.keySet()) {

            originalSize = originalSize + baseFreqMap.get(str);
        }
        int size = originalSize;
        for (String str : toAddMap) {
            if (baseFreqMap.containsKey(str)) {
                if (baseFreqMap.get(str) > 0) {
                    newScore = newScore + Math.log10(Math.pow((baseFreqMap.get(str) / (size + alpha)), diffMap.get(str)));
                    size = size + diffMap.get(str);
                } else {
                    newScore = newScore + Math.log10(alpha * Math.pow(gamma, str.length() + 1) / (size + alpha));
                    size = size + diffMap.get(str);
                }
            } else {
                newScore = newScore + Math.log10(alpha * Math.pow(gamma, str.length() + 1) / (size + alpha));
                size = size + diffMap.get(str);
            }
        }
        */
        return 0.0;
    }


    public void saveModel() throws IOException {
        HashMap<String, Integer> morphemeFreqCopy = new HashMap<>();
        morphemeFreqCopy.putAll(this.morphemeFreq);
        // toByteArray
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] yourBytes = null;
        out = new ObjectOutputStream(bos);
        out.writeObject(morphemeFreqCopy);
        yourBytes = bos.toByteArray();

        bos.close();
        out.close();

        FileUtils.writeByteArrayToFile(new File("baselineModel"), yourBytes);
    }

    public double calculatePoissonOverall() {
        double poissonOverall = 0;
        for (TrieST st : trieList) {
            double poisson = calculatePoisson(st, baselineBoundaries.get(st));
            triePoisson.put(st, poisson);
            poissonOverall = poissonOverall + poisson;
        }
        return poissonOverall;
    }

    public double calculatePoisson(TrieST st, Set<String> boundaries) {
        double result = 0;
        for (String str : boundaries) {
            double tmp = poissonDistribution(st.getWordList().get(str));
            result = result + Math.log10(tmp);
        }
        return result;
    }

    public double poissonDistribution(int branchingFactor) {
        return (Math.pow(lambda, branchingFactor) * Math.exp(-1 * lambda)) / MathUtils.factorial(branchingFactor);
    }

    private void calculateFrequencyForPath(TrieST st) {

        Set<String> boundaries = baselineBoundaries.get(st);
        Map<String, Integer> nodeList = new TreeMap<>(st.getWordList());

        for (String boundary : boundaries) {
            nodeList.put(boundary + "$", 1);
        }

        for (String node : nodeList.keySet()) {
            if (node.endsWith("$")) {
                String current = "";
                for (String boundary : boundaries) {
                    if (node.startsWith(boundary) && !node.equals(boundary + "$")) {
                        current = boundary;
                    }
                }
                String morpheme = node.substring(current.length(), node.length() - 1);

/*                if (morphemeTrieList.containsKey(morpheme) && !morphemeTrieList.get(morpheme).contains(st)) {
                    morphemeTrieList.get(morpheme).add(st);
                } else {
                    CopyOnWriteArrayList tmp = new CopyOnWriteArrayList<>();
                    tmp.add(st);
                    morphemeTrieList.put(morpheme, tmp);
                }
*/
                if (morphemeFreq.containsKey(morpheme)) {
                    morphemeFreq.put(morpheme, morphemeFreq.get(morpheme) + 1);
                } else {
                    morphemeFreq.put(morpheme, 1);
                }
            }
        }
    }

    private Map<String, Integer> calculateFrequencyWithMapForPath(TrieST st, Set<String> boundaries) {

        Map<String, Integer> morphmeFrequencies = new HashMap<>();

        Map<String, Integer> nodeList = new TreeMap<>(st.getWordList());

        for (String boundary : boundaries) {
            nodeList.put(boundary + "$", 1);
        }

        for (String node : nodeList.keySet()) {
            if (node.endsWith("$")) {
                String current = "";
                for (String boundary : boundaries) {
                    if (node.startsWith(boundary) && !node.equals(boundary + "$")) {
                        current = boundary;
                    }
                }
                String morpheme = node.substring(current.length(), node.length() - 1);
                /*
                if (morphemeTrieList.containsKey(morpheme) && !morphemeTrieList.get(morpheme).contains(st)) {
                    morphemeTrieList.get(morpheme).add(st);
                } else {
                    CopyOnWriteArrayList tmp = new CopyOnWriteArrayList<>();
                    tmp.add(st);
                    morphemeTrieList.put(morpheme, tmp);
                }
                */
                if (morphmeFrequencies.containsKey(morpheme)) {
                    morphmeFrequencies.put(morpheme, morphmeFrequencies.get(morpheme) + 1);
                } else {
                    morphmeFrequencies.put(morpheme, 1);
                }
            }
        }
        return morphmeFrequencies;
    }

    private void calculateFrequencyForMorp(TrieST st) {

        Set<String> boundaries = baselineBoundaries.get(st);
        Map<String, Integer> nodeList = new TreeMap<>(st.getWordList());

        //   for (String boundary : boundaries) {
        //       if (nodeList.containsKey(boundary))
        //    //           nodeList.put(boundary + "$", 1);
        //     }

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

    private Map<String, Integer> calculateFrequencyWithMapForMorp(TrieST st, Set<String> boundaries) {

        Map<String, Integer> morphmeFrequencies = new HashMap<>();

        Map<String, Integer> nodeList = new TreeMap<>(st.getWordList());
        ArrayList<String> tokenSegments = new ArrayList<String>(); // unique elements?? set??

        //      for (String boundary : boundaries) {
        //          if (nodeList.containsKey(boundary))
        //      nodeList.put(boundary + "$", 1);
        //}

        for (String node : nodeList.keySet()) {
            if (node.endsWith("$")) {

                Stack<String> morphmeStack = new Stack<>();

                String current = "";
                boolean found = false;
                for (String boundary : boundaries) {
                    if (node.startsWith(boundary) && !node.equals(boundary + "$")) {
                        current = boundary;
                        found = true;
                    }
                }
                String morpheme = node.substring(current.length(), node.length() - 1); //   EXCEPTION
                morphmeStack.add(morpheme);

                String word = node.substring(0, current.length());
                doSegmentation(word, boundaries, morphmeStack);

                String segmentation = morphmeStack.pop();
                int a = morphmeStack.size();
                for (int i = 0; i < a; i++) {
                    String popped = morphmeStack.pop();
                    segmentation = segmentation + "+" + popped;
                }
                tokenSegments.addAll(tokenSegmentation(segmentation));
            }
        }

        for (String morpheme : tokenSegments) {
            if (morphmeFrequencies.containsKey(morpheme)) {
                morphmeFrequencies.put(morpheme, morphmeFrequencies.get(morpheme) + 1);
            } else {
                morphmeFrequencies.put(morpheme, 1);
            }
        }

        return morphmeFrequencies;
    }

    public Map<String, Integer> changeFrequencyOneTrie(TrieST st, Set<String> oldBoundaries, Set<String> newBoundaries, Map<String, Integer> originalFrequencies) {

        Map<String, Integer> candidateFrequencies = new ConcurrentHashMap<>(originalFrequencies);

        Map<String, Integer> oldMorphemeFreq = calculateFrequencyWithMapForMorp(st, oldBoundaries);
        Map<String, Integer> newMorphemeFreq = calculateFrequencyWithMapForMorp(st, newBoundaries);

        for (String morph : oldMorphemeFreq.keySet()) {
            if (candidateFrequencies.containsKey(morph)) {
                int freq = candidateFrequencies.get(morph) - oldMorphemeFreq.get(morph);
                candidateFrequencies.put(morph, freq);
            } else {
                System.out.println("BIG PROBLEM :)  " + morph);
            }
        }
        for (String morp : newMorphemeFreq.keySet()) {
            if (candidateFrequencies.containsKey(morp)) {
                int freq = candidateFrequencies.get(morp) + newMorphemeFreq.get(morp);
                candidateFrequencies.put(morp, freq);
            } else {
                candidateFrequencies.put(morp, newMorphemeFreq.get(morp));
            }
        }

        return candidateFrequencies;
    }

    public void determineSegmentation(TrieST st) {
        Set<String> boundaries = baselineBoundaries.get(st);
        Map<String, Integer> nodeList = new TreeMap<>(st.getWordList());

        ArrayList<String> tokens = new ArrayList<String>(); // unique elements?? set??
        ArrayList<String> segmentationsList = new ArrayList<>();
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
                segmentationsList.add(segmentation);
                tokens.addAll(tokenSegmentation(segmentation));
            }
        }
        trieSegmentationsEachWord.put(st, tokens);
        trieSegmentations.put(st, tokens);
    }

    public Map<TrieST, ArrayList<String>> changeSegmentSequenceForOneTrie(TrieST st, Set<String> oldBoundaries, Set<String> newBoundaries, Map<TrieST, ArrayList<String>> originalTrieSegments) {

        Map<TrieST, ArrayList<String>> candidateTrieSegments = new ConcurrentHashMap<>(originalTrieSegments);
        ArrayList<String> newSegmentsSeq = determineSegmentsForOneTrie(st, newBoundaries, false);
        candidateTrieSegments.put(st, newSegmentsSeq);
        return candidateTrieSegments;
    }

    public ArrayList<String> determineSegmentsForOneTrie(TrieST st, Set<String> boundaries, boolean print) {

        Map<String, Integer> nodeList = new TreeMap<>(st.getWordList());
        ArrayList<String> tokenSegments = new ArrayList<String>(); // unique elements?? set??

        for (String node : nodeList.keySet()) {
            if (node.endsWith("$")) {

                Stack<String> morphmeStack = new Stack<>();

                String current = "";
                boolean found = false;
                for (String boundary : boundaries) {
                    if (node.startsWith(boundary) && !node.equals(boundary + "$")) {
                        current = boundary;
                        found = true;
                    }
                }
                String morpheme = node.substring(current.length(), node.length() - 1); //   EXCEPTION
                morphmeStack.add(morpheme);

                String word = node.substring(0, current.length());
                doSegmentation(word, boundaries, morphmeStack);

                String segmentation = morphmeStack.pop();
                int a = morphmeStack.size();
                for (int i = 0; i < a; i++) {
                    String popped = morphmeStack.pop();
                    segmentation = segmentation + "+" + popped;
                }
                tokenSegments.addAll(tokenSegmentation(segmentation));
                if (print)
                    System.out.println(segmentation);
            }
        }
        return tokenSegments;
    }

    public ArrayList<String> determineSegmentsOfOneTrieForTrieSegmenter(TrieST st, Set<String> boundaries) {

        Map<String, Integer> nodeList = new TreeMap<>(st.getWordList());
        ArrayList<String> segmentations = new ArrayList<String>(); // unique elements?? set??

        for (String node : nodeList.keySet()) {
            if (node.endsWith("$")) {

                Stack<String> morphmeStack = new Stack<>();

                String current = "";
                boolean found = false;
                for (String boundary : boundaries) {
                    if (node.startsWith(boundary) && !node.equals(boundary + "$")) {
                        current = boundary;
                        found = true;
                    }
                }
                String morpheme = node.substring(current.length(), node.length() - 1); //   EXCEPTION
                morphmeStack.add(morpheme);

                String word = node.substring(0, current.length());
                doSegmentation(word, boundaries, morphmeStack);

                String segmentation = morphmeStack.pop();
                int a = morphmeStack.size();
                for (int i = 0; i < a; i++) {
                    String popped = morphmeStack.pop();
                    segmentation = segmentation + "+" + popped;
                }
                segmentations.add(segmentation);
            }
        }
        return segmentations;
    }

    public double calculateBaselineSimilarityScore() {
        double overalScore = 0;
        for (TrieST st : baselineBoundaries.keySet()) {
            double score = generateSimiliarWordsForOneTrie(st, baselineBoundaries.get(st));
            boundarySimiliarScores.put(st, score);
            overalScore = overalScore + score;
        }
        return overalScore;
    }


    public double generateSimiliarWordsForOneTrie(TrieST st, Set<String> boundaries) {
        double score = 0;
        for (String str : boundaries) {

            Set<String> similiar = getSimilartyBoundaryForOneNode(st, boundaries, str);
            double localScore = 0;
            for (String sim : similiar) {
                double smlrty = vectors.similarity(str, sim);
                if (smlrty < 0 || smlrty > 1)
                    smlrty = 0.000000000001;
                localScore = localScore + smlrty;
            }
            localScore = localScore / similiar.size();
            score = score + Math.log10(localScore);
        }
        return score;
    }


    public Set<String> getSimilartyBoundaryForOneNode(TrieST st, Set<String> boundaries, String boundaryNode) {

        Map<String, Integer> nodeList = new TreeMap<>(st.getWordList());
        ArrayList<String> allSegmentations = new ArrayList<>();
        for (String node : nodeList.keySet()) {
            if (node.endsWith("$")) {

                Stack<String> morphmeStack = new Stack<>();

                String current = "";
                boolean found = false;
                for (String boundary : boundaries) {
                    if (node.startsWith(boundary) && !node.equals(boundary + "$")) {
                        current = boundary;
                        found = true;
                    }
                }
                String morpheme = node.substring(current.length(), node.length() - 1); //   EXCEPTION
                morphmeStack.add(morpheme);

                String word = node.substring(0, current.length());
                doSegmentation(word, boundaries, morphmeStack);

                String segmentation = morphmeStack.pop();
                int a = morphmeStack.size();
                for (int i = 0; i < a; i++) {
                    String popped = morphmeStack.pop();
                    segmentation = segmentation + "+" + popped;
                }
                allSegmentations.add(segmentation);
            }
        }
        Set<String> similiar = new HashSet<>();
        for (String s : allSegmentations) {
            String replaced = s.replaceAll("\\+", "");
            if (replaced.startsWith(boundaryNode)) {
                String iterate = s;
                while (!iterate.startsWith(boundaryNode)) {
                    iterate = iterate.replaceFirst("\\+", "");
                }
                if (iterate.contains("+")) {
                    String tempIterate = iterate.replaceFirst("\\+", "");
                    if (tempIterate.contains("+")) {
                        similiar.add(tempIterate.substring(0, tempIterate.indexOf("+")));
                    } else {
                        similiar.add(tempIterate);
                    }
                }

            }
        }
        return similiar;
    }

    public ArrayList<String> tokenSegmentation(String segmentation) {
        ArrayList<String> segments = new ArrayList<String>();
        StringTokenizer tokens = new StringTokenizer(segmentation, "+");
        while (tokens.hasMoreTokens()) {
            segments.add(tokens.nextToken());
        }
        return segments;
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

    public void generateTrieList(String dir, Set<String> boundaries) throws IOException, ClassNotFoundException {

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
            baselineBoundaries.put(trie, boundaries);
        }
        //generateBoundaryListforBaseline(3); /// !!!!!!!!!!!!!!!!!!
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


    /*
    public Map<String, ArrayList<String>> getSimilarityWords(String selectedBoundary, ArrayList<String> segmentations) {
        for (String segment : segmentations) {
            if (segment.startsWith(selectedBoundary + "+")) {

            }
        }
    }*/

    //Getters
    public List<TrieST> getTrieList() {
        return trieList;
    }

    public Map<TrieST, ArrayList<String>> getTrieSegmentations() {
        return trieSegmentations;
    }

    public Map<String, Integer> getMorphemeFreq() {
        return morphemeFreq;
    }

    public List<String> getSearchedWordList() {
        return searchedWordList;
    }

    public Map<TrieST, Set<String>> getBaselineBoundaries() {
        return baselineBoundaries;
    }

}

   /*
    public Map<String, CopyOnWriteArrayList<TrieST>> getMorphemeTrieList() {
        return morphemeTrieList;
    }*/
    /*    private class Morpheme {

                public String name;
                public List<TrieST> stList = new ArrayList<TrieST>();
                public boolean isBoundary;

                public Morpheme(String name, boolean isBoundary) {
                    this.name = name;
                    this.isBoundary = isBoundary;
                }

                public void trieToList(TriST st)
                {
                    this.stList.add(st);
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (!(o instanceof Morpheme)) return false;

                    Morpheme morpheme = (Morpheme) o;

                    return name.equals(morpheme.name);

                }

                @Override
                public int hashCode() {
                    return name.hashCode();
                }
            }
        */
   /*
    public class Pair<F, S> {
        private F first; //first member of pair
        private S second; //second member of pair

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public void setFirst(F first) {
            this.first = first;
        }

        public void setSecond(S second) {
            this.second = second;
        }

        public F getFirst() {
            return first;
        }

        public S getSecond() {
            return second;
        }

    }
    */
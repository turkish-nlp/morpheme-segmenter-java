package core;

import com.sun.org.apache.xpath.internal.SourceTree;
import tries.TrieST;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by ahmet on 18.06.2016.
 */
public class FrequencyProcessor {


    public List<String> getSearchedWordList() {
        return searchedWordList;
    }

    private List<String> searchedWordList = new ArrayList<String>();
    private List<TrieST> trieList = new ArrayList<TrieST>();

    private Map<TrieST, ArrayList<String>> trieSegmentations = new ConcurrentHashMap<>(); // unique elements?? set??

    private Map<String, Integer> morphemeFreq = new ConcurrentHashMap<>();
    private Map<String, CopyOnWriteArrayList<TrieST>> morphemeTrieList = new ConcurrentHashMap<>();
    private Map<TrieST, Set<String>> wordBoundary = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException, ClassNotFoundException {


        FrequencyProcessor fp = new FrequencyProcessor();
        fp.generateTrieList(args[0]);
        fp.generateBoundaryListforBaseline(3);

        fp.trieList.parallelStream().forEach((n) -> {
            fp.determineSegmentation(n, fp.wordBoundary.get(n));
        });

        for (String key : fp.morphemeFreq.keySet()) {
            System.out.println(key + "-->" + fp.morphemeFreq.get(key));
        }
    }

  /*  private void calcuateFrequency(TrieST st, Set<String> boundaries) {
        Map<String, Integer> nodeList = new TreeMap<>(st.getWordList());

        for (String boundary : boundaries) {
            nodeList.put(boundary + "$", 1);
        }

        for (String node : nodeList.keySet()) {
            if (node.endsWith("$")) {
                String current = "";
                boolean found = false;
                for (String boundary : boundaries) {
                    if (node.startsWith(boundary) && !node.equals(boundary + "$")) {
                        current = boundary;
                        found = true;
                    }
                }
                String morpheme = node.substring(current.length(), node.length() - 1);
                if (morphemeTrieList.containsKey(morpheme) && !morphemeTrieList.get(morpheme).contains(st)) {
                    morphemeTrieList.get(morpheme).add(st);
                } else {
                    CopyOnWriteArrayList tmp = new CopyOnWriteArrayList<>();
                    tmp.add(st);
                    morphemeTrieList.put(morpheme, tmp);
                }
                if (morphemeFreq.containsKey(morpheme)) {
                    morphemeFreq.put(morpheme, morphemeFreq.get(morpheme) + 1);
                } else {
                    morphemeFreq.put(morpheme, 1);
                }
            }
        }
    }
    */

    // in order to avoid performing segmentation needlessly : normally, segmentation+frequency are performed together
    private Map<String, Integer> calcuateFrequencyWithMap(TrieST st, Set<String> boundaries) {

        Map<String, Integer> morphmeFrequencies = new HashMap<>();

        Map<String, Integer> nodeList = new TreeMap<>(st.getWordList());

        for (String boundary : boundaries) {
            nodeList.put(boundary + "$", 1);
        }

        for (String node : nodeList.keySet()) {
            if (node.endsWith("$")) {
                String current = "";
                boolean found = false;
                for (String boundary : boundaries) {
                    if (node.startsWith(boundary) && !node.equals(boundary + "$")) {
                        current = boundary;
                        found = true;
                    }
                }
                String morpheme = node.substring(current.length(), node.length() - 1);
                if (morphemeTrieList.containsKey(morpheme) && !morphemeTrieList.get(morpheme).contains(st)) {
                    morphemeTrieList.get(morpheme).add(st);
                } else {
                    CopyOnWriteArrayList tmp = new CopyOnWriteArrayList<>();
                    tmp.add(st);
                    morphemeTrieList.put(morpheme, tmp);
                }
                if (morphmeFrequencies.containsKey(morpheme)) {
                    morphmeFrequencies.put(morpheme, morphmeFrequencies.get(morpheme) + 1);
                } else {
                    morphmeFrequencies.put(morpheme, 1);
                }
            }
        }
        return morphmeFrequencies;
    }
    // changed to public
    public Pair<Map<String, Integer>, Map<TrieST, ArrayList<String>>> changePairForOneTrie(TrieST st, Set<String> oldBoundaries, Set<String> newBoundaries) {

        Map<String, Integer> candidateFrequencies = new ConcurrentHashMap<>(morphemeFreq);

        Map<String, Integer> oldMorphmeFrequencies = calcuateFrequencyWithMap(st, oldBoundaries);
        Pair<Map<String, Integer>, ArrayList<String>> newPair = determinePairForOneTrie(st, newBoundaries);
        Map<String, Integer> newMorphmeFrequencies = newPair.getFirst();

        for (String morph : oldMorphemeFreq.keySet()) {
            if (candidateFrequencies.containsKey(morph)) {
                int freq = candidateFrequencies.get(morph) - oldMorphemeFreq.get(morph);
                candidateFrequencies.put(morph, freq);
            } else {
                System.out.println("BIG PROBLEM :)");
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

        Map<TrieST, ArrayList<String>> candidateSegmentationList = new ConcurrentHashMap<>(trieSegmentations);
        candidateSegmentationList.put(st, newPair.getSecond());
        Pair<Map<String, Integer>, Map<TrieST, ArrayList<String>>> candidatePair = new Pair<>(candidateFrequencies, candidateSegmentationList);

        return candidatePair;
    }

    private void determineSegmentation(TrieST st, Set<String> boundaries) {

        Map<String, Integer> nodeList = new TreeMap<>(st.getWordList());

        for (String boundary : boundaries) {
            nodeList.put(boundary + "$", 1);
        }
        ArrayList<String> segments = new ArrayList<String>(); // unique elements?? set??
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
                    segmentation = segmentation + " + " + popped;
                }
                segments.addAll(tokenSegmentation(segmentation));
                //    System.out.println(segmentation);
            }
        }

        Map<String, Integer> tempFreq = new HashMap<>();

        for (String s : segments)
            if (!tempFreq.containsKey(s))
                tempFreq.put(s, Collections.frequency(segments, s));
        for (String str : tempFreq.keySet()) {
            if (morphemeFreq.containsKey(str)) {
                morphemeFreq.put(str, morphemeFreq.get(str) + 1);
            } else {
                morphemeFreq.put(str, 1);
            }
        }
        trieSegmentations.put(st, segments);
    }

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

    private Pair<Map<String, Integer>, ArrayList<String>> determinePairForOneTrie(TrieST st, Set<String> boundaries) {

        Map<String, Integer> nodeList = new TreeMap<>(st.getWordList());

        for (String boundary : boundaries) {
            nodeList.put(boundary + "$", 1);
        }

        ArrayList<String> candidateSegments = new ArrayList<String>(); // unique elements?? set??

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
                System.out.println("cur: " + current + "  " + "node: " + node);
                String morpheme = node.substring(current.length(), node.length() - 1); //   EXCEPTION
                morphmeStack.add(morpheme);

                String word = node.substring(0, current.length());
                doSegmentation(word, boundaries, morphmeStack);

                String segmentation = morphmeStack.pop();
                int a = morphmeStack.size();
                for (int i = 0; i < a; i++) {
                    String popped = morphmeStack.pop();
                    segmentation = segmentation + " + " + popped;
                }
                candidateSegments.addAll(tokenSegmentation(segmentation));
                //System.out.println(segmentation);
            }
        }

        Map<String, Integer> tempFreq = new HashMap<>();

        for (String s : candidateSegments)
            if (!tempFreq.containsKey(s))
                tempFreq.put(s, Collections.frequency(candidateSegments, s));

        Pair<Map<String, Integer>, ArrayList<String>> candidatePair = new Pair<>(tempFreq, candidateSegments);
        return candidatePair;
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
        generateBoundaryListforBaseline(3); /// !!!!!!!!!!!!!!!!!!
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
            wordBoundary.put(st, boundaryList);
        }
    }


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

    public List<TrieST> getTrieList() {
        return trieList;
    }

    public Map<TrieST, ArrayList<String>> getTrieSegmentations() {
        return trieSegmentations;
    }

    public Map<String, Integer> getMorphemeFreq() {
        return morphemeFreq;
    }

    public Map<String, CopyOnWriteArrayList<TrieST>> getMorphemeTrieList() {
        return morphemeTrieList;
    }

    public Map<TrieST, Set<String>> getWordBoundary() {
        return wordBoundary;
    }

}

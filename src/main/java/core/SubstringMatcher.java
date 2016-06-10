package core;

/**
 * Created by ahmetu on 25.04.2016.
 */

import com.google.common.primitives.Ints;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import tree.MorphemeGraph;
import tries.TrieST;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class SubstringMatcher {

    private Map<String, Double> stems = new HashMap<>();
    private Map<String, Double> affixes = new HashMap<>();
    private Map<String, Double> results = new HashMap<>();
    private Map<String, TrieST> graphList = new HashMap<>();
    public static ArrayList<String> stemsList = new ArrayList<String>();
    public ArrayList<String> stemCandi = new ArrayList<String>();

    private String fileSegmentationInput;
    private WordVectors vectors;
    int limit = 2;
    int childLimit = 3;
    private ConcurrentSkipListSet<String> set;

    public Map<String, Double> getStems() {
        return stems;
    }

    public Map<String, Double> getAffixes() {
        return affixes;
    }

    public Map<String, Double> getResults() {
        return results;
    }

    public Map<String, TrieST> getGraphList() {
        return graphList;
    }

    public SubstringMatcher(String fileVectorInput, String fileSegmentationInput) throws FileNotFoundException {
        vectors = WordVectorSerializer.loadTxtVectors(new File(fileVectorInput));
        this.fileSegmentationInput = fileSegmentationInput;
    }

    public int substring(String word, String n) {
        int common = 0;
        for (int i = 0; i < n.length(); i++) {
            if (word.startsWith(n.substring(0, i)) && !word.startsWith(n.substring(0, i + 1))) {
                common = i;
                break;
            }
        }
        if (common == 0)
            return n.length();
        else
            return common;
    }

    private void findMostFrequentLongestSubsequence(String word, double freq, int numberOfneighboors) throws FileNotFoundException, UnsupportedEncodingException {

        System.out.println("Control Word: " + word);
        PrintWriter writer = new PrintWriter("tries-NonRecursive/" + word + ".txt", "UTF-8");
        Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
        String stem = word;
        TrieST st = new TrieST();
        // In order to limit the control length limit; i<(word.lenght()-limit+1) can be used.
        int[] stem_candidates = new int[word.length() + 1];
        if (!neighboors.isEmpty()) {
            st.put(word + "$");

            for (String w : neighboors)
                st.put(w + "$");
        }

        Map<String, Integer> WordList = st.getWordList();

        for (String s : WordList.keySet()) {
            if (WordList.get(s) >= childLimit) {
                writer.println("(" + s + ", " + WordList.get(s) + ")");
            }
        }
        writer.close();
        System.out.println("For word >>>> " + word + " <<<< from root node to all leaf nodes, all paths: ");
        System.out.println("-------------------------------------------------------------------");

    }

    private void findMostFrequentLongestSubsequenceRecursive(String word, double freq, int numberOfneighboors) throws FileNotFoundException, UnsupportedEncodingException {

        System.out.println("Control Word: " + word);
        PrintWriter writer = new PrintWriter("trie/" + word + ".txt", "UTF-8");
        Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
        String firstWord = word;
        TrieST st = new TrieST();

        // In order to limit the control length limit; i<(word.lenght()-limit+1) can be used.
        int[] stem_candidates = new int[word.length() + 1];
        if (!neighboors.isEmpty()) {
            /*
            for (String s : neighboors) {
                System.out.println(s);
            }
            */
            st.put(word + "$");
            neighboors.parallelStream().forEach((n) -> {
                if(vectors.similarity(word, n) > 0.50)
                    recursiveAddLevelOne(firstWord, n, freq, numberOfneighboors, st);
            });
        }

        Map<String, Integer> WordList = st.getWordList();

        for (String s : WordList.keySet()) {
            if (WordList.get(s) >= childLimit) {
                 writer.println("(" + s + ", " + WordList.get(s) + ")");
            }
        }
        writer.close();
        System.out.println("For word >>>> " + word + " <<<< from root node to all leaf nodes, all paths: ");
        System.out.println("-------------------------------------------------------------------");

        graphList.put(word, st);
    }

    private void recursiveAddLevelOne(String firstWord, String word, double freq, int numberOfneighboors, TrieST st) {
        System.out.println("l1:" + word);
        if (st.put(word + "$")) {
            Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
            if (!neighboors.isEmpty()) {
                    neighboors.parallelStream().forEach((n) -> {
                    if(vectors.similarity(firstWord, n) > 0.50)
                        recursiveAdd(firstWord, n, freq, numberOfneighboors, st);
                });
            }
        }
    }

    private void recursiveAdd(String firstWord, String word, double freq, int numberOfneighboors, TrieST st) {
        System.out.println("l2:" + word);
        if (st.put(word + "$")) {
            //  System.out.println("Child_2: " + word);
            Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
            for (String w : neighboors) {
                if(vectors.similarity(firstWord, w) > 0.50)
                    recursiveAdd(firstWord, w, freq, numberOfneighboors, st);
            }
        }
    }


    private void findSegmentsAndAffixes() throws IOException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(fileSegmentationInput));

        String line;
        while ((line = reader.readLine()) != null) {
            String space = " ";
            StringTokenizer st = new StringTokenizer(line, space);

            double freq = Double.parseDouble(st.nextToken());
            String word = st.nextToken();
            boolean found = false;

            for (Map.Entry<String, TrieST> entry : graphList.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().contains(word))
                        found = true;
                }
            }
            if (!found) {
                findMostFrequentLongestSubsequenceRecursive(word, freq, 50);
            } else {
                found = false;
                System.out.println(word + " has been skipped");
            }
        }

    }

    public static void main(String[] args) throws IOException {
        SubstringMatcher ssm = new SubstringMatcher("outputs/vectors.txt", args[0]);
        ssm.findSegmentsAndAffixes();

        PrintWriter writer_stem = new PrintWriter("stemList.txt", "UTF-8");
        for (String s : stemsList)
            writer_stem.println(s);
        writer_stem.close();

        /*
        Map<String, Double> s = ssm.getStems();
        Map<String, Double> a = ssm.getAffixes();
        Map<String, Double> r = ssm.getResults();

        PrintWriter writer_seg = new PrintWriter("outputs/stems", "UTF-8");
        PrintWriter writer_af = new PrintWriter("outputs/affixes", "UTF-8");
        PrintWriter writer_res = new PrintWriter("outputs/results", "UTF-8");

        for (Map.Entry<String, Double> entry : s.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_seg.println(line);
        }
        writer_seg.close();

        for (Map.Entry<String, Double> entry : a.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_af.println(line);
        }
        writer_af.close();

        for (Map.Entry<String, Double> entry : r.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_res.println(line);
        }
        writer_seg.close();
        writer_af.close();
        writer_res.close();
        */
    }
}

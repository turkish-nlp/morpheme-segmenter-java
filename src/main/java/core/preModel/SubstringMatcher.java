package core.preModel;

/**
 * Created by ahmetu on 25.04.2016.
 */

import org.apache.commons.io.FileUtils;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import tries.TrieST;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class SubstringMatcher {

    private Map<String, TrieST> trieList = new HashMap<>();
    private Map<String, Integer> morphemeFreq = new TreeMap<>();
    private Map<String, Set<String>> wordBoundary = new ConcurrentHashMap<>();
    Set<String> set = new CopyOnWriteArraySet<>();//

    private String fileSegmentationInput;
    private WordVectors vectors;

    int childLimit = 3;

    String dir;

    public Map<String, TrieST> getTrieList() {
        return trieList;
    }

    public SubstringMatcher(String fileVectorInput, String fileSegmentationInput, String path) throws FileNotFoundException {
        vectors = WordVectorSerializer.loadTxtVectors(new File(fileVectorInput));
        this.fileSegmentationInput = fileSegmentationInput;
        dir = path;
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

    private void findMostFrequentLongestSubsequence(String word, double freq, int numberOfneighboors) throws IOException {

        System.out.println("Control Word: " + word);
        //PrintWriter writer = new PrintWriter("tries-NonRecursive/" + word + ".txt", "UTF-8");
        Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
        TrieST st = new TrieST();
        // In order to limit the control length limit; i<(word.lenght()-limit+1) can be used.
        if (!neighboors.isEmpty()) {
            st.put(word + "$");
            for (String w : neighboors)
            {
                st.put(w + "$");
                System.out.println(w);
            }
        }
        serializeToFile(st, word);


        Map<String, Integer> wordList = st.getWordList();
        Set<String> boundaryList = new TreeSet<>();
        // for baseline
        for (String s : wordList.keySet()) {
            if (wordList.get(s) >= childLimit) {
                boundaryList.add(s);
            }
        }
        wordBoundary.put(word, boundaryList);
        calcuateFrequency(st, boundaryList);

        for (String s : morphemeFreq.keySet()) {
            System.out.println(s + " --> " + morphemeFreq.get(s));
        }
        System.out.println("-----------------------------------------------------------------------------------");
        System.out.println("-----------------------------------------------------------------------------------");

        for (String s : wordList.keySet()) {
            System.out.println(s + " --> " + wordList.get(s));
        }

        System.out.println("-----------------------------------------------------------------------------------");
        System.out.println("For word >>>> " + word + " <<<< from root node to all leaf nodes, all paths: ");
        System.out.println("-----------------------------------------------------------------------------------");

    }

    private void findMostFrequentLongestSubsequenceRecursive(String word, double freq, int numberOfneighboors) throws IOException {

        System.out.println("Control Word: " + word);
        Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
        String firstWord = word;

        set.add(firstWord + "$");

        if (!neighboors.isEmpty()) {
            neighboors.parallelStream().forEach((n) -> {
                if (vectors.similarity(firstWord, n) > 0.50)
                    recursiveAdd(firstWord, n, freq, numberOfneighboors);
            });
        }
        TrieST st = new TrieST();

        for (String str : set) {
            st.put(str);
        }
        set.clear();
        serializeToFile(st, word);
        /*
        Map<String, Integer> WordList = st.getWordList();
        Set<String> boundaryList = new TreeSet<>();
        // for baseline
        for (String s : WordList.keySet()) {
            if (WordList.get(s) >= childLimit) {
                boundaryList.add(s);
            }
        }
        baselineBoundaries.put(word, boundaryList);
        calcuateFrequency(st, boundaryList);

        for (String s : morphemeFreq.keySet()) {
            System.out.println(s + " --> " + morphemeFreq.get(s));
        }
        */

        System.out.println("For word >>>> " + word + " <<<< from root node to all leaf nodes, all paths: ");
        System.out.println("-------------------------------------------------------------------");

        trieList.put(word, st);

    }

    private void serializeToFile(TrieST st, String word) throws IOException {
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

    private void recursiveAddLevelOne(String firstWord, String word, double freq, int numberOfneighboors) {
//        System.out.println("l1:" + word);
        if (set.add(word + "$")) {
            Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
            if (!neighboors.isEmpty()) {
                neighboors.parallelStream().forEach((n) -> {
                    if (vectors.similarity(firstWord, n) > 0.50)
                        recursiveAdd(firstWord, n, freq, numberOfneighboors);
                });
            }
        }
    }

    private void recursiveAdd(String firstWord, String word, double freq, int numberOfneighboors) {
        if (set.add(word + "$")) {
            Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
            for (String n : neighboors) {
                if (vectors.similarity(firstWord, n) > 0.50)
                    recursiveAdd(firstWord, n, freq, numberOfneighboors);
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

            for (Map.Entry<String, TrieST> entry : trieList.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().contains(word))
                        found = true;
                }
            }
            if (!found) {
                findMostFrequentLongestSubsequence(word, freq, 50);
            } else {
                found = false;
                System.out.println(word + " has been skipped");
            }
        } // end of trie creation trielist, boundarylist
    }

    private void calcuateFrequency(TrieST st, Set<String> boundaries) {
        Map<String, Integer> nodeList = st.getWordList();
        Set<String> wordList = new TreeSet<>();

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
                if (morphemeFreq.containsKey(morpheme)) {
                    morphemeFreq.put(morpheme, morphemeFreq.get(morpheme) + 1);
                } else {
                    morphemeFreq.put(morpheme, 1);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {

        /*
        * Vector dosyasını main methoda argüman olarak verilecek şekilde değiştiriyorum.
        *
         */
        SubstringMatcher ssm = new SubstringMatcher(args[0], args[1], args[2]);
        ssm.findSegmentsAndAffixes();
    }
}

package core;

/**
 * Created by ahmetu on 25.04.2016.
 */

import net.didion.jwnl.data.Word;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import tries.TrieST;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class SubstringMatcher {

    private Map<String, TrieST> trieList = new HashMap<>();
    private Map<String, Integer> morphemeFreq = new TreeMap<>();
    private Map<String, Set<String>> wordBoundary = new ConcurrentHashMap<>();

    private String fileSegmentationInput;
    private WordVectors vectors;

    int childLimit = 3;

    private ConcurrentSkipListSet<String> set = new ConcurrentSkipListSet<>();//

    public Map<String, TrieST> getTrieList() {
        return trieList;
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
        TrieST st = new TrieST();
        // In order to limit the control length limit; i<(word.lenght()-limit+1) can be used.
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
        Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
        String firstWord = word;

        set.add(firstWord + "$");

        if (!neighboors.isEmpty()) {
            neighboors.parallelStream().forEach((n) -> {
                if (vectors.similarity(firstWord, n) > 0.50)
                    recursiveAddLevelOne(firstWord, n, freq, numberOfneighboors);
            });
        }
        TrieST st = new TrieST();

        for (String str : set) {
            st.put(str);
        }

        Map<String, Integer> WordList = st.getWordList();
        Set<String> boundaryList = new TreeSet<>();
        // for baseline
        for (String s : WordList.keySet()) {
            if (WordList.get(s) >= childLimit) {
                boundaryList.add(s);
            }
        }
        wordBoundary.put(word, boundaryList);
        calcuateFrequency(st, boundaryList);

        for (String s : morphemeFreq.keySet()) {
            System.out.println(s + " --> " + morphemeFreq.get(s));
        }

        System.out.println("For word >>>> " + word + " <<<< from root node to all leaf nodes, all paths: ");
        System.out.println("-------------------------------------------------------------------");

        trieList.put(word, st);

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
                findMostFrequentLongestSubsequenceRecursive(word, freq, 50);
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

    /*
        public void getFrequency(Map<String, Integer> wordList, List<String> boundaries) {

            //Stack<String> boundaries = new Stack();

            String root = wordList.keySet().iterator().next();
            boolean isRoot = false;

            for (String s : wordList.keySet()) {
                if (!isRoot && wordList.get(s) >= 3) {
                    root = s;
                    isRoot = true;
                    boundaries.add(root);

                    if (morphemeList.containsKey(s)) {
                        morphemeList.put(s, morphemeList.get(s) + 1);
                    } else {
                        morphemeList.put(s, 1);
                    }
                } else if (s.startsWith(root) && isRoot) {
                    if (wordList.get(s) >= 3 && !s.endsWith("$")) {
                        String morph = "";

                        int size = boundaries.size();
                        for (int i = 0; i < size; i++) {
                            String surface = boundaries.peek();
                            if (s.startsWith(surface)) {
                                morph = s.substring(surface.length(), s.length());
                                boundaries.add(s);
                                break;
                            } else {
                                boundaries.pop();
                            }
                        }

                        if (morphemeList.containsKey(morph)) {
                            morphemeList.put(morph, morphemeList.get(morph) + 1);
                        } else {
                            morphemeList.put(morph, 1);
                        }
                    } else {
                        String morph = "";

                        int size = boundaries.size();
                        for (int i = 0; i < size; i++) {
                            String surface = boundaries.peek();
                            if (s.startsWith(surface)) {
                                morph = s.substring(surface.length(), s.length() - 1);
                                break;
                            } else {
                                boundaries.pop();
                            }
                        }

                        if (morphemeList.containsKey(morph)) {
                            morphemeList.put(morph, morphemeList.get(morph) + 1);
                        } else {
                            morphemeList.put(morph, 1);
                        }
                    }
                } else if (isRoot) {
                    root = s;
                    isRoot = false;
                }
            }
        }
    */
    public static void main(String[] args) throws IOException {

        /*
        * Vector dosyasını main methoda argüman olarak verilecek şekilde değiştiriyorum.
        *
         */
        SubstringMatcher ssm = new SubstringMatcher(args[0], "outputs/test.txt");
        ssm.findSegmentsAndAffixes();

        /*PrintWriter writer_stem = new PrintWriter("stemList.txt", "UTF-8");
        for (String s : stemsList)
            writer_stem.println(s);
        writer_stem.close();*/

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

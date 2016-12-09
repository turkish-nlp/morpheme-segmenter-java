package core.postProcess;

import org.apache.commons.io.FileUtils;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.eclipse.jetty.util.ConcurrentHashSet;
import tries.TrieST;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ahmetu on 30.11.2016.
 */
public class TrieToFiles {

    private List<TrieST> trieList = new ArrayList<>();
    private List<String> searchedWordList = new ArrayList<>();
    private HashSet<String> similarityKeys = new HashSet<>();
    private static WordVectors vectors;

    public ConcurrentHashMap<String, Double> concurrentSimilarityScores = new ConcurrentHashMap<>();


    public HashMap<String, Double> similarityScoresToSerialize = new HashMap<>();
    public HashMap<String, HashMap<String, Integer>> branchFactors = new HashMap<>();
    public HashMap<String, TreeSet<String>> trieWords = new HashMap<>();

    public TrieToFiles(String dir, String vectorDir, String outputDir) throws IOException, ClassNotFoundException {
        this.vectors = WordVectorSerializer.loadTxtVectors(new File(vectorDir));
        generateTrieList(dir);
        serialize(outputDir, similarityScoresToSerialize, "similarityScoresToSerialize");
        serialize(outputDir, trieWords, "trieWords");
        serialize(outputDir, branchFactors, "branchFactors");
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        TrieToFiles ttf = new TrieToFiles(args[0], args[1], args[2]);
/*
        for (String s : ttf.branchFactors.keySet()) {
            System.out.println(s + " : " + ttf.branchFactors.get(s));
        }

        System.out.println("----------");

        for (String s : ttf.trieWords.keySet()) {
            System.out.println(s + " : " + ttf.trieWords.get(s));
        }

        System.out.println("----------");

        for (String s : ttf.concurrentSimilarityScores.keySet()) {
            System.out.println(s + " : " + ttf.concurrentSimilarityScores.get(s));
        }*/


    }

    public void deSerialize(String dir) throws IOException, ClassNotFoundException {


        File f = new File(dir);

        FileInputStream fis = new FileInputStream(f);
        ObjectInput in = null;
        Object o = null;
        in = new ObjectInputStream(fis);
        o = in.readObject();
        fis.close();
        in.close();

        HashMap<String, Double> trieWords = (HashMap<String, Double>) o;
        System.out.println("");

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

        fillBranchFactorMap();
        fillSimilarityMap();
        System.out.println();
    }

    public void generateMapsFromFile(String dir) {

    }

    private void fillSimilarityMap() {

        ArrayList<Set<String>> setsForParallel = new ArrayList<>();
        int count = 0;
        HashSet<String> tmp = new HashSet<>();
        for (String w : similarityKeys) {
            if (count < 2000) {
                tmp.add(w);
                count++;
            }
            if (count == 2000) {
                count = 0;
                setsForParallel.add(tmp);
                tmp = new HashSet<>();
            }
        }
        setsForParallel.add(tmp);

        ConcurrentHashSet<String> tokens = new ConcurrentHashSet<>();

        for (Set<String> similarityKeys : setsForParallel) {
            similarityKeys.parallelStream().forEach((word) -> {
                for (int i = word.length(); i > 1; i--) {
                    tokens.add(word.substring(0, i));
                }
            });
        }

        setsForParallel = new ArrayList<>();
        count = 0;
        tmp = new HashSet<>();
        for (String w : tokens) {
            if (count < 2000) {
                tmp.add(w);
                count++;
            }
            if (count == 2000) {
                count = 0;
                setsForParallel.add(tmp);
                tmp = new HashSet<>();
            }
        }
        setsForParallel.add(tmp);

/*        for (Set<String> similarityKeys : setsForParallel) {
            for (String word : similarityKeys) {
                for (int i = word.length(); i > 2; i--) {
                    tokens.add(word.substring(0, i));
                }
            }
        }
*/

        for (Set<String> similarityKeys : setsForParallel) {
            similarityKeys.parallelStream().forEach((word) -> {
                for (int i = 1; i < word.length(); i++) {
                    String key = word.substring(0, i);
                    String keyCopy = word.substring(0, i);
                    keyCopy = keyCopy + "+" + word.substring(i);
                    double cosine = vectors.similarity(key, word);
                    if (!vectors.hasWord(key) || !vectors.hasWord(word) || cosine < 0 || cosine > 1) {
                        cosine = -1;
                    }
                    concurrentSimilarityScores.put(keyCopy, cosine);
                }
            });
        }

        for (String str : concurrentSimilarityScores.keySet()) {
            similarityScoresToSerialize.put(str, concurrentSimilarityScores.get(str));
        }


    }

    private void fillBranchFactorMap() {

        for (TrieST trie : trieList) {
            for (String word : trie.getWordList().keySet()) {
                if (!word.endsWith("$")) {
                    if (branchFactors.containsKey(searchedWordList.get(trieList.indexOf(trie)))) {
                        branchFactors.get(searchedWordList.get(trieList.indexOf(trie))).put(word, trie.getWordList().get(word));
                    } else {
                        HashMap<String, Integer> branches = new HashMap<>();
                        branches.put(word, trie.getWordList().get(word));
                        branchFactors.put(searchedWordList.get(trieList.indexOf(trie)), branches);
                    }
                } else {

                    similarityKeys.add(word.substring(0, word.length() - 1));

                    if (trieWords.containsKey(searchedWordList.get(trieList.indexOf(trie)))) {
                        trieWords.get(searchedWordList.get(trieList.indexOf(trie))).add(word);
                    } else {
                        TreeSet<String> words = new TreeSet<>();
                        words.add(word);
                        trieWords.put(searchedWordList.get(trieList.indexOf(trie)), words);
                    }
                }
            }
        }
    }

    private void serialize(String dir, Map toBeSerialized, String fileName) throws IOException {
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdir();
        }
        // toByteArray
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] yourBytes = null;
        out = new ObjectOutputStream(bos);
        out.writeObject(toBeSerialized);
        yourBytes = bos.toByteArray();

        bos.close();
        out.close();

        FileUtils.writeByteArrayToFile(new File(dir + "/" + fileName), yourBytes);
    }

}

package core.preModel;

import core.mcmc.utils.SerializableModel;
import org.apache.commons.io.FileUtils;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.eclipse.jetty.util.ConcurrentHashSet;
import tries.TrieST;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by Murathan on 15-Aug-16.
 */
public class trieFromCluster {
    Set<String> set = new CopyOnWriteArraySet<>();//
    String dir;
    Charset charset = Charset.forName("UTF-8");

    private List<TrieST> trieList = new ArrayList<>();
    private List<String> searchedWordList = new ArrayList<>();
    private HashSet<String> similarityKeys = new HashSet<>();
    private static WordVectors vectors;

    public ConcurrentHashMap<String, Double> concurrentSimilarityScores = new ConcurrentHashMap<>();


    public HashMap<String, Double> similarityScoresToSerialize = new HashMap<>();
    public HashMap<String, HashMap<String, Integer>> branchFactors = new HashMap<>();
    public HashMap<String, TreeSet<String>> trieWords = new HashMap<>();

    public trieFromCluster(String file, String path, String singleTrie, String vectorFile) throws IOException {

        this.vectors = null;//WordVectorSerializer.loadTxtVectors(new File(vectorFile));

        dir = path;
        if (!singleTrie.equalsIgnoreCase("true")) {
            List<String> words = Files.readAllLines(new File(file).toPath(), charset);

            for (String str : words) {
                System.out.println(str);
                buildTries(str);
            }
        } else {
            System.out.println("single_trie");
            List<String> wordsSingle = Files.readAllLines(new File(file).toPath(), charset);
            buildSingleTrie(wordsSingle);
        }

    }

    public static void main(String[] args) throws IOException {
        trieFromCluster tfc = new trieFromCluster(args[0], args[1], args[2], args[3]);
    }

    public void buildSingleTrie(List<String> all_words) throws IOException {
        TrieST st = new TrieST();

        String word = all_words.get(0);

        if (!all_words.isEmpty()) {
            st.put(word + "$");
            for (String w : all_words) {
                StringTokenizer token = new StringTokenizer(w);
                token.nextToken();
                String x = token.nextToken();
                System.out.println(x);
                st.put(x + "$");
                // System.out.println(w);
            }
        }

        //HashMap<String, HashMap<String, Integer>> singleTrie = fillBranchFactorMap(st, "tek_trie");
        trieList.add(st);
        searchedWordList.add("singleTrie");

        fillBranchFactorMap();
        fillSimilarityMap();

        serialize(dir, similarityScoresToSerialize, "similarityScoresToSerialize");
        serialize(dir, trieWords, "trieWords");
        serialize(dir, branchFactors, "branchFactors");
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

    private void serializeSingleTrie(HashMap<String, HashMap<String, Integer>> singleTrie, String dir) throws IOException {


        SerializableModel model = new SerializableModel(singleTrie);

        // toByteArray
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] yourBytes = null;
        out = new ObjectOutputStream(bos);
        out.writeObject(model);
        yourBytes = bos.toByteArray();

        bos.close();
        out.close();

        FileUtils.writeByteArrayToFile(new File(dir + "/singleTrie"), yourBytes);


    }

    private HashMap<String, HashMap<String, Integer>> fillBranchFactorMap(TrieST trie, String trieName) {

        HashMap<String, HashMap<String, Integer>> branchFactors = new HashMap<>();

        for (String word : trie.getWordList().keySet()) {
            if (!word.endsWith("$")) {
                if (branchFactors.containsKey(trieName)) {
                    branchFactors.get(trieName).put(word, trie.getWordList().get(word));
                } else {
                    HashMap<String, Integer> branches = new HashMap<>();
                    branches.put(word, trie.getWordList().get(word));
                    branchFactors.put(trieName, branches);
                }
            } /*else {

                similarityKeys.add(word.substring(0, word.length() - 1));

                if (trieWords.containsKey(searchedWordList.get(trieList.indexOf(trie)))) {
                    trieWords.get(searchedWordList.get(trieList.indexOf(trie))).add(word);
                } else {
                    TreeSet<String> words = new TreeSet<>();
                    words.add(word);
                    trieWords.put(searchedWordList.get(trieList.indexOf(trie)), words);
                }
            }*/
        }
        return branchFactors;
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

    private void writeToFile(TrieST trie, String fileName) throws IOException {

        FileOutputStream fos = new FileOutputStream(new File(fileName));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        for (String word : trie.getWordList().keySet()) {
            if (!word.endsWith("$")) {
                bw.write(word + "#" + trie.getWordList().get(word));
            } else {

            }
            bw.write("$" + word.substring(0, word.length() - 1) + "#" + trie.getWordList().get(word));
        }
    }

    public void buildTries(String line) throws IOException {
        TrieST st = new TrieST();
        String[] clusterTmp = line.split(" ");
        ArrayList<String> cluster = new ArrayList<>(Arrays.asList(clusterTmp));

        String word = cluster.get(0);
        if (!cluster.isEmpty()) {
            st.put(word + "$");
            for (String w : cluster) {
                st.put(w + "$");
                System.out.println(w);
            }
        }
        serializeToFile(st, word);
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
}

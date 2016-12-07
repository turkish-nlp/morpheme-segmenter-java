package core.preModel;

import jdk.nashorn.internal.objects.NativeRegExp;
import org.apache.commons.io.FileUtils;
import tries.TrieST;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by Murathan on 15-Aug-16.
 */
public class trieFromCluster {
    Set<String> set = new CopyOnWriteArraySet<>();//
    String dir;
    Charset charset = Charset.forName("UTF-8");

    public trieFromCluster(String file, String path, String singleTrie) throws IOException {
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
        trieFromCluster tfc = new trieFromCluster(args[0], args[1], args[2]);
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
        //serializeToFile(st, word);

        fillBranchFactorMap(st, "tek_trie");
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

    private void writeToFile(TrieST trie, String fileName) throws IOException {

        FileOutputStream fos = new FileOutputStream(new File(fileName));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        for (String word : trie.getWordList().keySet()) {
            if (!word.endsWith("$")) {
                bw.write(word + "#" + trie.getWordList().get(word));
            }
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

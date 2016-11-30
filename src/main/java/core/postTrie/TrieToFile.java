package core.postTrie;

import org.apache.commons.io.FileUtils;
import tries.TrieST;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ahmetu on 30.11.2016.
 */
public class TrieToFile {

    private List<TrieST> trieList = new ArrayList<>();
    private List<String> searchedWordList = new ArrayList<>();

    public HashMap<String, HashMap<String, Integer>> branchFactors = new HashMap<>();

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        TrieToFile ttf = new TrieToFile();
        ttf.generateTrieList("trieData");

        for (String s : ttf.branchFactors.keySet()) {
            System.out.println(s + " : " + ttf.branchFactors.get(s));
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

        fillBranchFactorMap();

    }

    private void fillBranchFactorMap() {

        for (TrieST trie : trieList) {
            for (String word : trie.getWordList().keySet()) {
                if (!word.endsWith("$")) {

                    if (branchFactors.containsKey(word)) {
                        branchFactors.get(word).put(searchedWordList.get(trieList.indexOf(trie)), trie.getWordList().get(word));
                    } else {
                        HashMap<String, Integer> branches = new HashMap<>();
                        branches.put(searchedWordList.get(trieList.indexOf(trie)), trie.getWordList().get(word));
                        branchFactors.put(word, branches);
                    }

                }
            }
        }
    }

    private void serialize(String dir, String fileName) throws IOException {
        // toByteArray
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] yourBytes = null;
        out = new ObjectOutputStream(bos);
        out.writeObject(branchFactors);
        yourBytes = bos.toByteArray();

        bos.close();
        out.close();

        FileUtils.writeByteArrayToFile(new File(dir + "/" + fileName), yourBytes);
    }

}

package core;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.*;
import org.apache.commons.io.output.ByteArrayOutputStream;
import tries.TrieST;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ahmet on 18.06.2016.
 */
public class FrequencyProcessor {

    private Map<String, TrieST> trieList = new HashMap<>();
    private Map<String, Integer> morphemeFreq = new ConcurrentHashMap<>();
    private Map<String, Set<String>> wordBoundary = new ConcurrentHashMap<>();


    public static void main(String[] args) throws IOException, ClassNotFoundException {

        FrequencyProcessor fp = new FrequencyProcessor();
        fp.generateTrieList(args[0]);
        fp.generateBoundaryListforBaseline(3);

        /*
        fp.trieList.keySet().parallelStream().forEach((n) -> {
            fp.calcuateFrequency(fp.trieList.get(n), fp.wordBoundary.get(n));
        });
        */


        for (String key : fp.trieList.keySet()) {
            fp.calcuateFrequency(fp.trieList.get(key), fp.wordBoundary.get(key));
        }


        for (String key : fp.morphemeFreq.keySet()) {
            System.out.println(key + "-->" + fp.morphemeFreq.get(key));
        }
    }

    private void calcuateFrequency(TrieST st, Set<String> boundaries) {
        Map<String, Integer> nodeList = st.getWordList();

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

    public void generateTrieList(String dir) throws IOException, ClassNotFoundException {

        File[] files = new File(dir + "/").listFiles();

        for (File f : files) {

            if (f.getName().startsWith("t_")) {
                FileInputStream fis = new FileInputStream(f);
                ObjectInput in = null;
                Object o = null;
                in = new ObjectInputStream(fis);
                o = in.readObject();
                fis.close();
                in.close();

                TrieST trie = (TrieST) o;
                trieList.put(f.getName(), trie);
            }
        }
    }

    public void generateBoundaryListforBaseline(int childLimit) {

        for (String key : trieList.keySet()) {
            TrieST st = trieList.get(key);

            Map<String, Integer> WordList = st.getWordList();
            Set<String> boundaryList = new TreeSet<>();
            // for baseline
            for (String s : WordList.keySet()) {
                if (WordList.get(s) >= childLimit) {
                    boundaryList.add(s);
                }
            }
            wordBoundary.put(key, boundaryList);
        }
    }

}

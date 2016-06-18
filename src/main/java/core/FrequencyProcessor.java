package core;

import com.sun.tools.javac.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.*;
import org.apache.commons.io.output.ByteArrayOutputStream;
import tries.TrieST;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ahmet on 18.06.2016.
 */
public class FrequencyProcessor {


    private class Morpheme {

        public String name;
        public List<TrieST> trieBelonged = new ArrayList<>();
        public boolean isBoundary;

        public Morpheme(String name, boolean isBoundary) {
            this.name = name;
            this.isBoundary = isBoundary;
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


    private Map<String, TrieST> trieList = new HashMap<>();
    private Map<String, Integer> morphemeFreq = new ConcurrentHashMap<>();
    private Map<String, Set<String>> wordBoundary = new ConcurrentHashMap<>();
    private Map<Morpheme, Integer> morphemeFreq_new = new ConcurrentHashMap<>();


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
            fp.determineSegmentation(fp.trieList.get(key), fp.wordBoundary.get(key));
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

    private void determineSegmentation(TrieST st, Set<String> boundaries) {

        Map<String, Integer> nodeList = st.getWordList();

        for (String boundary : boundaries) {
            nodeList.put(boundary + "$", 1);
        }

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
                String morpheme = node.substring(current.length(), node.length() - 1);
                morphmeStack.add(morpheme);

                String word = node.substring(0, current.length());
                doSegmentation(word, boundaries, morphmeStack);

                String segmentation = morphmeStack.pop();
                int a = morphmeStack.size();
                for (int i = 0; i < a; i++) {
                    String popped = morphmeStack.pop();
                    segmentation = segmentation + " + " + popped;
                    Morpheme tmp = new Morpheme(popped, boundaries.contains(popped));
                    if (!morphemeFreq_new.containsKey(tmp)) {
                        tmp.trieBelonged.add(st);
                        morphemeFreq_new.put(tmp, 1);
                    } else {
                        if (!tmp.trieBelonged.contains(st))
                            tmp.trieBelonged.add(st);
                        morphemeFreq_new.put(tmp, morphemeFreq_new.get(tmp) + 1);
                    }
                }
                System.out.println(segmentation);
            }
        }

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

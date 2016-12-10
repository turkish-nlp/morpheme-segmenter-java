package core.preModel;

import org.apache.commons.io.FileUtils;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import tries.TrieST;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by ahmet on 11.08.2016.
 */
public class RecursiveTrieBuilder {

    private Map<String, TrieST> trieList = new HashMap<>();
    Set<String> set = new CopyOnWriteArraySet<>();//

    private String fileSegmentationInput;
    private WordVectors vectors;

    double treshold = 0.25;

    String dir;

    public Map<String, TrieST> getTrieList() {
        return trieList;
    }

    public RecursiveTrieBuilder(String fileVectorInput, String fileSegmentationInput, String path) throws FileNotFoundException {
            vectors = WordVectorSerializer.loadTxtVectors(new File(fileVectorInput));
        this.fileSegmentationInput = fileSegmentationInput;
        dir = path;
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

    private void readInputFile() throws IOException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(fileSegmentationInput));

        String line;
        while ((line = reader.readLine()) != null) {
            String space = " ";
            StringTokenizer st = new StringTokenizer(line, space);

            double freq = Double.parseDouble(st.nextToken());
            String word = st.nextToken();
            boolean found = false;

            if (trieList.containsKey(word))
                found = true;

            if (!found) {
                buildTries(word, freq, 50);
            } else {
                found = false;
                System.out.println(word + " has been skipped");
            }
        } // end of trie creation trielist, boundarylist
    }

    private void buildTries(String word, double freq, int numberOfneighboors) throws IOException {

        Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);

        set.add(word + "$");

        if (!neighboors.isEmpty()) {
            String stem = findStem(word);
            neighboors.parallelStream().forEach((n) -> {
                if (n.startsWith(stem)) {
                    buildParalelly(stem, n, freq, numberOfneighboors);
                }
            });
        }

        TrieST st = new TrieST();

        for (String str : set) {
            st.put(str);
        }
        set.clear();
        serializeToFile(st, word);

        trieList.put(word, st);
    }

    private void buildParalelly(String stem, String word, double freq, int numberOfneighboors) {
        if (set.add(word + "$")) {
            Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
            neighboors.parallelStream().forEach((n) -> {
                if (n.startsWith(stem)) {
                    buildRecursively(stem, n, freq, numberOfneighboors);
                }
            });
        }
    }

    private void buildRecursively(String stem, String word, double freq, int numberOfneighboors) {
        if (set.add(word + "$")) {
            Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
            for (String n : neighboors) {
                if (n.startsWith(stem))
                    buildRecursively(stem, n, freq, numberOfneighboors);
            }
        }
    }

    public void deSerializeTriesToDebug() throws IOException, ClassNotFoundException {

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
            trieList.put(f.getName(), trie);
        }
    }

    private String findStem(String word) {

        String stemFound = null;

        Stack<String> localSuffixes = new Stack<String>();
        String stem = word;

        if (!vectors.hasWord(word)) {
            // do nothing
        } else {
            if (word.length() < 3) {
                // word is equals to stem
                stemFound = word;
            } else {
                int count = 0;
                for (int i = 0; i < word.length() - 2; i++) {
                    String candidate = stem.substring(0, stem.length() - count);
                    double cosine = vectors.similarity(stem, candidate);
                    if (cosine > treshold && cosine < 0.99) {
                        String affix = stem.substring(stem.length() - count, stem.length());

                        localSuffixes.push(affix);

                        stem = candidate;
                        count = 0;
                    }
                    count = count + 1;
                }
                stemFound = stem;
            }
        }
        return stemFound;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        /*
        * Vector dosyasını main methoda argüman olarak verilecek şekilde değiştiriyorum.
        *
         */
        RecursiveTrieBuilder rtb = new RecursiveTrieBuilder(args[0], args[1], args[2]);
        rtb.deSerializeTriesToDebug();
        for (String s : rtb.trieList.keySet()) {
            TrieST st = rtb.trieList.get(s);
            System.out.println("");
            System.out.println("--------------------------------->" + s + "<---------------------------------");
            for (String k : st.getWordList().keySet()) {
                if (k.endsWith("$"))
                    System.out.println(k);
            }
        }
    }

}

package core.mcmc;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import tries.TrieST;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ahmetu on 28.09.2016.
 */
public class Constant {

    private static WordVectors vectors;
    private static double lambda;
    private static ConcurrentHashMap<String, Double> newCorpus = new ConcurrentHashMap<>();
    private static double newCorpusSize = 0;
    private static List<TrieST> trieList = new ArrayList<>();
    private static List<String> searchedWordList = new ArrayList<>();
    private double laplaceCoefficient = 0.1;

    public static WordVectors getVectors() {
        return vectors;
    }

    public static double getLambda() {
        return lambda;
    }

    public static ConcurrentHashMap<String, Double> getNewCorpus() {
        return newCorpus;
    }

    public static double getNewCorpusSize() {
        return newCorpusSize;
    }

    public Constant(String triesDir, String vectorDir, String wordListDir, double lambda) throws IOException, ClassNotFoundException {
        this.vectors = WordVectorSerializer.loadTxtVectors(new File(vectorDir));
        this.lambda = lambda;

        List<String> freqWords = Files.readAllLines(new File(wordListDir).toPath(), Charset.forName("UTF-8"));
        Map<String, Double> corpus = new HashMap<>();

        generateTrieList(triesDir);

        for (String str : freqWords) {
            StringTokenizer tokens = new StringTokenizer(str, " ");
            String f = tokens.nextToken();
            String w = tokens.nextToken();
            corpus.put(w, Double.parseDouble(f));
        }

        createSmoothCorpus(corpus);
        corpus.clear();


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
        //generateBoundaryListforBaseline(3); /// !!!!!!!!!!!!!!!!!!
    }

    private void createSmoothCorpus(Map<String, Double> corpus) {

        trieList.parallelStream().forEach((n) -> {
            for (String str : n.getWordList().keySet()) {
                if (!str.endsWith("$"))
                    newCorpus.put(str, laplaceCoefficient);
            }
        });

        for (String str : corpus.keySet()) {
            double value = corpus.get(str);
            newCorpus.put(str, (value + laplaceCoefficient));
        }

        for (String str : newCorpus.keySet()) {
            newCorpusSize = newCorpusSize + newCorpus.get(str);
        }

        corpus.clear();
    }
}

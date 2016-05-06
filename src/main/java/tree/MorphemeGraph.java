package tree;

import org.apache.commons.collections.FastTreeMap;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

/**
 * Created by ahmet on 2.05.2016.
 */
public class MorphemeGraph {

    MorphemeNode root;
    private WordVectors vectors;
    private Map<String, Double> nodes;

    public MorphemeGraph(String rootNode, WordVectors vectors) {
        this.vectors = vectors;
        root = new MorphemeNode(rootNode, vectors);
    }

    public MorphemeNode get(String morpheme) {
        //      return root.get(morpheme);
        return null;
    }


    public void finish() {
        for (String s : nodes.keySet()) {
            root.addChild(new MorphemeNode(s, vectors), nodes.get(s));
        }
    }

    public void add(String s, double f) {
        if (nodes == null)
            nodes = new FastTreeMap();
        
        nodes.put(s, f);
    }

    public void print() {
        root.printTree();
    }

    public static void main(String[] args) throws FileNotFoundException {
        WordVectors vectors = WordVectorSerializer.loadTxtVectors(new File(args[0]));
        MorphemeGraph g = new MorphemeGraph("gel", vectors);
        g.add("gelmek", 3);
        g.add("gelmişti", 1);
        g.add("gelmiş", 1);
        g.add("gelmekti", 3);
        g.add("gelmekse", 3);
        g.add("geldi", 5);
        g.add("geldim", 8);
        g.add("geldin", 3);
        g.add("gelmektiyse", 10);
        g.add("gelmekseymiş", 20);

        g.finish();
        //      MorphemeNode a = g.get("gelmek");
        //     System.out.println("get: " + a);
        g.print();
    }
}

package tree;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by ahmet on 2.05.2016.
 */
public class MorphemeGraph {

    MorphemeNode root;
    private WordVectors vectors;

    public MorphemeGraph(String morp, WordVectors vectors) {
        this.vectors = vectors;
        root = new MorphemeNode(morp, vectors);
    }
    public MorphemeNode get(String morpheme)
    {
  //      return root.get(morpheme);
        return null;
    }
    public void add(String s, double f) {
        root.addChild(new MorphemeNode(s, vectors), f);
    }

    public void print() {
        root.print();
    }

    public static void main(String[] args) throws FileNotFoundException {
        WordVectors vectors = WordVectorSerializer.loadTxtVectors(new File(args[0]));
        MorphemeGraph g = new MorphemeGraph("gel", vectors);
        g.add("gelmek", 3);
        g.add("gelmekti", 3);
        g.add("gelmekse", 3);
        g.add("geldi", 5);
        g.add("geldim", 8);
        g.add("geldin", 3);
        g.add("gelmektimiş", 10);
        g.add("gelmekseymiş", 20);
  //      MorphemeNode a = g.get("gelmek");
   //     System.out.println("get: " + a);
        g.print();
    }
}

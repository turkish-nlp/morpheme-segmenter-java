package tree;

import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

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

    public boolean hasNode(String morpheme) {
        if(nodes == null)
            return false;
        else
            return nodes.containsKey(morpheme);
    }

    public void finish() {

        if (!(nodes == null)) {
            Map<String, Double> sorted_nodes = new TreeMap<>();
            for (String s : nodes.keySet()) {
                sorted_nodes.put(s, nodes.get(s));
            }

                for (String s : sorted_nodes.keySet()) {
                    root.addChild(new MorphemeNode(s, vectors), nodes.get(s));
                }
            }
    }

    public boolean add(String s, double f) {

        boolean has = true;
        if (nodes == null) {
            nodes = new ConcurrentHashMap<>();
            nodes.put(s, f);
        } else if (nodes.containsKey(s)) {
            has = false;
        } else {
            nodes.put(s, f);
        }
        return has;
    }

    public void print(String keyWord) throws FileNotFoundException, UnsupportedEncodingException {
        root.printTree(keyWord);
    }
    public static int substring(String word, String n)
    {
        int common = 0;
        for(int i=0;i < n.length();i++)
        {
            System.out.println(i + "-- " + n.substring(0,i));
            if(word.startsWith(n.substring(0,i)) && !word.startsWith(n.substring(0,i+1)))
            {
                common = i;
                break;
            }
        }
        if(common == 0)
            return n.length();
        else
            return common;
    }



    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {

        System.out.println("gelirken".substring(0, substring("gelirken", "gel")) );

           /*   MorphemeGraph g = new MorphemeGraph("gel", vectors);
  WordVectors vectors = WordVectorSerializer.loadTxtVectors(new File(args[0]));
        Collection<String> neighboors = vectors.wordsNearest("zorundadır", 50);
        for(String s: neighboors)
            System.out.println(s);



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
        g.print("gel");*/
    }
}

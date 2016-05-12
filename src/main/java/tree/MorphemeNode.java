package tree;

import org.apache.commons.collections.FastHashMap;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import java.util.*;

/**
 * Created by ahmet on 2.05.2016.
 */
public class MorphemeNode {

    private String morpheme;
    private MorphemeNode parent;
    private double cosineSimilarity;
    private Map<MorphemeNode, Double> children;
    private WordVectors vectors;
    private boolean isLeaf;
    private int size;

    public MorphemeNode(String morphemeName, WordVectors vectors) {
        morpheme = morphemeName;
        children = null;
        this.vectors = vectors;
        isLeaf = true;
        size = 0;
    }

    public Map<MorphemeNode, Double> getChildren() {
        return children;
    }

    public String getMorpheme() {
        return morpheme;
    }

    public double getCosineSimilarity() {
        return cosineSimilarity;
    }

    public MorphemeNode getParent() {
        return parent;
    }

    public void setParent(MorphemeNode parent) {
        this.parent = parent;
        setCosineSimilarity();
    }

    public int getSize()
    {
        return this.size;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }

    private void setCosineSimilarity() {

        if (vectors.hasWord(morpheme) && vectors.hasWord(parent.getMorpheme())){
            cosineSimilarity = vectors.similarity(morpheme, parent.getMorpheme());
        } else {
            cosineSimilarity = -0.5;
        }
    }

    public void addChild(MorphemeNode morpheme, double morphemeFreq) {

        if (children == null) {
            children = new FastHashMap();
            size = size + 1;
        }
        if (children.containsKey(morpheme)) {
            children.put(morpheme, children.get(morpheme) + morphemeFreq);
        } else {
            boolean found = false;
            for (MorphemeNode mn : children.keySet()) {
                if (morpheme.getMorpheme().contains(mn.getMorpheme())) {
                    mn.addChild(morpheme, morphemeFreq);
                    found = true;
                    break;
                }
            }
            if (!found) {
                children.put(morpheme, morphemeFreq);
                morpheme.setParent(this);
                this.setLeaf(false);
                size = size + 1;
            }

        }

    }

    public String toString() {
        return this.morpheme;
    }

    public void printTree() {

        System.out.println("");

        List<String> paths = new ArrayList<>();
        String path = "";
        print(paths, path);

        for (String s : paths) {
            System.out.println(s);
        }

        System.out.println("");
    }

    public void print(List<String> paths, String path) {

        if (!this.isLeaf) {
            path = path + this.morpheme + "-->";
            for (MorphemeNode mn : children.keySet()) {
                mn.print(paths, path);
            }
        } else {
            path = path + this.getMorpheme();
            paths.add(path);
        }
    }

}

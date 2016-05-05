package tree;

import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import java.util.HashMap;
import java.util.TreeMap;

/**
 * Created by ahmet on 2.05.2016.
 */
public class MorphemeNode {

    private String morpheme;
    private MorphemeNode parent;
    private double cosineSimilarity;
    private HashMap<MorphemeNode, Double> children;
    private WordVectors vectors;

    public MorphemeNode(String morphemeName, WordVectors vectors) {
        morpheme = morphemeName;
        children = null;
        this.vectors = vectors;
    }

    public HashMap<MorphemeNode, Double> getChildren() {
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

    private void setCosineSimilarity(){
        cosineSimilarity = vectors.similarity(morpheme, parent.getMorpheme());
    }

    public void addChild(MorphemeNode morpheme, double morphemeFreq) {

        if (children == null)
            children = new HashMap<MorphemeNode, Double>();

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
            }

        }
    }

    public String toString() {
        return this.morpheme;
    }

    public void print() {

        if (this.children != null) {
            // System.out.println(morpheme);
            for (MorphemeNode mn : children.keySet()) {
                System.out.println(morpheme);
                mn.print();
            }
        } else
            System.out.println(morpheme);
    }

}

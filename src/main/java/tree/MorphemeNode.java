package tree;

import java.util.HashMap;
import java.util.TreeMap;

/**
 * Created by ahmet on 2.05.2016.
 */
public class MorphemeNode {

    private String morpheme;
    private HashMap<MorphemeNode, Double> children;

    public MorphemeNode(String morphemeName) {
        morpheme = morphemeName;
        children = null;
    }

    public HashMap<MorphemeNode, Double> getChildren() {
        return children;
    }

    public String getMorpheme() {
        return morpheme;
    }

    public void addChild(MorphemeNode morpheme, double morphemeFreq) {
        if (children == null)
            children = new HashMap<MorphemeNode, Double>();

        if (children.containsKey(morpheme)) {
            children.put(morpheme, children.get(morpheme) + morphemeFreq);
        } else {
            MorphemeNode temp;
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
            }
        }
    }

    public void print() {

        if (this.children == null)
            System.out.println(morpheme);
        else {
            for (MorphemeNode mn : children.keySet()) {
                System.out.println(morpheme);
                mn.print();
            }
        }
    }
}

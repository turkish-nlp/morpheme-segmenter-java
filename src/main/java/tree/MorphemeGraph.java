package tree;

/**
 * Created by ahmet on 2.05.2016.
 */
public class MorphemeGraph {

    MorphemeNode root;

    public MorphemeGraph(String morp) {
        root = new MorphemeNode(morp);
    }

    public void add(String s, double f) {
        root.addChild(new MorphemeNode(s), f);
    }

    public void print() {
        root.print();
    }

    public static void main(String[] args) {
        MorphemeGraph g = new MorphemeGraph("gel");
        g.add("gelmek", 3);
        g.add("gelmekti", 3);
        g.add("gelmekse", 3);
        g.add("geldi", 5);
        g.add("geldim", 8);
        g.add("geldin", 3);
        g.add("gelmektimiş", 10);
        g.add("gelmekseymiş", 20);


        g.print();
    }
}

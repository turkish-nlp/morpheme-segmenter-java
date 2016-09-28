package core.mcmc;

import org.canova.api.util.MathUtils;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import tries.TrieST;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Created by ahmetu on 28.09.2016.
 */
public class Operations {

    public static double getCosineScore(String w1, String w2) {
        double smlrty = Constant.getVectors().similarity(w1, w2);
        if (smlrty < 0 || smlrty > 1)
            smlrty = 0.000000000001;
        return smlrty;
    }

    public static ArrayList<String> getSegments(String segmentation) {
        ArrayList<String> segments = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(segmentation, "+");

        String segment = "";
        while (tokenizer.hasMoreTokens()) {
            segment = segment + tokenizer.nextToken();
            segments.add(segment);
        }

        return segments;
    }

    public static double getPoissonScore(int branchingFactor, double lambda) {
        return (Math.pow(lambda, branchingFactor) * Math.exp(-1 * lambda)) / MathUtils.factorial(branchingFactor);
    }
}

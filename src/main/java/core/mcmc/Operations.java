package core.mcmc;

import org.canova.api.util.MathUtils;

import java.util.*;

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

    public static String biasedBinarySplit(String word) {
        Random rand = new Random();
        int splitPoint = rand.nextInt(5);

        if (splitPoint == 0 || splitPoint == word.length())
            return word;
        else
            return word.substring(0, word.length() - splitPoint) + "+" + word.substring(word.length() - splitPoint);
    }

    public static String randomSplitB(String word) {
        String segmentation = "";
        Random rand = new Random();

        int counter = 0;
        String subString = word;
        while (counter < word.length() - 1) {
            int splitPoint = Math.abs(rand.nextInt()) % subString.length();
            String segment = subString.substring(0, splitPoint);
            subString = subString.substring(splitPoint);
            if (segment.length() != 0)
                segmentation = segmentation + "+" + segment;
            counter = counter + splitPoint;
        }
        return (subString.length() == 0) ? segmentation.substring(1) : (segmentation.substring(1) + "+" + subString);
    }

    public static String randomSplit(String word) {
        String segmentation = "";
        Random rand = new Random();

        Set<Integer> splitPoints = new TreeSet<>();
        int size = rand.nextInt(word.length() + 1);
        if (size == word.length()) {
            for (char a : word.toCharArray()) {
                segmentation = segmentation + "+" + a;
            }
            segmentation = segmentation.substring(1);
        } else if (size == 0) {
            segmentation = word;
        } else {
            while (splitPoints.size() < size) {
                int n = rand.nextInt(word.length() - 1) + 1;
                splitPoints.add(n);
            }
            int f = 0;
            for (int i : splitPoints) {
                segmentation = segmentation + "+" + word.substring(f, i);
                f = i;
            }
            segmentation = segmentation.substring(1) + "+" + word.substring(f, word.length());
        }
        return segmentation;
    }

    public static void main(String[] args) {
        int a = 100;
        while (a > 0) {
            System.out.println(biasedBinarySplit("lise"));
            a--;
        }
    }
}

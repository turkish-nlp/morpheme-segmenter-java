package prob;

import java.util.*;

/**
 * Created by ahmetu on 02.05.2016.
 */
public class Utilities {

    public static ArrayList<String> getPossibleSegmentations(String word, Set<String> stems, Set<String> affixes) {

        ArrayList<String> segmentations = new ArrayList<String>();

        Stack<String> morphemeStack = new Stack<>();

        for (int i = 2; i < word.length() + 1; i++) {
            String stem = word.substring(0, i);
            String remaning = word.substring(i);

            if (stems.contains(stem)) {
                getPossibleAffixSequence(affixes, stem, remaning, segmentations);
            }
        }

        return segmentations;
    }

    private static void getPossibleAffixSequence(Set<String> affixes, String head, String tail, List<String> segmentations) {

        for (int i = 1; i < tail.length() + 1; i++) {
            String morpheme = tail.substring(0, i);

            if (morpheme.length() == tail.length()) {
                if (affixes.contains(morpheme)) {
                    segmentations.add(head + "+" + morpheme);
                }
            } else {
                String tailMorph = tail.substring(i);
                if (affixes.contains(morpheme)) {
                    String headMorph = head + "+" + morpheme;
                    getPossibleAffixSequence(affixes, headMorph, tailMorph, segmentations);
                }
            }
        }
    }

    public static void main(String[] args) {

        Map<String, Double> stems = new HashMap<>();
        stems.put("gel", 5d);
        stems.put("geliyor", 3d);
        stems.put("geliyormuş", 6d);

        Map<String, Double> affixes = new HashMap<>();
        affixes.put("iyor", 3d);
        affixes.put("muş", 2d);
        affixes.put("sun", 1d);
        affixes.put("muşsun", 1d);
        affixes.put("iyormuş", 1d);
        affixes.put("su", 1d);
        affixes.put("n", 1d);

        Set<String> s = stems.keySet();
        Set<String> a = affixes.keySet();

        long start = System.nanoTime();
        ArrayList<String> results = getPossibleSegmentations("geliyormuşsun", s, a);
        long stop = System.nanoTime();

        System.out.println(stop - start);
        System.out.println("***************");
        for (String r : results){
            System.out.println(r);
        }
    }

}

package prob;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by ahmetu on 02.05.2016.
 */
public class Utilities {

    public static List<String> getPossibleSegmentations(String word, Map<String, Double> stems, Map<String, Double> affixes) {

        ArrayList<String> segmentations = new ArrayList<String>();

        for (int i = 2; i < word.length() + 1; i++) {
            String stem = word.substring(0, i);
            String remaning = word.substring(i, word.length());

            if (stems.containsKey(stem)) {
                getPossibleAffixSequence(stem, remaning, segmentations);
            }
        }

        return segmentations;
    }

    private static void getPossibleAffixSequence(String stem, String remainingFromStem, List<String> segmentations) {
        ArrayList<String> affixSequences = new ArrayList<String>();
        /*
        DEVAMI GELECEK :)
         */
    }

    public static void main(String[] args) {
        getPossibleSegmentations("ahmet", null, null);
    }

}

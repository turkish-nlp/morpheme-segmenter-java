package core.journal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ahmetu on 30.09.2016.
 */
public class SerializableModel_bigram implements Serializable {


    private Map<String, Integer> serializedFrequencyTable;
    private Map<String, ArrayList<String>> serializedSegmentations;
    private Map<String, HashMap<String, Integer>> serializedBigrams;

    HashMap<String, HashMap<String, Integer>> singleTrie;

    private static final long serialVersionUID = -2557088637718891877L;

    public Map<String, Integer> getSerializedFrequencyTable() {
        return serializedFrequencyTable;
    }

    public Map<String, ArrayList<String>> getSerializedSegmentations() {
        return serializedSegmentations;
    }

    public Map<String, HashMap<String, Integer>> getSerializedBigrams() {
        return serializedBigrams;
    }

    public SerializableModel_bigram(Map<String, Integer> frequencyTable, Map<String, ArrayList<String>> segmentations, Map<String, HashMap<String, Integer>> bigramTable) {
        serializedFrequencyTable = new HashMap<>();
        serializedSegmentations = new HashMap<>();
        serializedBigrams = new HashMap<>();

        serializedFrequencyTable.putAll(frequencyTable);
        serializedSegmentations.putAll(segmentations);
        if (bigramTable != null) serializedBigrams.putAll(bigramTable);
    }

    public SerializableModel_bigram(HashMap<String, HashMap<String, Integer>> singleTrieArg) {
        singleTrie = new HashMap<>();
        singleTrie.putAll(singleTrieArg);
    }
}

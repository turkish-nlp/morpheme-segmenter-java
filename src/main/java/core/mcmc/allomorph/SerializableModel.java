package core.mcmc.allomorph;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by ahmetu on 30.09.2016.
 */
public class SerializableModel implements Serializable {


    private Map<String, Integer> serializedFrequencyTable;
    private Map<String, ArrayList<String>> serializedSegmentations;
    private CopyOnWriteArrayList<Sample> serializedSamples;

    HashMap<String, HashMap<String, Integer>> singleTrie;

    private static final long serialVersionUID = -2557088637718891877L;

    public Map<String, Integer> getSerializedFrequencyTable() {
        return serializedFrequencyTable;
    }

    public Map<String, ArrayList<String>> getSerializedSegmentations() {
        return serializedSegmentations;
    }

    public CopyOnWriteArrayList<Sample> getSerializedSamples() {
        return serializedSamples;
    }

    public SerializableModel(Map<String, Integer> frequencyTable, Map<String, ArrayList<String>> segmentations, CopyOnWriteArrayList<Sample> serializedSamplesArg) {
        serializedFrequencyTable = new HashMap<>();
        serializedSegmentations = new HashMap<>();
        serializedSamples = new CopyOnWriteArrayList<>();

        serializedFrequencyTable.putAll(frequencyTable);
        serializedSegmentations.putAll(segmentations);
        serializedSamples.addAll(serializedSamplesArg);
    }

    public SerializableModel(HashMap<String, HashMap<String, Integer>> singleTrieArg) {
        singleTrie = new HashMap<>();
        singleTrie.putAll(singleTrieArg);
    }
}

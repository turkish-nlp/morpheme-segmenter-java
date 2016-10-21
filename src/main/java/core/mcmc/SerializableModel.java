package core.mcmc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ahmetu on 30.09.2016.
 */
public class SerializableModel implements Serializable {


    private Map<String, Integer> serializedFrequencyTable;
    private Map<String, ArrayList<String>> serializedSegmentations;

    private static final long serialVersionUID = -2557088637718891877L;

    public Map<String, Integer> getSerializedFrequencyTable() {
        return serializedFrequencyTable;
    }

    public Map<String, ArrayList<String>> getSerializedSegmentations() {
        return serializedSegmentations;
    }

    public SerializableModel(Map<String, Integer> frequencyTable, Map<String, ArrayList<String>> segmentations) {
        serializedFrequencyTable = new HashMap<>();
        serializedSegmentations = new HashMap<>();

        serializedFrequencyTable.putAll(frequencyTable);
        serializedSegmentations.putAll(segmentations);
    }
}

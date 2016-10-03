package core.mcmc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by ahmetu on 30.09.2016.
 */
public class SerializableModel implements Serializable {
    public Map<String, Integer> serializedFrequencyTable;
    public Map<String, CopyOnWriteArrayList<String>> serializedSegmentations;

    public SerializableModel(Map<String, Integer> frequencyTable, Map<String, CopyOnWriteArrayList<String>> segmentations) {
        serializedFrequencyTable = new HashMap<>();
        serializedSegmentations = new HashMap<>();

        serializedFrequencyTable.putAll(frequencyTable);
        serializedSegmentations.putAll(segmentations);
    }
}

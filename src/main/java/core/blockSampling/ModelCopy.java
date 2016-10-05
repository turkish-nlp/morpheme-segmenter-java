package core.blockSampling;

import tries.TrieST;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Murathan on 04-Aug-16.
 */
public class ModelCopy implements Serializable {
    public HashMap<String, Integer> morphemeFreqCopy;
    public HashMap<TrieST, ArrayList<String>> trieSegmentationsCopy;

    ModelCopy(Map<String, Integer> morphemeFreq, Map<TrieST, ArrayList<String>> trieSegmentations) {
        morphemeFreqCopy = new HashMap<>();
        trieSegmentationsCopy = new HashMap<>();
        morphemeFreqCopy.putAll(morphemeFreq);
        trieSegmentationsCopy.putAll(trieSegmentations);
    }
}

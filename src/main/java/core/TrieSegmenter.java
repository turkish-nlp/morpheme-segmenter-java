package core;

import tries.TrieST;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Murathan on 04-Aug-16.
 */
public class TrieSegmenter {
    HashMap<String, Integer> morphemeFreq = new HashMap<>();
    HashMap<TrieST, ArrayList<String>> trieSegmentations = new HashMap<>();
    ConcurrentHashMap<String, CopyOnWriteArrayList<String>> possibleSegmentation = new ConcurrentHashMap<>();

    static Charset charset = Charset.forName("UTF-8");


    public void deSerialize(String file) throws IOException, ClassNotFoundException {

        FileInputStream fis = new FileInputStream(new File(file));
        ObjectInput in = null;
        Object o = null;
        in = new ObjectInputStream(fis);
        o = in.readObject();
        fis.close();
        in.close();

        ModelCopy model = (ModelCopy) o;

        morphemeFreq = model.morphemeFreqCopy;
        trieSegmentations = model.trieSegmentationsCopy;


        //for (String m : this.morphemeFreq.keySet())
        //    System.out.println(m + ": " + this.morphemeFreq.get(m));
    }

    public void findCorrectTries(String word) {


        for (TrieST st : trieSegmentations.keySet()) {
            for (String str : trieSegmentations.get(st)) {
                System.out.print(str + " ");
            }
            System.out.println();
        }

    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        TrieSegmenter trs = new TrieSegmenter();
        trs.deSerialize("model_10.0_0.01");
    }

    public void findGoldDataInTries(String dir) throws IOException {
        List<String> goldData = Files.readAllLines(new File(dir).toPath(), charset);
        for (String searchWord : goldData) {
            CopyOnWriteArrayList<String> triesWithSearchWord = new CopyOnWriteArrayList<>();

            trieSegmentations.keySet().parallelStream().forEach((st) -> {

                ArrayList<String> segmentations = trieSegmentations.get(st);
                for (String correct : segmentations) {
                    String word = correct.replace("+", "");
                    if (word.equals(searchWord)) {
                        triesWithSearchWord.add(correct);
                        break;
                    }
                }
                /*
                for (String str : st.getWordList().keySet()) {
                    if (str.equals(searchWord + "$")) {
                        String possibleSegment = findCorrectSegmentation(searchWord, st);
                        triesWithSearchWord.add(possibleSegment);
                        break;
                    }
                }
                */
            });

            possibleSegmentation.put(searchWord, triesWithSearchWord);
        }
    }

    /*
    public String findCorrectSegmentation(String searchWord, TrieST st) {

        ArrayList<String> segmentations = trieSegmentations.get(st);

        for (String correct : segmentations) {
            String word = correct.replace("+", "");
            if (word.equals(searchWord)) {
                return correct;
            }
        }

    }
    */
}

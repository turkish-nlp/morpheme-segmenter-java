package core;

import tries.TrieST;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Murathan on 04-Aug-16.
 */
public class TrieSegmenter {
    HashMap<String, Integer> morphemeFreq = new HashMap<>();
    HashMap<TrieST, ArrayList<String>> trieSegmentations = new HashMap<>();
    ConcurrentHashMap<String, CopyOnWriteArrayList<String>> possibleSegmentations = new ConcurrentHashMap<>();
    Map<String, Double> morphemeProb = new ConcurrentHashMap<>();
    Map<String, String> finalSegmentation = new ConcurrentHashMap<>();

    static String modeldir = "";
    static String datadir= "";
    static Charset charset = Charset.forName("UTF-8");


    // copied from Segmenter.java
    public void calculateProb() {
        int totalMorp = 0;
        for (String str : morphemeFreq.keySet())
            totalMorp = totalMorp + morphemeFreq.get(str);

        for (String str : morphemeFreq.keySet()) {
            // System.out.println(str + "-->" + (double) morphemeFreq.get(str) / totalMorp);
            morphemeProb.put(str, (double) morphemeFreq.get(str) / totalMorp);
        }
    }


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
        for (TrieST st : trieSegmentations.keySet())
            System.out.println(trieSegmentations.get(st));
        System.out.println("------");

    }


    public static void main(String[] args) throws IOException, ClassNotFoundException {
        TrieSegmenter trs = new TrieSegmenter();
        modeldir = args[0];
        datadir = args[1];
        trs.deSerialize(args[0]);
        trs.findGoldDataInTries(args[1]);
        trs.calculateProb();
        trs.findCorrectSegmentation(args[1]);

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
            });

            possibleSegmentations.put(searchWord, triesWithSearchWord);
        }
      /*  for (String str : possibleSegmentations.keySet())
            System.out.println(possibleSegmentations.get(str));
        System.out.println("!!!!!!!!!!!!!!");*/
    }

    public void findCorrectSegmentation(String dir) throws IOException, ClassNotFoundException {
        List<String> goldData = Files.readAllLines(new File(dir).toPath(), charset);
        for (String searchWord : goldData) {
            double maxScore = Double.NEGATIVE_INFINITY;
            String segMax = "";
            if (possibleSegmentations.get(searchWord).size() > 0) {

                CopyOnWriteArrayList<String> possibleSegmentList = possibleSegmentations.get(searchWord);
                for (String str : possibleSegmentList) {
                    double score = 0;
                    StringTokenizer token = new StringTokenizer(str, "+");
                    while (token.hasMoreTokens()) {
                        score = score + Math.log10(morphemeProb.get(token.nextToken()));
                    }
                    if (score > maxScore) {
                        maxScore = score;
                        segMax = str;
                    }
                }
            }
            else
            {
                segMax = this.doSplit(searchWord, morphemeFreq.keySet()).replaceAll(" ", "+");
                if(segMax.equals(""))
                    segMax = searchWord;
            }
            finalSegmentation.put(searchWord, segMax);

        }
        for (String str : finalSegmentation.keySet())
            System.out.println(finalSegmentation.get(str));
    }


    public String doSplit(String word, Set<String> affixes) throws FileNotFoundException {
        ArrayList<String> results = Segmenter.getPossibleSplits(word, affixes);
        String segMax = "";
        if (!results.isEmpty()) {
            segMax = "";
            double maxScore = Double.NEGATIVE_INFINITY;
            for (String str : results) {
                double tmp = 0;
                StringTokenizer st = new StringTokenizer(str, " ");
                while (st.hasMoreTokens()) {
                    tmp = tmp + Math.log10(morphemeProb.get(st.nextToken()));
                }

                //    tmp = tmp * ( 4 - StringUtils.countMatches(str, " ") )*-1;
                //   System.out.println(str + "-->" + tmp);
                if (tmp > maxScore) {
                    maxScore = tmp;
                    segMax = str;
                }
            }
            //  finalSegmentation.put(word, segMax);
        }
        return segMax;
    }
}
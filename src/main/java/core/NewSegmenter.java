package core;

import com.hazelcast.util.StringUtil;
import core.mcmc.SerializableModel;
import org.apache.commons.lang.StringUtils;
import tries.TrieST;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Murathan on 26-Jun-16.
 */
public class NewSegmenter {

    Map<String, Integer> morphemeFreq;
    Map<String, Double> morphemeProb;
    public Map<String, CopyOnWriteArrayList<String>> serializedSegmentations;
    static Charset charset = Charset.forName("UTF-8");

    Map<String, String> finalSegmentation;

    public NewSegmenter(String file, String inputFile, String mode) throws IOException, ClassNotFoundException {

        this.morphemeFreq = new ConcurrentHashMap<>();
        this.morphemeProb = new ConcurrentHashMap<>();
        this.finalSegmentation = new ConcurrentHashMap<>();
        deSerialize(file);
        readWords(inputFile);
        calculateProb();
        if(mode.equals("uni"))
            parallelSplit();
        else
            findCorrectSegmentation(inputFile);
    }

    public void calculateProb() {
        int totalMorp = 0;
        for (String str : morphemeFreq.keySet())
            totalMorp = totalMorp + morphemeFreq.get(str);

        for (String str : morphemeFreq.keySet()) {
            // System.out.println(str + "-->" + (double) morphemeFreq.get(str) / totalMorp);

            morphemeProb.put(str, (double) morphemeFreq.get(str) / totalMorp);
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        NewSegmenter s = new NewSegmenter(args[0], args[1], args[2]);

    }

    public void parallelSplit() {
        finalSegmentation.keySet().parallelStream().forEach((n) -> {
            try {
                doSplit(n, morphemeFreq.keySet());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });

        for (String str : finalSegmentation.keySet()) {
            System.out.println(str + "\t" + finalSegmentation.get(str));
        }
    }

    public void doSplit(String word, Set<String> affixes) throws FileNotFoundException {
        ArrayList<String> results = getPossibleSplits(word, affixes);
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
            finalSegmentation.put(word, segMax);
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

        SerializableModel model = (SerializableModel) o;

        morphemeFreq = model.serializedFrequencyTable;
        serializedSegmentations = model.serializedSegmentations;
    }


    public void readWords(String inputFile) throws IOException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(inputFile));

        String line;
        while ((line = reader.readLine()) != null) {
            String separator = " ";
            StringTokenizer st = new StringTokenizer(line, separator);

            int freq = Integer.parseInt(st.nextToken());
            String word = st.nextToken();
            finalSegmentation.put(word, "err");
        }
    }

    public static ArrayList<String> getPossibleSplits(String word, Set<String> affixes) throws FileNotFoundException {

        ArrayList<String> segmentations = new ArrayList<String>();

        for (int i = 1; i < word.length() + 1; i++) {
            String stem = word.substring(0, i);
            String remaning = word.substring(i);

            if (affixes.contains(stem)) {
                getPossibleAffixSequence(affixes, stem, remaning, segmentations);
            }
        }

        //if (news.isEmpty()){
        return segmentations;
        //} else {
        //return news;
        //}
    }

    private static void getPossibleAffixSequence(Set<String> affixes, String head, String tail, List<String> segmentations) {
        if (tail.length() == 0) {
            segmentations.add(head);
        } else if (tail.length() == 1) {
            if (affixes.contains(tail)) {
                segmentations.add(head + " " + tail);
            }
        } else {
            for (int i = 1; i < tail.length() + 1; i++) {
                String morpheme = tail.substring(0, i);

                if (morpheme.length() == tail.length()) {
                    if (affixes.contains(morpheme)) {
                        segmentations.add(head + " " + morpheme);
                    }
                } else {
                    String tailMorph = tail.substring(i);
                    if (affixes.contains(morpheme)) {
                        String headMorph = head + " " + morpheme;
                        getPossibleAffixSequence(affixes, headMorph, tailMorph, segmentations);
                    }
                }
            }
        }
    }
    public void findCorrectSegmentation(String dir) throws IOException, ClassNotFoundException {
        List<String> goldDataTmp = Files.readAllLines(new File(dir).toPath(), charset);
        List<String> goldData = new ArrayList<>();

        for (String str : goldDataTmp) {
            if (str.equals(""))
                break;
            StringTokenizer token2 = new StringTokenizer(str, " ");
            String w1 = token2.nextToken();
            String w2 = token2.nextToken();
            goldData.add(w2);
        }

        for (String searchWord : goldData) {
            double maxScore = Double.NEGATIVE_INFINITY;
            String segMax = "";
            if (serializedSegmentations.get(searchWord).size() > 0) { // if the search word exists in the tries
                CopyOnWriteArrayList<String> possibleSegmentList = serializedSegmentations.get(searchWord);
                for (String str : possibleSegmentList) {
                    double score = 0;
                    StringTokenizer token = new StringTokenizer(str, "+");
                    while (token.hasMoreTokens()) {
                        score = score + Math.log10(morphemeProb.get(token.nextToken()));
                    }
                    //      System.out.println(str + ": " + score);
                    if (score > maxScore) {
                        maxScore = score;
                        segMax = str;
                    }
                }
            } else// if the search word DOES NOT exists in the tries
            {
                segMax = this.doSplitMaxLikelihood(searchWord, morphemeFreq.keySet()).replaceAll(" ", "+");
                if (segMax.equals("")) // if the searchWord cannot be segmented
                    segMax = searchWord;
            }
            if (segMax.equals("")) // if the searchWord cannot be segmented
                segMax = searchWord;
            //    System.out.println("result: " + segMax);
            finalSegmentation.put(searchWord, segMax);

        }
        for (String str : finalSegmentation.keySet())
            System.out.println(str.replaceAll("ö", "O").replaceAll("ç", "C").replaceAll("ü", "U").replaceAll("ı", "I").replaceAll("ğ", "G").replaceAll("ü", "U").replaceAll("ş", "S")
                    + "\t" + finalSegmentation.get(str).replaceAll("\\+", " ").replaceAll("ö", "O").
                    replaceAll("ç", "C").replaceAll("ü", "U").replaceAll("ı", "I").replaceAll("ğ", "G").replaceAll("ü", "U").replaceAll("ş", "S"));
    }

    public String doSplitMaxLikelihood(String word, Set<String> affixes) throws FileNotFoundException {
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

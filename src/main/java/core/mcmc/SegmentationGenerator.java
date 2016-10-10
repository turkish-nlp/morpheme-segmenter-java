package core.mcmc;

import core.blockSampling.Segmenter;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Murathan on 26-Jun-16.
 */
public class SegmentationGenerator {

    private Map<String, Integer> morphemeFreq;
    private Map<String, Double> morphemeProb;
    private Map<String, String> finalSegmentation;
    private Map<String, CopyOnWriteArrayList<String>> serializedSegmentations;

    static Charset charset = Charset.forName("UTF-8");

    public SegmentationGenerator(String file, String inputFile, String mode) throws IOException, ClassNotFoundException {

        this.morphemeFreq = new ConcurrentHashMap<>();
        this.morphemeProb = new ConcurrentHashMap<>();
        this.finalSegmentation = new ConcurrentHashMap<>();
        this.serializedSegmentations = new ConcurrentHashMap<>();

        deSerialize(file);
        readWords(inputFile);
        calculateProb();
        if (mode.equals("uni"))
            parallelSplit();
        else
            findCorrectSegmentation(inputFile);

        printFinalSegmentations();
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
      /*  Set<String> affixes = new HashSet<>();
        affixes.add("ki");
        affixes.add("tap");
        affixes.add("k");
        affixes.add("i");
        affixes.add("in");
        affixes.add("ap");
        //affixes.put("n", 1d);
        affixes.add("t");
        affixes.add("itap");
        affixes.add("da");
        affixes.add("n");
        affixes.add("dan");


        ArrayList<String> results = getPossibleSplits("kitapdan", affixes);

        for (String s : results) {
            System.out.println(s);
        }

        System.out.println(s.morphemeFreq.size());
        for (String str : s.morphemeFreq.keySet()) {
                System.out.println(str + "-->" + s.morphemeFreq.get(str));
        }*/
        SegmentationGenerator s = new SegmentationGenerator(args[0], args[1], args[2]);

    }

    public void parallelSplit() {
        finalSegmentation.keySet().parallelStream().forEach((n) -> {
            try {
                doSplit(n, morphemeFreq.keySet());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    public void printFinalSegmentations() {
        for (String str : finalSegmentation.keySet())
            System.out.println(str.replaceAll("ö", "O").replaceAll("ç", "C").replaceAll("ü", "U").replaceAll("ı", "I").replaceAll("ğ", "G").replaceAll("ü", "U").replaceAll("ş", "S")
                    + "\t" + finalSegmentation.get(str).replaceAll("\\+", " ").replaceAll("ö", "O").
                    replaceAll("ç", "C").replaceAll("ü", "U").replaceAll("ı", "I").replaceAll("ğ", "G").replaceAll("ü", "U").replaceAll("ş", "S"));
    }

    public void doSplit(String word, Set<String> affixes) throws FileNotFoundException {
        ArrayList<String> results = getPossibleSplits(word, affixes);
        if (!results.isEmpty()) {
            String segMax = "";
            double maxScore = Double.NEGATIVE_INFINITY;
            for (String str : results) {
                double tmp = 0;
                StringTokenizer st = new StringTokenizer(str, "+");
                while (st.hasMoreTokens()) {
                    tmp = tmp + Math.log10(morphemeProb.get(st.nextToken()));
                }

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

        model.getSerializedFrequencyTable().keySet().parallelStream().forEach((s) -> {
            int freq = model.getSerializedFrequencyTable().get(s);
            if (freq != 0)
                morphemeFreq.put(s, freq);
        });

        model.getSerializedSegmentations().keySet().parallelStream().forEach((n) -> {
            CopyOnWriteArrayList<String> segmentations = new CopyOnWriteArrayList<String>();
            segmentations.addAll(model.getSerializedSegmentations().get(n));
            serializedSegmentations.put(n, segmentations);
        });
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

        return segmentations;
    }

    private static void getPossibleAffixSequence(Set<String> affixes, String head, String tail, List<String> segmentations) {
        if (tail.length() == 0) {
            segmentations.add(head);
        } else if (tail.length() == 1) {
            if (affixes.contains(tail)) {
                segmentations.add(head + "+" + tail);
            }
        } else {
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
    }

    public void findCorrectSegmentation(String dir) throws IOException, ClassNotFoundException {

        for (String searchWord : finalSegmentation.keySet()) {
            double maxScore = Double.NEGATIVE_INFINITY;
            String segMax = "";
            if (serializedSegmentations.containsKey(searchWord)) { // if the search word exists in the tries
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
            }
            if (segMax.equals("")) // if the searchWord cannot be segmented
                segMax = searchWord;
            //    System.out.println("result: " + segMax);
            finalSegmentation.put(searchWord, segMax);

        }
    }

    public String doSplitMaxLikelihood(String word, Set<String> affixes) throws FileNotFoundException {
        ArrayList<String> results = Segmenter.getPossibleSplits(word, affixes);
        String segMax = "";
        if (!results.isEmpty()) {
            double maxScore = Double.NEGATIVE_INFINITY;
            for (String str : results) {
                double tmp = 0;
                StringTokenizer st = new StringTokenizer(str, "+");
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
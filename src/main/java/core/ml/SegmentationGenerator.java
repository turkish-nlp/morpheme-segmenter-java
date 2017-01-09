package core.ml;

import core.blockSampling.Segmenter;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Integer.parseInt;

/**
 * Created by Murathan on 26-Jun-16.
 */
public class SegmentationGenerator {

    private Map<String, Integer> morphemeFreq;
    private Map<String, Double> morphemeProb;
    private Map<String, String> finalSegmentation;
    private Map<String, CopyOnWriteArrayList<String>> serializedSegmentations;
    private int threshold;
    static Charset charset = Charset.forName("UTF-8");
    private double alpha = 0.1;
    private double gamma = 0.1;
    private double totalSize;
    private String file;
    private String mode;
    private static WordVectors vectors;
    private boolean sim = false;
    private boolean NoUnseg = false;


    public Map<String, CopyOnWriteArrayList<String>> getSerializedSegmentations() {
        return serializedSegmentations;
    }
    public Map<String, String> getFinalSegmentation() {
        return finalSegmentation;
    }

    public SegmentationGenerator(String vectorDir, String file, String inputFile, String mode, int thresholdArg) throws IOException, ClassNotFoundException {

        if (sim)
            this.vectors = WordVectorSerializer.loadTxtVectors(new File(vectorDir));
        this.morphemeFreq = new ConcurrentHashMap<>();
        this.morphemeProb = new ConcurrentHashMap<>();
        this.finalSegmentation = new ConcurrentHashMap<>();
        this.serializedSegmentations = new ConcurrentHashMap<>();
        this.mode = mode;
        this.threshold = thresholdArg;
        this.file = file;
        deSerialize(file);
        readWords(inputFile);
        calculateProb();
        if (mode.equals("uni"))
            parallelSplit();
        else
            findCorrectSegmentation(inputFile);

        printFinalSegmentations();
        System.out.println(file + " is done!");
    }

    PrintWriter pw = new PrintWriter("FQs");
    PrintWriter pw2 = new PrintWriter("Ps");

    public void calculateProb() {
        int totalMorp = 0;
        for (String str : morphemeFreq.keySet()) {
            totalMorp = totalMorp + morphemeFreq.get(str);
            pw.println(str + " : " + morphemeFreq.get(str));
        }
        pw.close();

        for (String str : morphemeFreq.keySet()) {
            // System.out.println(str + "-->" + (double) morphemeFreq.get(str) / totalMorp);
            double prob = (double) morphemeFreq.get(str) / totalMorp;
            morphemeProb.put(str, prob);

            pw2.println(str + " : " + prob);
        }
        totalSize = totalMorp;
        pw2.close();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        int[] thresholdSet = {50};

        for (int i : thresholdSet) {
            System.out.println("THRESHOLD: " + i);
            File folder = new File(args[1]);
            File[] listOfFiles = folder.listFiles();
            for (File x : listOfFiles) {
                if (x.getPath().contains("MODEL")) {
                    SegmentationGenerator s = new SegmentationGenerator(args[0], x.getPath(), args[2], args[3], i);
                    Thread.sleep(100);
                }
            }
        }
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
    public void printFinalSegmentations() throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter("results_th50\\UTF8_OLD_SIM_" + sim + "_NOUNSEG_" + NoUnseg + "_th_" + threshold + "_" + mode + "_" + file.substring(file.indexOf("\\") + 1).replaceAll("finalMODEL-NOI_", ""), "UTF-8");
        if(file.contains("ger")) {
            System.out.println("!!!!!GERMAN FILE");
            for (String str : finalSegmentation.keySet()) {
                str = str.toLowerCase();
                writer.println(str.replaceAll("ä", "ae").replaceAll("ö", "oe").replaceAll("ü", "ue").replaceAll("ß", "ss")
                        + "\t" + finalSegmentation.get(str).toLowerCase().replaceAll("\\+", " ").replaceAll("ö", "O").
                        replaceAll("ä", "ae").replaceAll("ö", "oe").replaceAll("ü", "ue").replaceAll("ß", "ss") );
            }
            writer.close();
        }
        else
        {
            for (String str : finalSegmentation.keySet()) {
                str = str.toLowerCase();
                writer.println(str.replaceAll("ö", "O").replaceAll("ç", "C").replaceAll("ü", "U").replaceAll("ı", "I").replaceAll("ğ", "G").replaceAll("ü", "U").replaceAll("ş", "S")
                        + "\t" + finalSegmentation.get(str).toLowerCase().replaceAll("\\+", " ").replaceAll("ö", "O").
                        replaceAll("ç", "C").replaceAll("ü", "U").replaceAll("ı", "I").replaceAll("ğ", "G").replaceAll("ü", "U").replaceAll("ş", "S"));
            }
            writer.close();
        }
    }



    public void doSplit(String word, Set<String> affixes) throws FileNotFoundException {
        ArrayList<String> results = getPossibleSplits(word, affixes);

        if (NoUnseg) {
            if (results.contains(word))
                results.remove(word);
        }
        if (!results.isEmpty()) {
            String segMax = "";
            double maxScore = Double.NEGATIVE_INFINITY;
            for (String str : results) {
                double tmp = 0;
                StringTokenizer st = new StringTokenizer(str, "+");

                double simScoreOfCurrent = 0;
                // similarity
                if (sim) {
                    String morphemes[] = str.split("//+");
                    simScoreOfCurrent = 0;
                    String cur = "";
                    String next = morphemes[0];
                    for (int i = 0; i < morphemes.length - 1; i++) {
                        if (i + 1 < morphemes.length) {
                            cur = next;
                            next = next + morphemes[i + 1];
                        }
                        simScoreOfCurrent = simScoreOfCurrent + Math.log10(vectors.similarity(cur, next));
                    }
                }
             /*      // end of similarity

                while (st.hasMoreTokens()) {
                    String m = st.nextToken();
                    if (morphemeFreq.containsKey(m))
                        tmp = tmp + Math.log10(morphemeFreq.get(m) / (totalSize + alpha));
                    else
                        tmp = tmp + Math.log10(alpha * Math.pow(gamma, m.length() + 1) / (totalSize + alpha));
                }*/

                while (st.hasMoreTokens()) {
                    tmp = tmp + Math.log10(morphemeProb.get(st.nextToken()));
                }
                tmp = tmp + simScoreOfCurrent;
                if (tmp > maxScore) {
                    maxScore = tmp;
                    segMax = str;
                }
            }
            finalSegmentation.put(word, segMax);
            // System.out.println(segMax);
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
            if (freq > threshold)   // CHANGED!!!!!!
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

            int freq = parseInt(st.nextToken());
            String word = st.nextToken();
            finalSegmentation.put(word, word);
        }
    }

    public static ArrayList<String> getPossibleSplits(String word, Set<String> affixes) throws FileNotFoundException {

        ArrayList<String> segmentations = new ArrayList<String>();

        for (int i = 1; i < word.length() + 1; i++) {
            String stem = word.substring(0, i);
            String remaining = word.substring(i);

            if (affixes.contains(stem)) {
                getPossibleAffixSequence(affixes, stem, remaining, segmentations);
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
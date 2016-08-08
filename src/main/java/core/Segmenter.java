package core;

import com.hazelcast.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import tries.TrieST;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Murathan on 26-Jun-16.
 */
public class Segmenter {

    Map<String, Integer> morphemeFreq;
    Map<String, Double> morphemeProb;

    Map<String, String> finalSegmentation;

    public Segmenter(String file, String inputFile) throws IOException, ClassNotFoundException {

        this.morphemeFreq = new ConcurrentHashMap<>();
        this.morphemeProb = new ConcurrentHashMap<>();
        this.finalSegmentation = new ConcurrentHashMap<>();
        deSerialize(file);
        readWords(inputFile);
        calculateProb();
        parallelSplit();
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
        Segmenter s = new Segmenter(args[0], args[1]);

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

        HashMap<String, Integer> morphemeFreqCopy = (HashMap<String, Integer>) o;

        for (String str : morphemeFreqCopy.keySet())
            if (morphemeFreqCopy.get(str) > 0)
                this.morphemeFreq.put(str, morphemeFreqCopy.get(str));

        //for (String m : this.morphemeFreq.keySet())
        //    System.out.println(m + ": " + this.morphemeFreq.get(m));
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


}

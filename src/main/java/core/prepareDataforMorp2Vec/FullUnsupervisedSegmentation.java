package core.prepareDataforMorp2Vec;

import org.apache.commons.lang.StringUtils;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Created by ahmet on 02/04/2017.
 */
public class FullUnsupervisedSegmentation {

    public static WordVectors vectors;
    public static double threshold = 0.35;

    static {
        try {
            vectors = WordVectorSerializer.loadTxtVectors(new File("/Users/murathan/IdeaProjects/vectors.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getPossibleSplitsByStem(String word, String stem, int suffixNo) throws FileNotFoundException {

        List<String> pSegmentations = new ArrayList<String>();
        getPossibleAffixSequence(stem, word.substring(stem.length()), pSegmentations, suffixNo, 1);

        List<String> fSegmentations = new ArrayList<String>();
        for (String s : pSegmentations) {
            StringTokenizer st = new StringTokenizer(s, " ");
            String curr = st.nextToken() + st.nextToken();
            String next = "";
            boolean ok = true;
            while (st.hasMoreTokens()) {
                next = curr + st.nextToken();
                if (vectors.hasWord(curr) && vectors.hasWord(next) && (vectors.similarity(curr, next) > threshold && vectors.similarity(curr, next) < 1)) {
                    curr = next;
                } else {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                fSegmentations.add(s);
            }
        }

        return fSegmentations;
    }

    private static void getPossibleAffixSequence(String head, String tail, List<String> segmentations, int suffixNo, int level) {
        if (suffixNo == level) {
            segmentations.add(head + " " + tail);
        } else if (tail.length() == 0) {
            segmentations.add(head);
//        } else if (tail.length() == 1) {
//            segmentations.add(head + " " + tail);
        } else {
            for (int i = 1; i < tail.length() + 1; i++) {
                String morpheme = tail.substring(0, i);

                if (morpheme.length() != tail.length()) {
                    String tailMorph = tail.substring(i);
                    String headMorph = head + " " + morpheme;
                    getPossibleAffixSequence(headMorph, tailMorph, segmentations, suffixNo, level + 1);
                }
            }
        }
    }

    public static HashMap<String, Double> getAllPossibleSplits(String word, int morphemeNo) {
        List<String> tmpResults = new ArrayList<>();
        String tWord = word;
        int nM = morphemeNo - 1;

        List<int[]> all = getAllLists(tWord.length(), morphemeNo - 1);
        for (int[] i : all) {
            String nWord = "";
            int b = 0;
            for (int j = 0; j < nM; j++) {
                nWord = nWord + tWord.substring(b, i[j]) + " ";
                b = i[j];
            }
            tmpResults.add(nWord + " " + tWord.substring(i[nM - 1]));
        }
        HashMap<String, Double> results;
        double th = threshold;
        do {
            th = th - 0.05;
            results = getSplitByThreshold(tmpResults, th);
        } while ((results.size() == 0) && (th != 1) && th > 0);
        return results;
    }

    public static HashMap<String, Double> getSplitByThreshold(List<String> all, double threshold) {
        HashMap<String, Double> results = new HashMap<>();

        for (String s : all) {
            StringTokenizer st = new StringTokenizer(s, " ");
            String curr = st.nextToken();
            String next = "";
            boolean isOK = true;
            double sim = 0;
            while (st.hasMoreTokens()) {
                next = curr + st.nextToken();
                if (!(isOK = (vectors.hasWord(curr) && vectors.hasWord(next) && (vectors.similarity(curr, next) > threshold && vectors.similarity(curr, next) < 1)))) {
                    break;
                }
                sim = sim + vectors.similarity(curr, next);
                curr = next;
            }
            if (isOK){
                results.put(s, (sim/StringUtils.countMatches(s, " ")));
            }
        }
        return results;
    }

    public static List<int[]> getAllLists(int sWord, int nMorpheme) {

        int[] input = new int[sWord - 1];    // input array

        for (int i = 1; i < sWord; i++) {
            input[i - 1] = i;
        }
        int k = nMorpheme;                             // sequence length
        List<int[]> subsets = new ArrayList<>();

        int[] s = new int[k];                  // here we'll keep indices
        // pointing to elements in input array

        if (k <= input.length) {
            // first index sequence: 0, 1, 2, ...
            for (int i = 0; (s[i] = i) < k - 1; i++) ;
            subsets.add(getSubset(input, s));
            for (; ; ) {
                int i;
                // find position of item that can be incremented
                for (i = k - 1; i >= 0 && s[i] == input.length - k + i; i--) ;
                if (i < 0) {
                    break;
                } else {
                    s[i]++;                    // increment this item
                    for (++i; i < k; i++) {    // fill up remaining items
                        s[i] = s[i - 1] + 1;
                    }
                    subsets.add(getSubset(input, s));
                }
            }
        }
        return subsets;
    }

    // generate actual subset by index sequence
    private static int[] getSubset(int[] input, int[] subset) {
        int[] result = new int[subset.length];
        for (int i = 0; i < subset.length; i++)
            result[i] = input[subset[i]];
        return result;
    }

    private static String doNested(String word, double threshold) {

        Stack<String> localSuffixes = new Stack<String>();
        String stem = word;

        int count = 0;
        for (int i = 0; i < word.length() - 2; i++) {

            String candidate = stem.substring(0, stem.length() - count);
            if (vectors.hasWord(stem) && vectors.hasWord(candidate) && (vectors.similarity(stem, candidate) > threshold && vectors.similarity(stem, candidate) < 1)) {
                String affix = stem.substring(stem.length() - count, stem.length());

                localSuffixes.push(affix);

                stem = candidate;
                count = 0;
            }
            count = count + 1;
        }

        String result = stem;
        int suffixNo = localSuffixes.size();
        for (int j = 0; j < suffixNo; j++) {
            result = result + "+" + localSuffixes.pop();
        }

        return result;
    }

    public static void main(String[] args) throws IOException {
        Charset charset = Charset.forName("UTF-8");
        List<String> lines = Files.readAllLines(new File("DATAMOR2VEC/tur_10000.txt").toPath(), charset);
        HashMap<String, ArrayList<String>> wordSegMap = new HashMap<>();

        PrintWriter writer = new PrintWriter("seg.txt", "UTF-8");

        int[] morpNo = {2, 3, 4};
        int wordC = 0;
        int totalS = 0;
        for (String word : lines) {
            if (vectors.hasWord(word) && !word.trim().equals("")) {
                HashMap<String, Double> segs = new HashMap<>();
                for (int i : morpNo) {
                    segs.putAll(getAllPossibleSplits(word, i));
                }
                if(segs.keySet().size() > 10)
                {
                    Map<String, Double> result2 = new LinkedHashMap<>();
                    segs.entrySet().stream()
                            .sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(10)
                            .forEachOrdered(x -> result2.put(x.getKey(), x.getValue()));
                    segs.clear();
                    segs.putAll(result2);
                }
                ArrayList<String> segmen = new ArrayList<>(segs.keySet());
                if(segmen.size() < 10 && segmen.size() > 0)
                {
                    int size = segmen.size();
                    while(segmen.size() < 10)
                        segmen.add(segmen.get(size-1));
                }
                if(segmen.size() == 10){
                    wordC++;
                System.out.println(wordC);
                wordSegMap.put(word, segmen );
                totalS = totalS + segmen.size();}
                 /* */
            }
        }
        System.out.println(wordC);
        System.out.println((double) totalS / wordC);

            for (String word : wordSegMap.keySet()) {
                writer.print(word + ":");
                String toPrint = "";
                for(String seg: wordSegMap.get(word))
                {
                    seg = seg.replaceAll("  ", " ").replaceAll(" ", "-");
                    toPrint = toPrint + seg + "+";
                }
                writer.print( toPrint.substring(0,toPrint.length() -1));
                writer.println();
            }
        writer.close();


    }

}
        /*
        String tFile = "/Users/ahmet/Desktop/ccglab-test/test";

        BufferedReader reader = new BufferedReader(new FileReader(tFile));

        String line;
        while ((line = reader.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line, " ");
            String word = st.nextToken();

            System.out.println(doNested(word, threshold));
        }
        */

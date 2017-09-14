package core.prepareDataforMorp2Vec;

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
            vectors = WordVectorSerializer.loadTxtVectors(new File("C:\\Users\\Murathan\\github\\vectors.txt"));
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

    public static List<String> getAllPossibleSplits(String word, int morphemeNo) {

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
        List<String> results;
        double th = threshold;
        do {
            th = th - 0.05;
            results = getSplitByThreshold(tmpResults, th);
        } while ((results.size() == 0) && (th != 1) && th > 0);

        return results;
    }

    public static List<String> getSplitByThreshold(List<String> all, double threshold) {
        List<String> results = new ArrayList<>();

        for (String s : all) {
            StringTokenizer st = new StringTokenizer(s, " ");
            String curr = st.nextToken();
            String next = "";
            boolean isOK = true;
            while (st.hasMoreTokens()) {
                next = curr + st.nextToken();
                if (!(isOK = (vectors.hasWord(curr) && vectors.hasWord(next) && (vectors.similarity(curr, next) > threshold && vectors.similarity(curr, next) < 1)))) {
                    break;
                }
                curr = next;
            }
            if (isOK) results.add(s);
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
        List<String> lines = Files.readAllLines(new File("DATAMOR2VEC\\tur_10000.txt").toPath(), charset);
        HashMap<String, ArrayList<String>> wordSegMap = new HashMap<>();

        int[] morpNo = {2, 3, 4};
        int wordC = 0;
        int totalS = 0;
        for (String word : lines) {
            if (vectors.hasWord(word)) {
                System.out.println("==========================================================");
                int seg = 0;
                ArrayList<String> segs = new ArrayList<>();
                for (int i : morpNo) {
                    for (String s : getAllPossibleSplits(word, i)) {
                        //System.out.println(s);
                        seg++;
                        segs.add(s);
                    }
                }
                if(segs.size() > 10)
                {
                    Collections.shuffle(segs);
                    segs = new ArrayList<>(segs.subList(0,10));
                }
                if(segs.size() < 10 && segs.size() > 0)
                {
                    int size = segs.size();
                    while(segs.size() < 10)
                    {
                        segs.add(segs.get(size-1));
                    }
                }

                if(segs.size() > 1)
                    wordC++;
                wordSegMap.put(word, segs);
                // System.out.println("==========================================================  " + word + " " + seg);
                totalS = totalS + segs.size();
                 /* */
            }
        }
        System.out.println(wordC);
        System.out.println((double) totalS / wordC);

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

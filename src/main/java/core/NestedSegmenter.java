package core;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.jboss.logging.Logger;

import java.io.*;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * Created by ahmetu on 29.04.2016.
 */
public class NestedSegmenter {

    private Map<String, Integer> stems = new TreeMap<String, Integer>();
    private Map<String, Integer> affixes = new TreeMap<String, Integer>();
    private Map<String, Integer> results = new TreeMap<String, Integer>();
    private Map<String, Integer> notFound = new TreeMap<String, Integer>();

    private String fileSegmentationInput;

    private WordVectors vectors;

    public Map<String, Integer> getStems() {
        return stems;
    }

    public Map<String, Integer> getAffixes() {
        return affixes;
    }

    public Map<String, Integer> getResults() {
        return results;
    }

    public Map<String, Integer> getNotFound() {
        return notFound;
    }

    public NestedSegmenter(String fileVectorInput, String fileSegmentationInput) {
        try {
            vectors = WordVectorSerializer.loadTxtVectors(new File(fileVectorInput));
            this.fileSegmentationInput = fileSegmentationInput;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void doNested(String word, int frequency, double treshold) {

        Stack<String> localSuffixes = new Stack<String>();
        String stem = word;

        if (!vectors.hasWord(word)) {
            if (notFound.containsKey(word)) {
                notFound.put(word, notFound.get(word) + frequency);
            } else {
                notFound.put(word, frequency);
            }
        } else {
            if (word.length() < 3){
                if (stems.containsKey(word)){
                    stems.put(word, stems.get(word) + frequency);
                } else {
                    stems.put(word, frequency);
                }
            } else {
                int count = 0;
                for (int i=0; i < word.length()-2; i++){
                    String candidate = stem.substring(0, stem.length()-count);
                    double cosine = vectors.similarity(stem, candidate);
                    if (cosine > treshold && cosine < 1d){
                        String affix = stem.substring(stem.length()-count, stem.length());

                        localSuffixes.push(affix);

                        if (affixes.containsKey(affix)){
                            affixes.put(affix, affixes.get(affix)+frequency);
                        } else {
                            affixes.put(affix, frequency);
                        }

                        stem = candidate;
                        count = 0;
                    }
                    count = count + 1;
                }

                if (stems.containsKey(stem)) {
                    stems.put(stem, stems.get(stem) + frequency);
                } else {
                    stems.put(stem, frequency);
                }

                String result = stem;
                int suffixNo = localSuffixes.size();
                for (int j=0; j<suffixNo; j++){
                    result = result + "+" + localSuffixes.pop();
                }

                if (results.containsKey(result)) {
                    results.put(result, results.get(result) + frequency);
                } else {
                    results.put(result, frequency);
                }
            }
        }
    }

    private void findSegmentsAndAffixes() {
        try {

            BufferedReader reader = null;
            reader = new BufferedReader(new FileReader(fileSegmentationInput));

            String line;
            while ((line = reader.readLine()) != null) {
                String space = " ";
                StringTokenizer st = new StringTokenizer(line, space);

                int freq = Integer.parseInt(st.nextToken());
                String word = st.nextToken();

                doNested(word, freq, 0.25);

            }
        } catch (Exception ex) {
            Logger.getLogger(NestedSegmenter.class).log(Logger.Level.ERROR, ex.getLocalizedMessage());
        }
    }

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        NestedSegmenter ns = new NestedSegmenter(args[0], args[1]);
        ns.findSegmentsAndAffixes();

        Map<String, Integer> s = ns.getStems();
        Map<String, Integer> a = ns.getAffixes();
        Map<String, Integer> r = ns.getResults();
        Map<String, Integer> n = ns.getNotFound();

        PrintWriter writer_seg = new PrintWriter("outputs/stems", "UTF-8");
        PrintWriter writer_af = new PrintWriter("outputs/affixes", "UTF-8");
        PrintWriter writer_res = new PrintWriter("outputs/results", "UTF-8");
        PrintWriter writer_noF = new PrintWriter("outputs/absent", "UTF-8");

        for (Map.Entry<String, Integer> entry : s.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_seg.println(line);
        }
        writer_seg.close();

        for (Map.Entry<String, Integer> entry : a.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_af.println(line);
        }
        writer_af.close();

        for (Map.Entry<String, Integer> entry : r.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_res.println(line);
        }

        for (Map.Entry<String, Integer> entry : n.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_noF.println(line);
        }

        writer_seg.close();
        writer_af.close();
        writer_res.close();
        writer_noF.close();
    }
}

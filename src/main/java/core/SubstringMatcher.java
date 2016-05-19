package core;

/**
 * Created by ahmetu on 25.04.2016.
 */

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import tree.MorphemeGraph;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class SubstringMatcher {

    private Map<String, Double> stems = new HashMap<>();
    private Map<String, Double> affixes = new HashMap<>();
    private Map<String, Double> results = new HashMap<>();
    private Map<String, MorphemeGraph> graphList = new HashMap<>();

    private String fileSegmentationInput;

    private WordVectors vectors;

    public Map<String, Double> getStems() {
        return stems;
    }

    public Map<String, Double> getAffixes() {
        return affixes;
    }

    public Map<String, Double> getResults() {
        return results;
    }

    public Map<String, MorphemeGraph> getGraphList() {
        return graphList;
    }

    public SubstringMatcher(String fileVectorInput, String fileSegmentationInput) throws FileNotFoundException {
        vectors = WordVectorSerializer.loadTxtVectors(new File(fileVectorInput));
        this.fileSegmentationInput = fileSegmentationInput;
    }

    private void findMostFrequentLongestSubsequence(String word, double freq, int numberOfneighboors) throws FileNotFoundException, UnsupportedEncodingException {

        System.out.println("Control Word: " + word);

        Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
        boolean notFound = false;

        if (neighboors.isEmpty()) {
            notFound = true;
        }

        int max_f = 0;
        String stem = word;
        String affix = "NLL";

        // In order to limit the control length limit; i<(word.lenght()-limit+1) can be used.
        for (int i = 0; i < word.length() - 1; i++) {

            if (notFound) {
                break;
            }

            int f = 0;
            for (String n : neighboors) {
                if (n.startsWith(word.substring(0, word.length() - i))) {
                    f++;
                }
            }
            if (f > max_f) {
                max_f = f;
                stem = word.substring(0, word.length() - i);
                affix = word.substring(word.length() - i, word.length());
            }
        }

        MorphemeGraph graph = new MorphemeGraph(stem, vectors);
        if (!stem.equals(word)) {
            graph.add(word, freq);
            System.out.println("Stem: " + stem);
        }
/*
        // Stream kısmının içinde (effectively ?) final variable'lar kullanmak gerekiyormuş, stem hiç değişmediği için onu bir final variable'a attım.
        // aynı sebepten suffixfar ve suffixClose'u stream bloğunun içine aldım, yoksa hata veriyordu.
        final String final_stem = stem;

        neighboors.parallelStream().forEach((n) -> {
            String suffixFar = "";
            String suffixClose = "";
            if (n.startsWith(word)) {

                //graph.add(n, freq);
                recursiveAddLevelOne(n, final_stem, freq, numberOfneighboors, graph);

                suffixFar = n.substring(word.length());
                suffixClose = n.substring(final_stem.length(), word.length());

                if (affixes.containsKey(suffixClose)) {
                    affixes.put(suffixClose, affixes.get(suffixClose) + freq);
                } else {
                    affixes.put(suffixClose, freq);
                }

                if (affixes.containsKey(suffixFar)) {
                    affixes.put(suffixFar, affixes.get(suffixFar) + freq);
                } else {
                    affixes.put(suffixFar, freq);
                }
            } else if (n.startsWith(final_stem)) {

                //graph.add(n, freq);
                recursiveAddLevelOne(n, final_stem, freq, numberOfneighboors, graph);

                suffixClose = n.substring(final_stem.length());

                if (affixes.containsKey(suffixClose)) {
                    affixes.put(suffixClose, affixes.get(suffixClose) + freq);
                } else {
                    affixes.put(suffixClose, freq);
                }
            }
        });

        if (stems.containsKey(stem)) {
            stems.put(stem, stems.get(stem) + freq);
        } else {
            stems.put(stem, freq);
        }


        String result = stem + "+" + affix;
        if (results.containsKey(result)) {
            results.put(result, results.get(result) + freq);
        } else {
            results.put(result, freq);
        }

        System.out.println("-------------------------------------------------------------------");
        System.out.println("For word >>>> " + word + " <<<< from root node to all leaf nodes, all paths: ");

        graph.finish();

        graph.print(word);
        graphList.put(word, graph);
*/
    }

    private void recursiveAddLevelOne(String word, String stem, double freq, int numberOfneighboors, MorphemeGraph graph) {


        int max_f = 0;
        String affix = "NLL";

        if (graph.add(word, freq)) {

           // System.out.println("Child_1: " + word);
            Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);

            neighboors.parallelStream().forEach((n) -> {
                String suffixFar = "";
                String suffixClose = "";
                if (n.startsWith(word)) {

                    //graph.add(n, freq);
                    recursiveAdd(n, stem, freq, numberOfneighboors, graph);

                    suffixFar = n.substring(word.length());
                    suffixClose = n.substring(stem.length(), word.length());

                    if (affixes.containsKey(suffixClose)) {
                        affixes.put(suffixClose, affixes.get(suffixClose) + freq);
                    } else {
                        affixes.put(suffixClose, freq);
                    }

                    if (affixes.containsKey(suffixFar)) {
                        affixes.put(suffixFar, affixes.get(suffixFar) + freq);
                    } else {
                        affixes.put(suffixFar, freq);
                    }
                } else if (n.startsWith(stem)) {

                    //graph.add(n, freq);
                    recursiveAdd(n, stem, freq, numberOfneighboors, graph);

                    suffixClose = n.substring(stem.length());

                    if (affixes.containsKey(suffixClose)) {
                        affixes.put(suffixClose, affixes.get(suffixClose) + freq);
                    } else {
                        affixes.put(suffixClose, freq);
                    }
                }
            });
        }
    }


    private void recursiveAdd(String word, String stem, double freq, int numberOfneighboors, MorphemeGraph graph) {

        int max_f = 0;
        String affix = "NLL";

        if (graph.add(word, freq)) {

          //  System.out.println("Child_2: " + word);
            System.out.println("tn: " + Thread.getAllStackTraces().keySet().size());
            Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);

            String suffixFar = "";
            String suffixClose = "";
            for (String n : neighboors) {

                if (n.startsWith(word)) {

                    //graph.add(n, freq);
                    recursiveAdd(n, stem, freq, numberOfneighboors, graph);

                    suffixFar = n.substring(word.length());
                    suffixClose = n.substring(stem.length(), word.length());

                    if (affixes.containsKey(suffixClose)) {
                        affixes.put(suffixClose, affixes.get(suffixClose) + freq);
                    } else {
                        affixes.put(suffixClose, freq);
                    }

                    if (affixes.containsKey(suffixFar)) {
                        affixes.put(suffixFar, affixes.get(suffixFar) + freq);
                    } else {
                        affixes.put(suffixFar, freq);
                    }
                } else if (n.startsWith(stem)) {

                    //graph.add(n, freq);
                    recursiveAdd(n, stem, freq, numberOfneighboors, graph);

                    suffixClose = n.substring(stem.length());

                    if (affixes.containsKey(suffixClose)) {
                        affixes.put(suffixClose, affixes.get(suffixClose) + freq);
                    } else {
                        affixes.put(suffixClose, freq);
                    }
                }
            }
        }
    }

    private void findSegmentsAndAffixes() throws IOException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(fileSegmentationInput));

        String line;
        while ((line = reader.readLine()) != null) {
            String space = " ";
            StringTokenizer st = new StringTokenizer(line, space);

            double freq = Double.parseDouble(st.nextToken());
            String word = st.nextToken();
            boolean found = false;

            for (Map.Entry<String, MorphemeGraph> entry : graphList.entrySet())
            {
                if( entry.getValue() != null) {
                    if (entry.getValue().hasNode(word))
                        found = true;
                }
            }
            if(!found) {
                findMostFrequentLongestSubsequence(word, freq, 50);
            }
            else {
                found = false;
                System.out.println(word + " has been skipped");
            }
        }

    }

    public static void main(String[] args) throws IOException {
        SubstringMatcher ssm = new SubstringMatcher("outputs/vectors.txt", args[0]);
        ssm.findSegmentsAndAffixes();

        /*
        Map<String, Double> s = ssm.getStems();
        Map<String, Double> a = ssm.getAffixes();
        Map<String, Double> r = ssm.getResults();

        PrintWriter writer_seg = new PrintWriter("outputs/stems", "UTF-8");
        PrintWriter writer_af = new PrintWriter("outputs/affixes", "UTF-8");
        PrintWriter writer_res = new PrintWriter("outputs/results", "UTF-8");

        for (Map.Entry<String, Double> entry : s.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_seg.println(line);
        }
        writer_seg.close();

        for (Map.Entry<String, Double> entry : a.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_af.println(line);
        }
        writer_af.close();

        for (Map.Entry<String, Double> entry : r.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_res.println(line);
        }
        writer_seg.close();
        writer_af.close();
        writer_res.close();
        */
    }
}

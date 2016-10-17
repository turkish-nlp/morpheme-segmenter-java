package core.mcmc;

import core.blockSampling.ModelCopy;
import tries.TrieST;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Murathan on 10-Oct-16.
 */
public class testClass {
    private Map<String, Integer> frequencyTable = new ConcurrentHashMap<>();
    private ArrayList<Sample> segmentations = new ArrayList<>();
    double gamma = 0.037;
    double alpha = 0.1;
    int sizeOfTable = 0;
    TrieST trie;

    public testClass(String file, String TrieFile) throws IOException, ClassNotFoundException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(file));
        String line;

        trie = deSerialize(TrieFile);
        while ((line = reader.readLine()) != null) {
            segmentations.add(new Sample(line.replaceAll("\\+", ""), line, trie));
            String separator = "+";
            StringTokenizer st = new StringTokenizer(line, separator);
            while (st.hasMoreTokens()) {
                String word = st.nextToken();
                if (!frequencyTable.containsKey(word))
                    frequencyTable.put(word, 1);
                else
                    frequencyTable.put(word, frequencyTable.get(word) + 1);
            }
        }

        for (String str : frequencyTable.keySet()) {
            sizeOfTable = sizeOfTable + frequencyTable.get(str);
        }

    }

    public TrieST deSerialize(String file) throws IOException, ClassNotFoundException {

        FileInputStream fis = new FileInputStream(new File(file));
        ObjectInput in = null;
        Object o = null;
        in = new ObjectInputStream(fis);
        o = in.readObject();
        fis.close();
        in.close();

        TrieST model = (TrieST) o;
        return model;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        String files[] = {"sample_correct.txt", "sample_char_char.txt", "sample_unsegmented.txt"};
        Constant baseline = new Constant("sample_unsegmented", "C:\\Users\\Murathan\\github\\vectors.txt", "C:\\Users\\Murathan\\github\\wordlist_tur.txt", 0.1);

        for (String file : files) {

            System.out.println("--------------" + file+"--------------");

            testClass t = new testClass(file, "sample_unsegmented\\liseler");
            System.out.println("Morpheme - Freq Table");

        /*     for (String s : t.frequencyTable.keySet()) {

                System.out.println(s + " " + t.frequencyTable.get(s));
            }
            System.out.println("----");*/

            ArrayList<Double> scores = new ArrayList<Double>();
            double poisson = 0;
            double similarity = 0;
            double presence = 0;

            for (Sample s : t.segmentations) {
                System.out.println(s);

                //0:poisson, 1:similarity, 2:presence

                scores = s.calculateScores(s.getSegmentation());

                System.out.println("Poisson: " + scores.get(0));
                poisson = poisson + scores.get(0);

                System.out.println("Similarity: " + scores.get(1));
                similarity = similarity + scores.get(1);


                System.out.println("Presence: " + scores.get(2));
                presence = presence + scores.get(2);


                System.out.println();
            }
            double dp =  t.calculateLikelihoodsWithDP();
            System.out.println("Overall DP: " + dp);
            System.out.println("Overall Poisson: " + poisson);
            System.out.println("Overall Similarity: " + similarity);
            System.out.println("Overall Presence: " + presence);

            double overall = dp + poisson + similarity + presence;
            System.out.println("Overall Posterior Prob.: " + overall);


        }
    }

    private double calculateLikelihoodsWithDP() {

        double totalLikelihood = 0;
        int size = 0;
        for (Sample s : segmentations) { // for all segmentations
            StringTokenizer segments = new StringTokenizer(s.getSegmentation(), "+");
            while (segments.hasMoreTokens()) {
                String morpheme = segments.nextToken();
                if (frequencyTable.containsKey(morpheme)) {
                    if (frequencyTable.get(morpheme) > 0) {
                        totalLikelihood = totalLikelihood + Math.log10(frequencyTable.get(morpheme) / (size + alpha));
                        frequencyTable.put(morpheme, frequencyTable.get(morpheme) + 1);
                        size++;
                    } else {
                        totalLikelihood = totalLikelihood + Math.log10(alpha * Math.pow(gamma, morpheme.length() + 1) / (size + alpha));
                        frequencyTable.put(morpheme, 1);
                        size++;
                    }
                } else {
                    totalLikelihood = totalLikelihood + Math.log10(alpha * Math.pow(gamma, morpheme.length() + 1) / (size + alpha));
                    frequencyTable.put(morpheme, 1);
                    size++;
                }
            }

        }
        return totalLikelihood;
    }
}

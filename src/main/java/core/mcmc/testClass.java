package core.mcmc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Murathan on 10-Oct-16.
 */
public class testClass {
    private Map<String, Integer> frequencyTable = new ConcurrentHashMap<>();
    private ArrayList<Sample> segmentations = new ArrayList<>();
    double gamma = 0.037;
    double alpha = 0.1;
    int sizeOfTable = 0;

    public testClass(String file) throws IOException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            segmentations.add(new Sample(line.replaceAll("\\+", ""), line, null));
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

    public static void main(String[] args) throws IOException {

        String files[] = {"sample_correct.txt", "sample_char_char.txt", "sample_unsegmented.txt"};
        for (String file : files) {
            System.out.println(file);
            testClass t = new testClass(file);
            for (String s : t.frequencyTable.keySet())
                System.out.println(s + " " + t.frequencyTable.get(s));
            for (Sample s : t.segmentations) {
                System.out.println(s);
       /*     ArrayList<Double> scores = s.calculateScores(s.getSegmentation());
            for (double d : scores) {
                System.out.println(d);
            }*/
            }
            System.out.println(t.calculateLikelihoodsWithDP());
        }
    }

    private double calculateLikelihoodsWithDP() {

        double totalLikelihood = 0;
        int size = 0;
        for (Sample s : segmentations) {
            StringTokenizer segments = new StringTokenizer(s.getSegmentation());
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

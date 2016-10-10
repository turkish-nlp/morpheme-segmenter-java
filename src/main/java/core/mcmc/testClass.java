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
    }


    public static void main(String[] args) throws IOException {
        testClass t = new testClass("sample_correct.txt");
        for (String s : t.frequencyTable.keySet())
            System.out.println(s + " " + t.frequencyTable.get(s));
        for (Sample s : t.segmentations) {
            System.out.println(s);
            ArrayList<Double> scores = s.calculateScores(s.getSegmentation());
            for(double d: scores)
            {
                System.out.println(d);
            }
        }
    }
}

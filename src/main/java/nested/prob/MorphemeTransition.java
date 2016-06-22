package nested.prob;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ahmetu on 02.05.2016.
 */
public class MorphemeTransition {

    private ConcurrentHashMap<String, Double> stemCount;
    private ConcurrentHashMap<String, Double> morphemeCount;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> morphemeBiagramCount;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> morphemeBiagramProbabilities;
    private ConcurrentHashMap<String, Double> stemPropabilities;
    private ConcurrentHashMap<String, Double> results;

    String fileName;
    String startMorpheme = "STR";
    String endMorphmeme = "END";

    double totalMorphemeCount = 0;
    double totalStemCount = 0;

    public enum Smoothing {
        LAPLACE, INTERPOLATION, KNESERNEY
    }

    public MorphemeTransition(String inputFileName) {
        stemCount = new ConcurrentHashMap<>();
        morphemeCount = new ConcurrentHashMap<>();
        morphemeBiagramCount = new ConcurrentHashMap<>();
        morphemeBiagramProbabilities = new ConcurrentHashMap<>();
        stemPropabilities = new ConcurrentHashMap<>();
        fileName = inputFileName;
    }

    public MorphemeTransition(ConcurrentHashMap<String, Double> results) {
        morphemeCount = new ConcurrentHashMap<>();
        morphemeBiagramCount = new ConcurrentHashMap<>();
        morphemeBiagramProbabilities = new ConcurrentHashMap<>();
        stemPropabilities = new ConcurrentHashMap<>();
        this.results = results;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> getMorphemeBiagramProbabilities() {
        return morphemeBiagramProbabilities;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> getMorphemeBiagramCount() {
        return morphemeBiagramCount;
    }

    public ConcurrentHashMap<String, Double> getMorphemeCount() {
        return morphemeCount;
    }

    public ConcurrentHashMap<String, Double> getStemCount() {
        return stemCount;
    }

    public double getTotalMorphemeCount() {
        return totalMorphemeCount;
    }

    public double getTotalStemCount() {
        return totalStemCount;
    }

    public void setStemCount(ConcurrentHashMap<String, Double> stemCount) {
        this.stemCount = stemCount;
    }

    public void setMorphemeCount(ConcurrentHashMap<String, Double> morphemeCount) {
        this.morphemeCount = morphemeCount;
    }

    public void setMorphemeBiagramCount(ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> morphemeBiagramCount) {
        this.morphemeBiagramCount = morphemeBiagramCount;
    }

    public void setMorphemeBiagramProbabilities(ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> morphemeBiagramProbabilities) {
        this.morphemeBiagramProbabilities = morphemeBiagramProbabilities;
    }

    public void setResults(ConcurrentHashMap<String, Double> results) {
        this.results = results;
    }

    public void doItForFile() throws IOException {

        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(fileName));

        String line;
        while ((line = reader.readLine()) != null) {
            String space = " +";
            StringTokenizer st = new StringTokenizer(line, space);

            double freq = Double.parseDouble(st.nextToken());
            //String word = st.nextToken();

            countMorhemeForWordWithAllomorphs(st, freq);

        }
    }

    public void doItWithResults() {
        for (Map.Entry<String, Double> e : results.entrySet()) {
            countMorhemeForWord(new StringTokenizer(e.getKey(), "+"), e.getValue());
        }
    }

    private void countMorhemeForWord(StringTokenizer st, double frequency) {

        String stem = st.nextToken();
        if (stemCount.containsKey(stem)) {
            stemCount.put(stem, stemCount.get(stem) + frequency);
        } else {
            stemCount.put(stem, frequency);
        }

        String curr = startMorpheme;
        String next = null;
        while (st.hasMoreTokens()) {

            if (morphemeCount.containsKey(curr)) {
                morphemeCount.put(curr, morphemeCount.get(curr) + frequency);
            } else {
                morphemeCount.put(curr, frequency);
            }

            next = st.nextToken();

            ConcurrentHashMap<String, Double> transitions;
            if (morphemeBiagramCount.containsKey(curr)) {
                transitions = morphemeBiagramCount.get(curr);
                if (transitions.containsKey(next)) {
                    transitions.put(next, transitions.get(next) + frequency);
                } else {
                    transitions.put(next, frequency);
                }
            } else {
                transitions = new ConcurrentHashMap<>();
                transitions.put(next, frequency);
            }
            morphemeBiagramCount.put(curr, transitions);
            curr = next;
        }
        next = endMorphmeme;

        if (morphemeCount.containsKey(curr)) {
            morphemeCount.put(curr, morphemeCount.get(curr) + frequency);
        } else {
            morphemeCount.put(curr, frequency);
        }

        if (morphemeCount.containsKey(next)) {
            morphemeCount.put(next, morphemeCount.get(next) + frequency);
        } else {
            morphemeCount.put(next, frequency);
        }

        ConcurrentHashMap<String, Double> transitions;
        if (morphemeBiagramCount.containsKey(curr)) {
            transitions = morphemeBiagramCount.get(curr);
            if (transitions.containsKey(next)) {
                transitions.put(next, transitions.get(next) + frequency);
            } else {
                transitions.put(next, frequency);
            }
        } else {
            transitions = new ConcurrentHashMap<>();
            transitions.put(next, frequency);
        }
        morphemeBiagramCount.put(curr, transitions);
    }

    private void countMorhemeForWordWithAllomorphs(StringTokenizer st, double frequency) {

        String stem = st.nextToken();
        if (stemCount.containsKey(stem)) {
            stemCount.put(stem, stemCount.get(stem) + frequency);
        } else {
            stemCount.put(stem, frequency);
        }

        String curr = startMorpheme;
        String next = null;
        while (st.hasMoreTokens()) {

            if (morphemeCount.containsKey(curr)) {
                morphemeCount.put(curr, morphemeCount.get(curr) + frequency);
            } else {
                morphemeCount.put(curr, frequency);
            }

            next = st.nextToken();
            if (!next.equals("ken")) {
                next = next.replaceAll("a|e|ı|i", "H");
                next = next.replaceAll("t|d", "D");
                next = next.replaceAll("c|ç", "C");
                next = next.replaceAll("k|ğ", "G");
            }

            ConcurrentHashMap<String, Double> transitions;
            if (morphemeBiagramCount.containsKey(curr)) {
                transitions = morphemeBiagramCount.get(curr);
                if (transitions.containsKey(next)) {
                    transitions.put(next, transitions.get(next) + frequency);
                } else {
                    transitions.put(next, frequency);
                }
            } else {
                transitions = new ConcurrentHashMap<>();
                transitions.put(next, frequency);
            }
            morphemeBiagramCount.put(curr, transitions);
            curr = next;
        }
        next = endMorphmeme;

        if (morphemeCount.containsKey(curr)) {
            morphemeCount.put(curr, morphemeCount.get(curr) + frequency);
        } else {
            morphemeCount.put(curr, frequency);
        }

        if (morphemeCount.containsKey(next)) {
            morphemeCount.put(next, morphemeCount.get(next) + frequency);
        } else {
            morphemeCount.put(next, frequency);
        }

        ConcurrentHashMap<String, Double> transitions;
        if (morphemeBiagramCount.containsKey(curr)) {
            transitions = morphemeBiagramCount.get(curr);
            if (transitions.containsKey(next)) {
                transitions.put(next, transitions.get(next) + frequency);
            } else {
                transitions.put(next, frequency);
            }
        } else {
            transitions = new ConcurrentHashMap<>();
            transitions.put(next, frequency);
        }
        morphemeBiagramCount.put(curr, transitions);
    }

    private void calculateTotalMorphemeCount() {
        for (String s : morphemeCount.keySet()) {
            totalMorphemeCount = totalMorphemeCount + morphemeCount.get(s);
        }
    }

    private void calculateTotalStemCount() {
        for (String s : stemCount.keySet()) {
            totalStemCount = totalStemCount + stemCount.get(s);
        }
    }

    public void calculateStemProbabilities() {
        for (String stem : stemCount.keySet()) {
            stemPropabilities.put(stem, (stemCount.get(stem) / totalStemCount));
        }
    }

    public void calculateTransitionProbabilities(Smoothing smoothing) throws Exception {
        switch (smoothing) {
            case LAPLACE:
                System.out.println("---------------------------------------------------------------");
                System.out.println("Transition probabilities are calculating with laplace smoothing");
                calculateTransitionProbabilitiesWithLaplace();
                break;
            case INTERPOLATION:
                System.out.println("---------------------------------------------------------------");
                System.out.println("Transition probabilities are calculating with interpolation smoothing");
                break;
            case KNESERNEY:
                System.out.println("---------------------------------------------------------------");
                System.out.println("Transition probabilities are calculating with kneser-ney smoothing");
                break;
            default:
                System.out.println("---------------------------------------------------------------");
                System.out.println("Transition probabilities are calculating with laplace smoothing");
                break;
        }
    }

    private void calculateTransitionProbabilitiesWithLaplace() throws Exception {

        double additive = 1d;
        for (String firstMorpheme : morphemeCount.keySet()) {

            double noF_denominator = ((morphemeCount.get(firstMorpheme) + morphemeCount.size() * additive));
            double noF = additive / noF_denominator;

            ConcurrentHashMap<String, Double> transitions;
            ConcurrentHashMap<String, Double> transitionProbabilities;
            if (morphemeBiagramCount.containsKey(firstMorpheme)) {
                transitions = morphemeBiagramCount.get(firstMorpheme);
                transitionProbabilities = new ConcurrentHashMap<>();
                for (String secondMorpheme : morphemeCount.keySet()) {
                    if (transitions.containsKey(secondMorpheme)) {
                        transitionProbabilities.put(secondMorpheme, (transitions.get(secondMorpheme) + additive) / noF_denominator);
                    } else {
                        transitionProbabilities.put(secondMorpheme, noF);
                    }
                }
            } else {
                transitionProbabilities = new ConcurrentHashMap<>();
                for (String secondMorpheme : morphemeCount.keySet()) {
                    transitionProbabilities.put(secondMorpheme, noF);
                }
            }
            morphemeBiagramProbabilities.put(firstMorpheme, transitionProbabilities);
        }
    }

    public static void main(String[] args) throws Exception {
        MorphemeTransition mt = new MorphemeTransition("outputs/nested.pre.pre.test.txt");
        mt.doItForFile();
        mt.calculateTotalMorphemeCount();
        mt.calculateTransitionProbabilities(Smoothing.LAPLACE);

        ConcurrentHashMap<String, Double> stemCount = mt.getStemCount();
        ConcurrentHashMap<String, Double> morphemeCount = mt.getMorphemeCount();
        ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> morphemeBiagramCount = mt.getMorphemeBiagramCount();
        ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> morphemeBiagramProbabilities = mt.getMorphemeBiagramProbabilities();
        System.out.println("...................FINISH...................");
    }
}

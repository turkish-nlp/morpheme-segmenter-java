package prob;

import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by ahmetu on 02.05.2016.
 */
public class MorphemeTransition {

    /*private HashMap<String, Double> stemCount;*/
    private HashMap<String, Double> morphemeCount;
    private HashMap<String, HashMap<String, Double>> morphemeBiagramCount;
    private HashMap<String, HashMap<String, Double>> morphemeBiagramProbabilities;

    String fileName;
    String startMorpheme = "STR";
    String endMorphmeme = "END";

    public enum Smoothing {
        LAPLACE, INTERPOLATION, KNESERNEY
    }

    public MorphemeTransition(String inputFileName) {
        /*stemCount = new HashMap<String, Double>();*/
        morphemeCount = new HashMap<String, Double>();
        morphemeBiagramCount = new HashMap<String, HashMap<String, Double>>();
        morphemeBiagramProbabilities = new HashMap<String, HashMap<String, Double>>();
        fileName = inputFileName;
    }

    public HashMap<String, HashMap<String, Double>> getMorphemeBiagramProbabilities() {
        return morphemeBiagramProbabilities;
    }

    public HashMap<String, HashMap<String, Double>> getMorphemeBiagramCount() {
        return morphemeBiagramCount;
    }

    public HashMap<String, Double> getMorphemeCount() {
        return morphemeCount;
    }
/*
    public HashMap<String, Double> getStemCount() {
        return stemCount;
    }
*/
    public void doItForFile() throws IOException {

        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(fileName));

        String line;
        while ((line = reader.readLine()) != null) {
            String space = " ";
            StringTokenizer st = new StringTokenizer(line, space);

            double freq = Double.parseDouble(st.nextToken());
            String word = st.nextToken();

            countMorhemeForLine(word, freq);

        }
    }

    private void countMorhemeForLine(String word, double frequency) {

        String seperator = "+";
        StringTokenizer st = new StringTokenizer(word, seperator);

        String stem = st.nextToken();
/*        if (stemCount.containsKey(stem)) {
            stemCount.put(stem, stemCount.get(stem) + frequency);
        } else {
            stemCount.put(stem, frequency);
        }
*/
        String curr = startMorpheme;
        String next = null;
        while (st.hasMoreTokens()) {

            if (morphemeCount.containsKey(curr)) {
                morphemeCount.put(curr, morphemeCount.get(curr) + frequency);
            } else {
                morphemeCount.put(curr, frequency);
            }

            next = st.nextToken();

            HashMap<String, Double> transitions;
            if (morphemeBiagramCount.containsKey(curr)) {
                transitions = morphemeBiagramCount.get(curr);
                if (transitions.containsKey(next)) {
                    transitions.put(next, transitions.get(next) + frequency);
                } else {
                    transitions.put(next, frequency);
                }
            } else {
                transitions = new HashMap<String, Double>();
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

        HashMap<String, Double> transitions;
        if (morphemeBiagramCount.containsKey(curr)) {
            transitions = morphemeBiagramCount.get(curr);
            if (transitions.containsKey(next)) {
                transitions.put(next, transitions.get(next) + frequency);
            } else {
                transitions.put(next, frequency);
            }
        } else {
            transitions = new HashMap<String, Double>();
            transitions.put(next, frequency);
        }
        morphemeBiagramCount.put(curr, transitions);
    }

    public void calculateTransitionProbabilities(Smoothing smoothing) throws Exception {
        switch (smoothing) {
            case LAPLACE:
                System.out.println("Transition probabilities are calculating with laplace smoothing");
                calculateTransitionProbabilitiesWithLaplace();
                break;
            case INTERPOLATION:
                System.out.println("Transition probabilities are calculating with interpolation smoothing");
                break;
            case KNESERNEY:
                System.out.println("Transition probabilities are calculating with kneser-ney smoothing");
                break;
            default:
                System.out.println("Transition probabilities are calculating with laplace smoothing");
                break;
        }
    }

    private void calculateTransitionProbabilitiesWithLaplace() throws Exception {

        double additive = 0.1d;
        for (String firstMorpheme : morphemeCount.keySet()) {

            HashMap<String, Double> transitions;
            HashMap<String, Double> transitionProbabilities;
            if (morphemeBiagramCount.containsKey(firstMorpheme)) {
                transitions = morphemeBiagramCount.get(firstMorpheme);
                transitionProbabilities = new HashMap<String, Double>();

                for (String secondMorpheme : morphemeCount.keySet()) {
                    if (transitions.containsKey(secondMorpheme)) {
                        transitionProbabilities.put(secondMorpheme, transitions.get(secondMorpheme) / morphemeCount.get(firstMorpheme));
                    } else {
                        transitionProbabilities.put(secondMorpheme, additive / morphemeCount.get(firstMorpheme));
                    }
                    morphemeBiagramProbabilities.put(firstMorpheme, transitionProbabilities);
                }
            } else {
                throw new Exception("Transition from " + firstMorpheme +" was not found");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        MorphemeTransition mt = new MorphemeTransition("outputs/test.txt");
        mt.doItForFile();
        mt.calculateTransitionProbabilities(Smoothing.LAPLACE);

        /*HashMap<String, Double> stemCount = mt.getStemCount();*/
        HashMap<String, Double> morphemeCount = mt.getMorphemeCount();
        HashMap<String, HashMap<String, Double>> morphemeBiagramCount = mt.getMorphemeBiagramCount();
        HashMap<String, HashMap<String, Double>> morphemeBiagramProbabilities = mt.getMorphemeBiagramProbabilities();
        System.out.println("...................FINISH...................");
    }
}

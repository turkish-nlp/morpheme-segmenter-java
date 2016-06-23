package core;

import tries.TrieST;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Murathan on 20-Jun-16.
 */

public class Model {

    public List<String> searchedWordList;
    public List<TrieST> trieList;
    public Map<TrieST, ArrayList<String>> trieSegmentations;
    public Map<String, Integer> morphemeFreq;
    public Map<TrieST, Set<String>> wordBoundary;
    public Map<TrieST, Double> triePoisson;
    public double overallPoisson;
    Baseline fp;
    public double oldScore;

    public Model(String dir) throws IOException, ClassNotFoundException {

        fp = new Baseline(dir);
        searchedWordList = new ArrayList<String>(fp.searchedWordList);
        trieList = new ArrayList<TrieST>(fp.trieList);
        trieSegmentations = new ConcurrentHashMap<>(fp.trieSegmentations); // unique elements?? set??
        morphemeFreq = new ConcurrentHashMap<>(fp.morphemeFreq);
        wordBoundary = new ConcurrentHashMap<>(fp.baselineBoundaries);
        triePoisson = new ConcurrentHashMap<>(fp.triePoisson);
        oldScore = calculateOverallProbability(overallPoisson, morphemeFreq, trieSegmentations);
        overallPoisson = fp.overallPoisson;
        System.out.println("baseline likelihood: " + oldScore);
    }

    // private double acceptProb = 0.4;


    public static void main(String[] args) throws IOException, ClassNotFoundException {

        Model m = new Model(args[0]);
        m.random();

    }


    public void random() throws IOException, ClassNotFoundException {

        Random rand = new Random();

        int count = 10000;
        while (count > 0) {

            double candidatePoissonOverall = overallPoisson;
            // original objects //
            int trieRandom = (int) rand.nextInt(this.trieList.size());  // rand number for the trie
            // System.out.println("randomly chosen trie:" + fp.getSearchedWordList().get(trieRandom).toString());
            TrieST chosenTrie = this.trieList.get(trieRandom); // chosen trie ( to be less verbose )
            Set<String> originalBoundaryList = this.wordBoundary.get(chosenTrie); // original boundaryList of the chosen trie ( to be less verbose )
            // original objects //

            // copied objects //
            Set<String> candidateBoundaryList = new TreeSet<>(originalBoundaryList); // deep copy of the randomly chosen trie's boundaryList
            // copied objects //
            String candidateMorpheme = "$";

            while (candidateMorpheme.contains("$")) {
                int nodeRandom = (int) rand.nextInt(chosenTrie.size()); // rand number for the node in the random trie
                Object[] values = chosenTrie.getWordList().keySet().toArray(); // convert the wordList of the chosen trie into array to be able to select a random node
                candidateMorpheme = (String) values[nodeRandom];  // new boundary candidate
            }

            if (candidateBoundaryList.contains(candidateMorpheme)) {
                candidateBoundaryList.remove(candidateMorpheme);
                candidatePoissonOverall = overallPoisson - Math.log(fp.calculatePoisson(chosenTrie, candidateBoundaryList));
            } else {
                candidateBoundaryList.add(candidateMorpheme);
                candidatePoissonOverall = overallPoisson + Math.log(fp.calculatePoisson(chosenTrie, candidateBoundaryList));
            }
            System.out.println(candidateMorpheme);
            Map<String, Integer> candidateFrequencies = fp.changeFrequencyOneTrie(chosenTrie, originalBoundaryList, candidateBoundaryList, this.morphemeFreq);
            Map<TrieST, ArrayList<String>> candidateSegmentationList = fp.changeSegmentSequenceForOneTrie(chosenTrie, originalBoundaryList, candidateBoundaryList, this.trieSegmentations);

            double newScore = calculateOverallProbability(candidatePoissonOverall, candidateFrequencies, candidateSegmentationList);

           // double acceptProb = oldScore - newScore; // Is accepted value dynamic ????
           // System.out.println("prob: " + acceptProb);

            if (newScore > oldScore) {
          //      System.out.println("accepted mor: " + candidateMorpheme);
                update(chosenTrie, candidateBoundaryList, candidateFrequencies, candidateSegmentationList,candidatePoissonOverall);
                oldScore = newScore;
            } else // accept the boundary with randProb probability
            {
                int randProb = rand.nextInt(100);
                if ((double) randProb / 100 < 0.4  ) {
               //     System.out.println("accepted mor2: " + candidateMorpheme);
                    update(chosenTrie, candidateBoundaryList, candidateFrequencies, candidateSegmentationList,candidatePoissonOverall);
                    oldScore = newScore;
                }
            }
            count--;
        }
        System.out.println("-------------------------------------------------------------------------------------");

        System.out.println("Final Score: " + oldScore);

        for (TrieST st : this.trieList) {
            System.out.println(this.wordBoundary.get(st));
        }
        for (TrieST st : this.trieSegmentations.keySet()) {
        //    fp.determineSegmentsForOneTrie(st, this.wordBoundary.get(st), true);
        }
    }

    public void update(TrieST st, Set<String> candidateBoundaryList, Map<String, Integer> candidateFrequencies,Map<TrieST, ArrayList<String>> candidateSegmentationList, double candidatePoissonOverall  )
    {
        this.wordBoundary.put(st, candidateBoundaryList);
        this.morphemeFreq = candidateFrequencies;
        this.trieSegmentations = candidateSegmentationList;
        this.overallPoisson = candidatePoissonOverall;
    }


    public double calculateOverallProbability(double candidatePoissonOverall, Map<String, Integer> candidateFrequencies, Map<TrieST, ArrayList<String>> candidateSegmentationList) {
        return candidatePoissonOverall + calculateMaxLikelihoodForCorpus(candidateFrequencies, candidateSegmentationList);
    }

    public double calculateMaxLikelihoodForCorpus(Map<String, Integer> candidateFrequencies, Map<TrieST, ArrayList<String>> candidateSegmentationList) {

        /*
        * PARALEL !!!!!
        *
         */

        Map<TrieST, ArrayList<String>> candidateSegmentsSequence = new ConcurrentHashMap<>();
        for (TrieST st : candidateSegmentationList.keySet()) {
            for (String s : candidateSegmentationList.get(st)) {
                candidateSegmentsSequence.put(st, tokenSegmentation(s));
            }
        }
        Map<String, Double> morphemeProbabilities = calculateMorphemeProbabilities(candidateFrequencies);
        CopyOnWriteArrayList<Double> values = new CopyOnWriteArrayList<>();
        candidateSegmentsSequence.keySet().parallelStream().forEach((st) -> {
            values.add(calculateMLforOneTrie(candidateSegmentsSequence.get(st), morphemeProbabilities));
        });
        double result = 0;
        for (double d : values) {
            result = result + d;
        }
        return result;
    }

    public double calculateMLforOneTrie(ArrayList<String> segmentationList, Map<String, Double> morphemeProbabilities) {
        double prob = 0;
        for (String morph : segmentationList) {
            prob = prob + morphemeProbabilities.get(morph);
        }
        return prob;
    }

    public Map<String, Double> calculateMorphemeProbabilities(Map<String, Integer> candidateFrequencies) {
        Map<String, Double> morphemeProbabilities = new ConcurrentHashMap<>();

        int totalNumber = 0;
        for (String s : candidateFrequencies.keySet()) {
            totalNumber = totalNumber + candidateFrequencies.get(s);
        }
        for (String s : candidateFrequencies.keySet()) {
            double logLikelihood = Math.log((double) candidateFrequencies.get(s) / totalNumber);
            //  if(candidateFrequencies.get(s) == 0)
            //     System.out.println("ERROR:" + s + "   >>>  " + candidateFrequencies.get(s) );
            morphemeProbabilities.put(s, logLikelihood);
        }
        return morphemeProbabilities;
    }

    public ArrayList<String> tokenSegmentation(String segmentation) {
        ArrayList<String> segments = new ArrayList<String>();
        StringTokenizer tokens = new StringTokenizer(segmentation, "+");
        while (tokens.hasMoreTokens()) {
            segments.add(tokens.nextToken());
        }
        return segments;
    }

}

/*
        Random rand2 = new Random();
        int count = 1000;
        int chosen = 0;
        while (count > 0) {
            int randProb = rand2.nextInt(100);
            System.out.println( (double) randProb / 100);

        if ((double)randProb / 100 < 0.4) {
            System.out.println("        here");
            chosen++;
        }
            count--;
        }
        System.out.println(chosen);
        */


/*      TrieST randTrie = fp.getTrieList().get(1).cloneTrie();
        randTrie.put("xx");
        randTrie.put("xxx");
        System.out.println("original:" + fp.getTrieList().get(1).getWordList().get("çekiliş"));
        System.out.println("rand:" + randTrie.getWordList().get("çekiliş"));

        System.out.println("original:" + fp.getTrieList().get(1).getWordList().get("xxx"));
        System.out.println("rand:" + randTrie.getWordList().get("xxx"));
        */

// prints
            /*
            System.out.println("randomly chosen trie:" + fp.getSearchedWordList().get(trieRandom).toString() + "; indexes: " + trieRandom + " " + nodeRandom);
            System.out.println("candidate morpheme: " + candidateMorpheme);
            System.out.println("candidate boundary: " + tempWordBoundary.get(randTrie));
            System.out.println("candidate boundary2: " + tempWordBoundary.get(originalTrie));

            System.out.println("oldScore = " + oldScore + "newScore= " + newScore);
            System.out.println("after =" + originalBoundaryList);
            System.out.println("-------------------------");*/
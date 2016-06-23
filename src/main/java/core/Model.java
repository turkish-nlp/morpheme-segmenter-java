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
    Baseline fp = new Baseline();

    public Model(String dir) throws IOException, ClassNotFoundException {

        fp.generateTrieList(dir);
        fp.trieList.parallelStream().forEach((n) -> {
            fp.determineSegmentation(n);
        });

        searchedWordList = new ArrayList<String>(fp.searchedWordList);
        trieList = new ArrayList<TrieST>(fp.trieList);
        trieSegmentations = new ConcurrentHashMap<>(fp.trieSegmentations); // unique elements?? set??
        morphemeFreq = new ConcurrentHashMap<>(fp.morphemeFreq);
        wordBoundary = new ConcurrentHashMap<>(fp.baselineBoundaries);
        triePoisson = new ConcurrentHashMap<>(fp.triePoisson);
    }

    // private double acceptProb = 0.4;


    public static void main(String[] args) throws IOException, ClassNotFoundException {

        //new Model(args[0]).random();
    }

    /*
    public void random() throws IOException, ClassNotFoundException {

        Random rand = new Random();
        double poissonOverall = fp.calculatePoissonOverall();
        System.out.println("pp" + poissonOverall);
        double oldScore = calculateOverallProbability(poissonOverall, this.morphemeFreq, this.trieSegmentations);
        int printCount = 0;
        System.out.println("Score: " + oldScore);
        for (TrieST st : this.trieList)
        {
            System.out.println(this.baselineBoundaries.get(st));
        }
        ArrayList<String> init = new ArrayList<>();
        for (TrieST st : this.trieSegmentations.keySet()) {
            for (String str : this.trieSegmentations.get(st)) {
                System.out.println(printCount+ ": " + str);
                init.add(printCount+ ": " + str);
                printCount++;
            }
        }
        System.out.println("----------------------");
        System.out.println("----------------------");

        int count = 10;
        while (count > 0) {

            double candidatePoissonOverall = poissonOverall;
            Map<TrieST, Set<String>> tempWordBoundary = fp.getBaselineBoundaries();
            // original objects //
            int trieRandom = (int) rand.nextInt(fp.getTrieList().size());  // rand number for the trie
         //   System.out.println("randomly chosen trie:" + fp.getSearchedWordList().get(trieRandom).toString());
            TrieST originalTrie = fp.getTrieList().get(trieRandom); // chosen trie ( to be less verbose )
            Set<String> originalBoundaryList = fp.getBaselineBoundaries().get(originalTrie); // original boundaryList of the chosen trie ( to be less verbose )
            // original objects //

            // copied objects //
            TrieST randTrie = originalTrie.cloneTrie(); // deep copy of the randomly chosen trie
            Set<String> candidateBoundaryList = new TreeSet<>(fp.getBaselineBoundaries().get(originalTrie)); // deep copy of the randomly chosen trie's boundaryList
            // copied objects //
            String candidateMorpheme = "$";

            int nodeRandom = (int) rand.nextInt(originalTrie.size()); // rand number for the node in the random trie
            while (candidateMorpheme.contains("$")) {
                nodeRandom = (int) rand.nextInt(originalTrie.size()); // rand number for the node in the random trie
                Object[] values = randTrie.getWordList().keySet().toArray(); // convert the wordList of the chosen trie into array to be able to select a random node
                candidateMorpheme = (String) values[nodeRandom];  // new boundary candidate
            }

            if (candidateBoundaryList.contains(candidateMorpheme)) {
                candidateBoundaryList.remove(candidateMorpheme);
                candidatePoissonOverall = candidatePoissonOverall - Math.log(fp.poissonDistribution(originalTrie.getWordList().get(candidateMorpheme)));
            } else {
                candidateBoundaryList.add(candidateMorpheme);
                candidatePoissonOverall = candidatePoissonOverall + Math.log(fp.poissonDistribution(originalTrie.getWordList().get(candidateMorpheme)));
            }
           // System.out.println("candidate morpheme: " + candidateMorpheme);


            core.Baseline.Pair<Map<String, Integer>, Map<TrieST, ArrayList<String>>> candidatePair = fp.changePairForOneTrie(randTrie, originalBoundaryList, candidateBoundaryList);  // calculate the frequency changes between old and new boundary lists
            Map<String, Integer> candidateFrequencies = candidatePair.getFirst();
            Map<TrieST, ArrayList<String>> candidateSegmentationList = candidatePair.getSecond();
            double newScore = calculateOverallProbability(candidatePoissonOverall, candidateFrequencies, candidateSegmentationList);
            double acceptProb = (double) oldScore / newScore;
            // Is accepted value dynamic ????
           // System.out.println("before=" + originalBoundaryList);

            if (newScore > oldScore) {
                //update();
                this.baselineBoundaries.put(originalTrie, candidateBoundaryList);
                this.morphemeFreq = null;
                this.morphemeFreq = candidateFrequencies;
                this.trieSegmentations= null;
                this.trieSegmentations = candidateSegmentationList;
                oldScore = newScore;
                System.out.println("accepted morpheme: " + candidateMorpheme);
            } else // accept the boundary with randProb probability
            {
                int randProb = rand.nextInt(100);
                if ((double) randProb / 100 < 0.4) {
                    this.baselineBoundaries.put(originalTrie, candidateBoundaryList);
                    this.morphemeFreq = null;
                    this.morphemeFreq = candidateFrequencies;
                    this.trieSegmentations= null;
                    this.trieSegmentations = candidateSegmentationList;
                    oldScore = newScore;
                    System.out.println("accepted morpheme: " + candidateMorpheme);
                } else
                    originalBoundaryList.remove(candidateMorpheme);
            }
            count--;
            this.trieList.parallelStream().forEach((n) -> {
                fp.determineSegmentation(n);
            });
        }
        printCount = 0;
        System.out.println("Final Score: " + oldScore);

        for (TrieST st : this.trieList)
        {
            System.out.println(this.baselineBoundaries.get(st));
        }
        for (TrieST st : this.trieSegmentations.keySet()) {
            for (String str : this.trieSegmentations.get(st)) {
                System.out.println(printCount+ ": " + str);
                printCount++;
            }
        }
    }
    */

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
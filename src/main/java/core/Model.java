package core;

import org.apache.commons.io.FileUtils;
import tries.TrieST;

import java.io.*;
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
    public Map<TrieST, Double> trieSimiliarityScores;

    public Map<TrieST, Set<String>> wordBoundary;
    public Map<TrieST, Double> triePoisson;
    public double overallPoisson;
    public Map<TrieST, Double> boundarySimiliar;
    Baseline fp;
    public double oldScore;
    public double overallSS;
    public int noOfIteration;

    public Model(String dir, String vectorDir, String noOfIterationP, String lambda) throws IOException, ClassNotFoundException {

        fp = new Baseline(dir, vectorDir, Integer.parseInt(lambda));
        trieSimiliarityScores = new ConcurrentHashMap<>();
        searchedWordList = new ArrayList<String>(fp.searchedWordList);
        trieList = new ArrayList<TrieST>(fp.trieList);
        trieSegmentations = new ConcurrentHashMap<>(fp.trieSegmentations); // unique elements?? set??
        morphemeFreq = new ConcurrentHashMap<>(fp.morphemeFreq);
        wordBoundary = new ConcurrentHashMap<>(fp.baselineBoundaries);
        triePoisson = new ConcurrentHashMap<>(fp.triePoisson);
        overallPoisson = fp.overallPoisson;
        boundarySimiliar = new ConcurrentHashMap<>(fp.boundarySimiliarScores);
        overallSS = fp.overallSimilarityScore;
        oldScore = calculateOverallProbability(overallPoisson, morphemeFreq, trieSegmentations, overallSS);
        this.noOfIteration = Integer.parseInt(noOfIterationP);
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Model m = new Model(args[0], args[1], args[2], args[3]);
      m.random();

    }

    public void random() throws IOException, ClassNotFoundException {

        while (noOfIteration > 0) {
            Random rand = new Random();
            double candidatePoissonOverall = overallPoisson;
            // original objects //
            int trieRandom = (int) rand.nextInt(this.trieList.size());  // rand number for the trie
            // System.out.println("randomly chosen trie:" + fp.getSearchedWordList().get(trieRandom).toString());
            TrieST chosenTrie = this.trieList.get(trieRandom); // chosen trie ( to be less verbose )
            System.out.println("The trie " + searchedWordList.get(trieList.indexOf(chosenTrie)) + " is selected");
            if (chosenTrie.size() != 0) {
                Set<String> originalBoundaryList = this.wordBoundary.get(chosenTrie); // original boundaryList of the chosen trie ( to be less verbose )
                // original objects //

                // copied objects //
                Set<String> candidateBoundaryList = new TreeSet<>(originalBoundaryList); // deep copy of the randomly chosen trie's boundaryList
                // copied objects //
                String candidateMorpheme = "$";

                while (candidateMorpheme.contains("$")) {
                    int nodeRandom = rand.nextInt(chosenTrie.size()); // rand number for the node in the random trie
                    Object[] values = chosenTrie.getWordList().keySet().toArray(); // convert the wordList of the chosen trie into array to be able to select a random node
                    candidateMorpheme = (String) values[nodeRandom];  // new boundary candidate
                }

                if (originalBoundaryList.contains(candidateMorpheme)) {
                    System.out.println("the morpheme " + candidateMorpheme + " is unmarked");
                    candidateBoundaryList.remove(candidateMorpheme);
                    candidatePoissonOverall = overallPoisson - Math.log10(fp.poissonDistribution(chosenTrie.getWordList().get(candidateMorpheme)));
                    System.out.println("old poisson: " + overallPoisson + " candidate poisson: " + candidatePoissonOverall);
                } else {
                    candidateBoundaryList.add(candidateMorpheme);
                    candidatePoissonOverall = overallPoisson + Math.log10(fp.poissonDistribution(chosenTrie.getWordList().get(candidateMorpheme)));
                    System.out.println("when the morpheme " + candidateMorpheme + " is marked. It has " + chosenTrie.getWordList().get(candidateMorpheme) + " branches");
                    System.out.println("old poisson: " + overallPoisson + " candidate poisson: " + candidatePoissonOverall);
                }
                Map<String, Integer> candidateFrequencies = fp.changeFrequencyOneTrie(chosenTrie, originalBoundaryList, candidateBoundaryList, this.morphemeFreq);
                Map<TrieST, ArrayList<String>> candidateSegmentationList = fp.changeSegmentSequenceForOneTrie(chosenTrie, originalBoundaryList, candidateBoundaryList, this.trieSegmentations);

                double candidateTrieSim = fp.generateSimiliarWordsForOneTrie(chosenTrie, candidateBoundaryList);
                double candidateSS = overallSS - boundarySimiliar.get(chosenTrie) + candidateTrieSim;
                System.out.println("old (overall) similiarity score: " + overallSS + " candidate (overall) similiarity score: " + candidateSS);

                double newScore = calculateOverallProbability(candidatePoissonOverall, candidateFrequencies, candidateSegmentationList, candidateSS);
                System.out.println("old score: " + oldScore + " candidate score: " + newScore);
                if (newScore > oldScore) {
                    //      System.out.println("accepted mor: " + candidateMorpheme);
                    System.out.println("candidate score > oldscore accepted");

                    update(chosenTrie, candidateBoundaryList, candidateFrequencies, candidateSegmentationList, candidatePoissonOverall, candidateSS, candidateTrieSim);
                    oldScore = newScore;
                } else // accept the boundary with randProb probability
                {
                    double acceptProb = newScore - oldScore;
                    acceptProb = Math.pow(10, acceptProb);
                    double randProb = rand.nextDouble();
                    System.out.println("acceptProb: " + acceptProb);
                    System.out.println("randProb: " + randProb);

                    if ((double) randProb < acceptProb) {
                        System.out.println("candidate score < oldscore, yet it is accepted accepted");
                        update(chosenTrie, candidateBoundaryList, candidateFrequencies, candidateSegmentationList, candidatePoissonOverall, candidateSS, candidateTrieSim);
                        oldScore = newScore;
                    }
                }
                noOfIteration--;
                System.out.println("-----------------------------------------------------");
            }
        }

        for (TrieST st : this.wordBoundary.keySet()) {
            System.out.println("-------------------------------------------------------------------------------------");
            System.out.println(searchedWordList.get(trieList.indexOf(st)) + "--old-->" + fp.baselineBoundaries.get(st));
            System.out.println(searchedWordList.get(trieList.indexOf(st)) + "--new-->" + this.wordBoundary.get(st));
            System.out.println("-------------------------------------------------------------------------------------");

        }
        saveModel();
    }

    public void saveModel() throws IOException {
        HashMap<String, Integer> morphemeFreqCopy = new HashMap<>();
        morphemeFreqCopy.putAll(this.morphemeFreq);
        // toByteArray
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] yourBytes = null;
        out = new ObjectOutputStream(bos);
        out.writeObject(morphemeFreqCopy);
        yourBytes = bos.toByteArray();

        bos.close();
        out.close();

        FileUtils.writeByteArrayToFile(new File("model"), yourBytes);
    }

    public void update(TrieST st, Set<String> candidateBoundaryList, Map<String, Integer> candidateFrequencies, Map<TrieST, ArrayList<String>> candidateSegmentationList, double candidatePoissonOverall, double candidateOverallSimiliarityScore, double candidateTrieSimiliarityScore) {
        this.wordBoundary.put(st, candidateBoundaryList);
        this.morphemeFreq = candidateFrequencies;
        this.trieSegmentations = candidateSegmentationList;
        this.overallPoisson = candidatePoissonOverall;
        this.overallSS = candidateOverallSimiliarityScore;
        boundarySimiliar.put(st, candidateTrieSimiliarityScore);
    }


    public double calculateOverallProbability(double candidatePoissonOverall, Map<String, Integer> candidateFrequencies, Map<TrieST, ArrayList<String>> candidateSegmentationList, double similiarityScore) {
        return candidatePoissonOverall + calculateMaxLikelihoodForCorpus(candidateFrequencies, candidateSegmentationList) + similiarityScore;
    }

    public double calculateMaxLikelihoodForCorpus(Map<String, Integer> candidateFrequencies, Map<TrieST, ArrayList<String>> candidateSegmentationList) {

        ArrayList<Map<TrieST, ArrayList<String>>> listOftemplist = new ArrayList<>();

        Map<String, Double> morphemeProbabilities = calculateMorphemeProbabilities(candidateFrequencies);
        CopyOnWriteArrayList<Double> values = new CopyOnWriteArrayList<>();

        int i = 0;
        int paralelCount = 2000;
        Map<TrieST, ArrayList<String>> tempList = new ConcurrentHashMap<>();
        for (TrieST st : candidateSegmentationList.keySet()) {
            tempList.put(st, candidateSegmentationList.get(st));
            i++;
            if (i == paralelCount) {
                listOftemplist.add(tempList);
                tempList.clear();
                i = 0;
            }
        }
        if (!tempList.isEmpty()) {
            listOftemplist.add(tempList);
        }

        for (Map<TrieST, ArrayList<String>> temp : listOftemplist) {
            temp.keySet().parallelStream().forEach((st) -> {
                values.add(calculateMLforOneTrie(candidateSegmentationList.get(st), morphemeProbabilities));
            });
        }

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
            double logLikelihood = Math.log10((double) candidateFrequencies.get(s) / totalNumber);
            morphemeProbabilities.put(s, logLikelihood);
        }
        return morphemeProbabilities;
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
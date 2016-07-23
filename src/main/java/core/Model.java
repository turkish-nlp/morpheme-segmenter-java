package core;

import org.apache.commons.io.FileUtils;
import tries.TrieST;

import java.io.*;
import java.lang.reflect.Array;
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
    //  public double oldScore;
    public double overallSS;
    public int noOfIteration;
    public List<String> shuffleList;
    public double alpha = 0.1;
    public double gamma = 0.1;


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
        //  oldScore = calculateInitialProbabilityForDP(overallPoisson, overallSS);
        this.noOfIteration = Integer.parseInt(noOfIterationP);

        shuffleList = new ArrayList<String>();

        for (int i = 0; i < trieList.size(); i++) {
            for (int j = 0; j < trieList.get(i).getWordList().size(); j++) {
                String s = i + "-" + j;
                shuffleList.add(s);
            }
        }
        Collections.shuffle(shuffleList);
        System.out.println("model is built");
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Model m = new Model(args[0], args[1], args[2], args[3]);
        m.random();

    }

    public HashMap<String, Integer> getDifferenceSegmentationForDP(ArrayList<String> newTrieSegmentation, ArrayList<String> oldTrieSegmentation) {

        HashMap<String, Integer> diffMetricTMP = new HashMap<>();
        HashMap<String, Integer> diffMetric = new HashMap<>();

        HashSet<String> tokens = new HashSet<>();

        HashMap<String, Integer> oldTokenMetric = new HashMap<>();
        HashMap<String, Integer> newTokenMetric = new HashMap<>();

        for (String morp : oldTrieSegmentation) {
            tokens.add(morp);
            if (oldTokenMetric.containsKey(morp)) {
                oldTokenMetric.put(morp, oldTokenMetric.get(morp) + 1);
            } else {
                oldTokenMetric.put(morp, 1);
            }
        }
        for (String morp : newTrieSegmentation) {
            tokens.add(morp);
            if (newTokenMetric.containsKey(morp)) {
                newTokenMetric.put(morp, newTokenMetric.get(morp) + 1);
            } else {
                newTokenMetric.put(morp, 1);
            }
        }
        for (String morp : tokens) {
            if (oldTokenMetric.containsKey(morp) && newTokenMetric.containsKey(morp)) {
                diffMetricTMP.put(morp, (newTokenMetric.get(morp) - oldTokenMetric.get(morp)));
            } else if (oldTokenMetric.containsKey(morp) && !newTokenMetric.containsKey(morp)) {
                diffMetricTMP.put(morp, (-1 * oldTokenMetric.get(morp)));
            } else {
                diffMetricTMP.put(morp, (newTokenMetric.get(morp)));
            }
        }
        for (String str : diffMetricTMP.keySet())
            if (diffMetricTMP.get(str) != 0)
                diffMetric.put(str, diffMetricTMP.get(str));

        return diffMetric;
    }


    public void random() throws IOException, ClassNotFoundException {

        while (noOfIteration > 0) {

            for (String str : shuffleList) {

                double candidatePoissonOverall = overallPoisson;
                // original objects //
                int trieRandom = Integer.parseInt(str.substring(0, str.indexOf("-"))); // rand number for the trie
                // System.out.println("randomly chosen trie:" + fp.getSearchedWordList().get(trieRandom).toString());
                TrieST chosenTrie = this.trieList.get(trieRandom); // chosen trie ( to be less verbose )
                System.out.println("The trie " + searchedWordList.get(trieList.indexOf(chosenTrie)) + " is selected");
                if (chosenTrie.size() != 0) {
                    Set<String> originalBoundaryList = this.wordBoundary.get(chosenTrie); // original boundaryList of the chosen trie ( to be less verbose )
                    // original objects //

                    // copied objects //
                    Set<String> candidateBoundaryList = new TreeSet<>(originalBoundaryList); // deep copy of the randomly chosen trie's boundaryList
                    // copied objects //
                    int nodeRandom = Integer.parseInt(str.substring(str.indexOf("-") + 1)); // rand number for the node in the random trie
                    Object[] values = chosenTrie.getWordList().keySet().toArray(); // convert the wordList of the chosen trie into array to be able to select a random node
                    String candidateMorpheme = (String) values[nodeRandom];  // new boundary candidate

                    if (candidateMorpheme.contains("$")) {
                        continue;
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

                    // for DP
                    ArrayList<String> newTrieSegmentation = candidateSegmentationList.get(chosenTrie);
                    ArrayList<String> oldTrieSegmentation = trieSegmentations.get(chosenTrie);
                    HashMap<String, Integer> diffMap = getDifferenceSegmentationForDP(newTrieSegmentation, oldTrieSegmentation);

                    HashMap<String, Integer> baseFreqMap = getBaseFreqForDP(diffMap);

                    ArrayList<Double> dpScores = calculateProbForDP(diffMap, baseFreqMap);

                    //
                    double candidateTrieSim = fp.generateSimiliarWordsForOneTrie(chosenTrie, candidateBoundaryList);
                    double candidateSS = overallSS - boundarySimiliar.get(chosenTrie) + candidateTrieSim;
                    System.out.println("old (overall) similarity score: " + overallSS + " candidate (overall) similarity score: " + candidateSS);

                    // double newScore = calculateOverallProbability(candidatePoissonOverall, candidateFrequencies, candidateSegmentationList, candidateSS); // before DP
                    double newScore = dpScores.get(1) + candidatePoissonOverall + candidateSS;
                    double oldScore = dpScores.get(0) + candidatePoissonOverall + candidateSS;
                    System.out.println("old score: " + oldScore + " new score: " + newScore);
                    if (newScore > oldScore) {
                        //      System.out.println("accepted mor: " + candidateMorpheme);
                        System.out.println("new score > oldscore accepted");

                        update(chosenTrie, candidateBoundaryList, candidateFrequencies, candidateSegmentationList, candidatePoissonOverall, candidateSS, candidateTrieSim);
                        //oldScore = newScore; before DP
                    } else // accept the boundary with randProb probability
                    {
                        double acceptProb = newScore - oldScore;
                        acceptProb = Math.pow(10, acceptProb);
                        Random rand = new Random();
                        double randProb = rand.nextDouble();
                        System.out.println("acceptProb: " + acceptProb);
                        System.out.println("randProb: " + randProb);

                        if ((double) randProb < acceptProb) {
                            System.out.println("candidate score < oldscore, yet it is accepted accepted");
                            update(chosenTrie, candidateBoundaryList, candidateFrequencies, candidateSegmentationList, candidatePoissonOverall, candidateSS, candidateTrieSim);
                            oldScore = newScore;
                        }
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

    private ArrayList<Double> calculateProbForDP(HashMap<String, Integer> diffMap, HashMap<String, Integer> baseFreqMap) {

        double oldScore = 0;
        double newScore = 0;

        ArrayList<Double> scores = new ArrayList<>();

        ArrayList<String> toAddMap = new ArrayList<>();
        ArrayList<String> toRemoveMap = new ArrayList<>();
        for (String str : diffMap.keySet()) {
            if (diffMap.get(str) > 0) {
                toAddMap.add(str);
            } else
                toRemoveMap.add(str);
        }

        int originalSize = 0;
        for (String str : baseFreqMap.keySet()) {

            originalSize = originalSize + baseFreqMap.get(str);
        }
        int size = originalSize;
        for (String str : toAddMap) {
            if (baseFreqMap.containsKey(str)) {
                if (baseFreqMap.get(str) > 0) {
                    newScore = newScore + Math.log10(Math.pow((baseFreqMap.get(str) / (size + alpha)), diffMap.get(str)));
                    size = size + diffMap.get(str);
                } else {
                    newScore = newScore + Math.log10(alpha * Math.pow(gamma, str.length() + 1) / (size + alpha));
                    size = size + diffMap.get(str);
                }
            } else {
                newScore = newScore + Math.log10(alpha * Math.pow(gamma, str.length() + 1) / (size + alpha));
                size = size + diffMap.get(str);
            }
        }
        size = originalSize;
        for (String str : toRemoveMap) {
            if (baseFreqMap.containsKey(str)) {
                if (baseFreqMap.get(str) > 0) {
                    oldScore = oldScore + Math.log10(Math.pow((baseFreqMap.get(str) / (size + alpha)),(-1* diffMap.get(str) ) ));
                    size = size + (-1* diffMap.get(str) );
                } else {
                    oldScore = oldScore + Math.log10(alpha * Math.pow(gamma, str.length() + 1) / (size + alpha));
                    size = size + (-1* diffMap.get(str) );
                }
            } else {
                oldScore = oldScore + Math.log10(alpha * Math.pow(gamma, str.length() + 1) / (size + alpha));
                size = size + (-1* diffMap.get(str) );
            }
        }
        scores.add(oldScore);
        scores.add(newScore);
        return scores;

    }

    private HashMap<String, Integer> getBaseFreqForDP(HashMap<String, Integer> diffMap) {

        HashMap<String, Integer> baseMap = new HashMap<String, Integer>(this.morphemeFreq);
        for (String str : diffMap.keySet()) {
            if (diffMap.get(str) < 0)
                baseMap.put(str, this.morphemeFreq.get(str) + diffMap.get(str));
        }
        return baseMap;
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

    public double calculateInitialProbabilityForDP(double candidatePoissonOverall, double similiarityScore) {

        return candidatePoissonOverall + similiarityScore;
    }
/*
    public double calculateOverallProbability(double candidatePoissonOverall, Map<String, Integer> candidateFrequencies, Map<TrieST, ArrayList<String>> candidateSegmentationList, double similiarityScore) {
        double likelihoodScore = calculateMaxLikelihoodForCorpus(candidateFrequencies, candidateSegmentationList);
        System.out.println("likelihood: " + likelihoodScore);
        return candidatePoissonOverall + likelihoodScore + similiarityScore;
    }*/


/*
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
    }*/

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
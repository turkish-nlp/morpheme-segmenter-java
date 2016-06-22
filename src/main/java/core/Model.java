package core;

import org.canova.api.util.MathUtils;
import tries.TrieST;

import java.io.IOException;
import java.util.*;

/**
 * Created by Murathan on 20-Jun-16.
 */
public class Model {

    // private double acceptProb = 0.4;

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        Set<String> originalBoundaryList = new TreeSet<>();
        // originalBoundaryList.add("a");
        originalBoundaryList.remove("b");
        FrequencyProcessor fp = new FrequencyProcessor();
        fp.generateTrieList(args[0]);
        new Model().random(fp);
    }

    public double poissonDistribution(double lambda, int branchingFactor) {
        return (Math.pow(lambda, branchingFactor) * Math.exp(lambda)) / MathUtils.factorial(lambda);
    }

    public void random(FrequencyProcessor fp) throws IOException, ClassNotFoundException {

        Random rand = new Random();
        int count = 100;
        while (count > 0) {

            Map<TrieST, Set<String>> tempWordBoundary = fp.getWordBoundary();
            // original objects //
            int trieRandom = (int) rand.nextInt(fp.getTrieList().size());  // rand number for the trie
            System.out.println("randomly chosen trie:" + fp.getSearchedWordList().get(trieRandom).toString());
            TrieST originalTrie = fp.getTrieList().get(trieRandom); // chosen trie ( to be less verbose )
            Set<String> originalBoundaryList = fp.getWordBoundary().get(originalTrie); // original boundaryList of the chosen trie ( to be less verbose )
            // original objects //

            // copied objects //
            TrieST randTrie = originalTrie.cloneTrie(); // deep copy of the randomly chosen trie
            Set<String> candidateBoundaryList = new TreeSet<>(fp.getWordBoundary().get(originalTrie)); // deep copy of the randomly chosen trie's boundaryList
            // copied objects //
            String candidateMorpheme = "$";

            int nodeRandom = (int) rand.nextInt(originalTrie.size()); // rand number for the node in the random trie
            while (candidateMorpheme.contains("$")) {
                nodeRandom = (int) rand.nextInt(originalTrie.size()); // rand number for the node in the random trie
                Object[] values = randTrie.getWordList().keySet().toArray(); // convert the wordList of the chosen trie into array to be able to select a random node
                candidateMorpheme = (String) values[nodeRandom];  // new boundary candidate
            }

            candidateBoundaryList.add(candidateMorpheme); // mark the random node as the boundary ? should we check contains
            System.out.println("candidate morpheme: " + candidateMorpheme);

            core.FrequencyProcessor.Pair<Map<String, Integer>, Map<TrieST, ArrayList<String>>> candidatePair = fp.changePairForOneTrie(randTrie, originalBoundaryList, candidateBoundaryList);  // calculate the frequency changes between old and new boundary lists
            Map<String, Integer> candidateFrequencies = candidatePair.getFirst();
            Map<TrieST, ArrayList<String>> candidateSegmentationList = candidatePair.getSecond();

        /**/
            double oldScore = rand.nextInt(100);
            double newScore = calculateMaxLikelihoodForCorpus(candidateFrequencies, candidateSegmentationList);
            double acceptProb = (double) oldScore / newScore;
            // Is accepted value dynamic ????
            System.out.println("before=" + originalBoundaryList);
            if (newScore > oldScore)
                originalBoundaryList.add(candidateMorpheme);
            else // accept the boundary with randProb probability
            {
                int randProb = rand.nextInt(100);
                if ((double) randProb / 100 < acceptProb)
                    originalBoundaryList.add(candidateMorpheme);
                else
                    originalBoundaryList.remove(candidateMorpheme);
            }


            // prints
            System.out.println("randomly chosen trie:" + fp.getSearchedWordList().get(trieRandom).toString() + "; indexes: " + trieRandom + " " + nodeRandom);
            System.out.println("candidate morpheme: " + candidateMorpheme);
            System.out.println("candidate boundary: " + tempWordBoundary.get(randTrie));
            System.out.println("candidate boundary2: " + tempWordBoundary.get(originalTrie));

            System.out.println("oldScore = " + oldScore + "newScore= " + newScore);
            System.out.println("after =" + originalBoundaryList);
            count--;
            System.out.println("-------------------------");
        }


    }

    public double calculateMaxLikelihoodForCorpus(Map<String, Integer> candidateFrequencies, Map<TrieST, ArrayList<String>> candidateSegmentationList) {
        return new Random().nextInt(100);
    }

    public void calculateUnigramProbabilitiesForMorpheme(Map<String, Integer> candidateFrequencie) {
        
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
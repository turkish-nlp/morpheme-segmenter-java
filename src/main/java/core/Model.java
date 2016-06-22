package core;

import org.canova.api.util.MathUtils;

import java.io.IOException;

/**
 * Created by Murathan on 20-Jun-16.
 */
public class Model {

    FrequencyProcessor fp = new FrequencyProcessor();

    public double poissonDistribution(double lambda, int branchingFactor) {
        return (Math.pow(lambda, branchingFactor) * Math.exp(lambda)) / MathUtils.factorial(lambda);
        }

    public void random() throws IOException, ClassNotFoundException {

        int  trieRandom = (int) Math.random()*fp.getTrieList().size();
        int  nodeRandom = (int) Math.random()*fp.getTrieList().get(trieRandom).size();



        // create tempTrieBoundaryList;
        // tempTrieBoundaryList.add( trieList.get(trieRandom).get(nodeRandom));
        // changePairForOneTrie(TrieBoundaryList,tempTrieBoundaryList );


    }
    public double calculateMaxLikelihood()
    {
            return 0;
    }


}

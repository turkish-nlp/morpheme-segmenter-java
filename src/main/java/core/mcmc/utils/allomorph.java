package core.mcmc.utils;

import core.blockSampling.Segmenter;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Integer.parseInt;

/**
 * Created by Murathan on 26-Jun-16.
 */
public class allomorph {


    private Map<String, String> finalSegmentation;
    private Map<String, CopyOnWriteArrayList<String>> serializedSegmentations;
    private ArrayList<String> stemList = new ArrayList<>();
    private ArrayList<String> affixList = new ArrayList<>();
    private static WordVectors vectors;

    public allomorph(String vectorDir, String file, String inputFile, String mode, int thresholdArg) throws IOException, ClassNotFoundException {
        this.vectors = WordVectorSerializer.loadTxtVectors(new File(vectorDir));
        SegmentationGenerator generator = new SegmentationGenerator(vectorDir, file, inputFile, mode, thresholdArg);
        finalSegmentation = generator.getFinalSegmentation();
        serializedSegmentations = generator.getSerializedSegmentations();
        generateStemAffixLists();
//        for (String str : stemList)
//            System.out.println(str);
//        for (String str : affixList)
//            System.out.println(str);
        System.out.println();
        System.out.println();

        findAllomorphes();
    }

    private void findAllomorphes() {

        INDArray stemVector = vectors.getWordVectorMatrix("lise");
        List<String> nearest = (List<String>) vectors.wordsNearest(stemVector, 5);
        for(String str: nearest)
            System.out.println(str);

//        for (String stem : stemList) {
//            for (String affix : affixList) {
//                INDArray stemVector = vectors.getWordVectorMatrix(stem);
//                INDArray stemAffixVector = vectors.getWordVectorMatrix(stem+affix);
//                List<String> nearest = (List<String>) vectors.wordsNearest(stemVector,10);
//                for(String str: nearest)
//                    System.out.println(str);
//
//            }
//        }
    }

    public void generateStemAffixLists() {
        for (String str : serializedSegmentations.keySet()) {
            if (serializedSegmentations.get(str) != null) {
                for (String seg : serializedSegmentations.get(str)) {
                    String morphemes[] = seg.split("\\+");
                    String stem = "";
                    for (int i = 0; i < morphemes.length - 1; i++)
                        stem = stem + morphemes[i];
                    stemList.add(stem);
                    affixList.add(morphemes[morphemes.length - 1]);
                }
            }
        }

    }


    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        allomorph a = new allomorph(args[0], args[1], args[2], args[3], 25);

    }


}

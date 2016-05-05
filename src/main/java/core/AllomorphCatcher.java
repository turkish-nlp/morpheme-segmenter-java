package core;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created by ahmetu on 27.04.2016.
 */
public class AllomorphCatcher {

    private String fileSegmentationInput;

    private WordVectors vectors;

    public AllomorphCatcher(String fileVectorInput, String fileSegmentationInput) {
        try {
                vectors = WordVectorSerializer.loadTxtVectors (new File(fileVectorInput)) ;
                this.fileSegmentationInput = fileSegmentationInput;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void TestAnalogy(){
        String a = "kedi";
        String b = "kediler";
        String c = "köpek";
        String d = "köpekler";

        Collection<String> neighboors_a = vectors.wordsNearest(Arrays.asList(b, c), Arrays.asList(a), 10);
        System.out.println("***************************");
        for (String s : neighboors_a) {
            System.out.println(s);
        }

        Collection<String> neighboors_b = vectors.wordsNearest(Arrays.asList(a, d), Arrays.asList(b), 10);
        System.out.println("***************************");
        for (String s : neighboors_b) {
            System.out.println(s);
        }
    }

    public static void main(String[] args) {

        AllomorphCatcher ac = new AllomorphCatcher("tvec2.bin", "metusabanci_kelimeler_lowercase.txt");
        ac.TestAnalogy();
    }

}

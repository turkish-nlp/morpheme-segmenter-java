package core;

/**
 * Created by ahmetu on 25.04.2016.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

public class SubstringMatcher {

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        WordVectors vectors = WordVectorSerializer.loadTxtVectors(new File("datas\\turkce.txt"));

        try {
            String a = "test";
            String b = "testleri";
            System.out.println(vectors.similarity(a, b));
        } catch (Exception e) {
            System.out.println(-.5);
        }

        try {
            String a = new String("titreşimle".getBytes("utf-8"));
            String b = new String("titreşim".getBytes("utf-8"));
            System.out.println(vectors.similarity(a, b));
        } catch (Exception e) {
            System.out.println(-.5);
        }

        try {
            String a = new String("erkek".getBytes("utf-8"));
            String b = new String("kadın".getBytes("utf-8"));
            System.out.println(vectors.similarity(a, b));
        } catch (Exception e) {
            System.out.println(-.5);
        }

        try {
            String a = new String("erkek".getBytes("utf-8"));
            String b = new String("zırvalarında".getBytes("utf-8"));
            System.out.println(vectors.similarity(a, b));
        } catch (Exception e) {
            System.out.println(-.5);
        }

        try {
            String a = new String("titreşimle".getBytes());
            String b = new String("erkek".getBytes("utf-8"));
            Collection<String> nearestList = vectors.wordsNearest(b, 10);
            System.out.println("\nNEAREST:");
            for (String s : nearestList){
                System.out.println(new String(s.getBytes("cp1254"), "utf-8"));
            }
        } catch (Exception e) {
            System.out.println(-.5);
        }

        try {
            String a = new String("giderek".getBytes());
            String b = new String("erkek".getBytes("utf-8"));
            Collection<String> nearestL = vectors.wordsNearest(a, 10);
            System.out.println("\nNEAREST:");
            for (String s : nearestL){
                System.out.println(new String(s.getBytes("cp1254"), "utf-8"));
            }
        } catch (Exception e) {
            System.out.println(-.5);
        }

        String control = new String("gelmişti".getBytes("utf-8"));
        System.out.println("************");
        System.out.println(findMostFrequentLongestSubsequence(vectors, control, 10));

    }

    public static String findMostFrequentLongestSubsequence(WordVectors vectors, String word, int numberOfneighboors){
        try {

            Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
            int max_f = 0;
            String l_subs = "";
            for (int i=0; i<word.length(); i++){
                int f = 0;
                for (String n : neighboors){
                    if (n.startsWith(word.substring(0, word.length()-i))){
                        f++;
                    }
                }
                if (f > max_f){
                    max_f = f;
                    l_subs = word.substring(0, word.length()-i);
                }
            }
            return l_subs;
        } catch (Exception ex){
            System.out.println("PROBLEM");
            return null;
        }
    }
}

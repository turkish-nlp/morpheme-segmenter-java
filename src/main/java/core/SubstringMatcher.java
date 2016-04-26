package core;

/**
 * Created by ahmetu on 25.04.2016.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.jboss.logging.Logger;

public class SubstringMatcher {

    Map<String, Integer> segments = new TreeMap<String, Integer>();
    Map<String, Integer> affixes = new TreeMap<String, Integer>();

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
            for (String s : nearestList) {
                System.out.println(s);
            }
        } catch (Exception e) {
            System.out.println(-.5);
        }

        try {
            String a = new String("giderek".getBytes());
            String b = new String("erkek".getBytes("utf-8"));
            Collection<String> nearestL = vectors.wordsNearest(a, 10);
            System.out.println("\nNEAREST:");
            for (String s : nearestL) {
                System.out.println(new String(s.getBytes("cp1254"), "utf-8"));
            }
        } catch (Exception e) {
            System.out.println(-.5);
        }

        String control = new String("gelmişti".getBytes("utf-8"));
        System.out.println("************");
        //System.out.println(findMostFrequentLongestSubsequence(vectors, control, 10));

    }

    public void findMostFrequentLongestSubsequence(WordVectors vectors, String word, int numberOfneighboors) {
        try {
            Collection<String> neighboors = vectors.wordsNearest(word, numberOfneighboors);
            int max_f = 0;
            String segment = "";
            String affix = "";
            // In order to limit the control lenght limit; i<word.lenght()-limit can be used.
            for (int i = 0; i < word.length(); i++) {
                int f = 0;
                for (String n : neighboors) {
                    if (n.startsWith(word.substring(0, word.length() - i))) {
                        f++;
                    }
                }
                if (f > max_f) {
                    max_f = f;
                    segment = word.substring(0, word.length() - i);
                    affix = word.substring(word.length() - i, word.length());
                }
            }

            if (segments.containsKey(segment)) {
                segments.put(segment, segments.get(segment) + 1);
            }

            if (affixes.containsKey(affix)) {
                affixes.put(affix, affixes.get(affix) + 1);
            }

        } catch (Exception ex) {
            Logger.getLogger(SubstringMatcher.class).log(Logger.Level.ERROR, ex.getLocalizedMessage());
        }
    }

    public void findSegmentsAndAffixes() {

    }
}

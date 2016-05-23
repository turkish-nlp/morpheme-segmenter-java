package prob;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created by ahmetu on 02.05.2016.
 */
public class Utilities {

    public static List<String> getPossibleSegmentations(String word, Set<String> stems, Set<String> affixes/*, WordVectors vectors*/) throws FileNotFoundException {

        ArrayList<String> segmentations = new ArrayList<String>();

        Stack<String> morphemeStack = new Stack<>();

        for (int i = 2; i < word.length() + 1; i++) {
            String stem = word.substring(0, i);
            String remaning = word.substring(i);

            if (stems.contains(stem)) {
                getPossibleAffixSequence(affixes, stem, remaning, segmentations);
            }
        }

        //List<String> news = checkConstraint(segmentations, vectors);

        //if (news.isEmpty()){
        return segmentations;
        //} else {
        //return news;
        //}
    }

    private static void getPossibleAffixSequence(Set<String> affixes, String head, String tail, List<String> segmentations) {
        /*
        if (tail.length() == 0) {
            segmentations.add(head);
        } else
        */
        if (tail.length() == 1) {
            if (affixes.contains(tail)) {
                segmentations.add(head + "+" + tail);
            }
        } else {

            //////////////
            for (int i = 1; i < tail.length() + 1; i++) {
                String morpheme = tail.substring(0, i);

                if (morpheme.length() == tail.length()) {
                    if (affixes.contains(morpheme)) {
                        segmentations.add(head + "+" + morpheme);
                    }
                } else {
                    String tailMorph = tail.substring(i);
                    if (affixes.contains(morpheme)) {
                        String headMorph = head + "+" + morpheme;
                        getPossibleAffixSequence(affixes, headMorph, tailMorph, segmentations);
                    }
                }
            }
            ////////////////
        }
    }

    private static List<String> checkConstraint(List<String> segmentations, WordVectors vectors) throws FileNotFoundException {

        double threshold = 0.25;

        List<String> satifiedSegmentations = new ArrayList<>();

        for (String s : segmentations) {
            boolean satify = true;

            StringTokenizer st = new StringTokenizer(s, "+");
            String c_word = st.nextToken();
            String n_word = "";
            while (st.hasMoreTokens() && satify) {
                n_word = c_word + st.nextToken();
                if (threshold > vectors.similarity(c_word, n_word)) {
                    satify = false;
                }
                c_word = n_word;
            }
            if (satify) {
                satifiedSegmentations.add(s);
            }
        }

        return satifiedSegmentations;
    }

    public static void writeFileBigramProbabilities(Map<String, Map<String, Double>> morphemeBiagramProbabilities) throws FileNotFoundException, UnsupportedEncodingException {

        PrintWriter writer = new PrintWriter("outputs/collection", "UTF-8");

        for (String first : morphemeBiagramProbabilities.keySet()) {
            for (String second : morphemeBiagramProbabilities.get(first).keySet()) {
                String line = first + "->" + second + ":" + morphemeBiagramProbabilities.get(first).get(second);
                writer.println(line);
            }
        }

        writer.close();
    }

    public static void writeDBFromFile(String fileName) throws IOException {

        MongoClient mongo = new MongoClient("localhost", 27017);
        MongoDatabase db = mongo.getDatabase("nlp-db");
        //DB db = mongo.getDB("nlp-db");
        MongoCollection<BasicDBObject> bigrams = db.getCollection("allomorphs", BasicDBObject.class);
        //DBCollection collection = db.getCollection("collection");

        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(fileName));

        String line;
        while ((line = reader.readLine()) != null) {
            String seperator = ":";
            StringTokenizer st = new StringTokenizer(line, seperator);

            String word = st.nextToken();
            double prob = Double.parseDouble(st.nextToken());

            BasicDBObject object = new BasicDBObject("pair", word);
            object.append("probability", prob);
            bigrams.insertOne(object);
        }
    }

    public static double getProbabilityForBigram(DBCollection bigrams, String firstWord, String secondWord) {

        String pair = firstWord + "->" + secondWord;

        BasicDBObject object = new BasicDBObject("pair", pair);
        return (double) (double) bigrams.findOne(object).get("probability");
    }

    public static double getPrior(String morpheme) {
        double coefficient = 1.0 / 29.0;
        int power = morpheme.length() + 1;

        return Math.pow(coefficient, power);
    }

    public static void constructStemAndAffixMaps(String inputFileName, Map<String, Double> stems, Map<String, Double> affixes) throws IOException {

        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(inputFileName));

        String line;
        while ((line = reader.readLine()) != null) {
            String space = " +";
            StringTokenizer st = new StringTokenizer(line, space);

            double freq = Double.parseDouble(st.nextToken());
            String stem = st.nextToken();

            if (stems.containsKey(stem)) {
                stems.put(stem, stems.get(stem) + freq);
            } else {
                stems.put(stem, freq);
            }

            String curr = "STR";
            String next = null;
            while (st.hasMoreTokens()) {

                if (affixes.containsKey(curr)) {
                    affixes.put(curr, affixes.get(curr) + freq);
                } else {
                    affixes.put(curr, freq);
                }

                next = st.nextToken();
                curr = next;
            }
            next = "END";

            if (affixes.containsKey(curr)) {
                affixes.put(curr, affixes.get(curr) + freq);
            } else {
                affixes.put(curr, freq);
            }

            if (affixes.containsKey(next)) {
                affixes.put(next, affixes.get(next) + freq);
            } else {
                affixes.put(next, freq);
            }

        }
    }

    public static void multiThreadWriteToDB(String fileName, int threadNumber) throws InterruptedException, IOException {

        Logger root = (Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);

        root.setLevel(Level.ERROR);

        MongoClient mongo = new MongoClient("localhost", 27017);
        MongoDatabase db = mongo.getDatabase("nlp-db");
        //DB db = mongo.getDB("nlp-db");
        MongoCollection<BasicDBObject> allomorphs = db.getCollection("bigram_allomorphs", BasicDBObject.class);
        //DBCollection collection = db.getCollection("collection");

        final BufferedReader reader = new BufferedReader(new FileReader(fileName), 1024 * 1024);

        Thread[] threads = new Thread[threadNumber];
        for (int i = 0; i < threadNumber; i++) {
            threads[i] = new Thread(new Runnable() {

                @Override
                public void run() {
                    //method testing

                    System.out.println("Thread starting: " + Thread.currentThread().getName());

                    String line = null;
                    do {
                        try {
                            synchronized (reader) {
                                line = reader.readLine();
                            }
                            if (line != null) {
                                String seperator = ":";
                                StringTokenizer st = new StringTokenizer(line, seperator);

                                String word = st.nextToken();
                                double prob = Double.parseDouble(st.nextToken());

                                BasicDBObject object = new BasicDBObject("pair", word);
                                object.append("probability", prob);
                                allomorphs.insertOne(object);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } while (line != null);
                }
            });
        }

        for (int i = 0; i < threadNumber; i++) {
            threads[i].start();
        }

        for (int i = 0; i < threadNumber; i++) {
            threads[i].join();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        WordVectors vectors = WordVectorSerializer.loadTxtVectors(new File(args[0]));

        Map<String, Double> stems = new HashMap();
        stems.put("korunm", 5d);
        //stems.put("sep", 1d);

        Map<String, Double> affixes = new HashMap();
        affixes.put("ıdırlar", 2d);
        affixes.put("al", 1d);
        //affixes.put("i", 1d);
        affixes.put("in", 1d);
        affixes.put("r", 1d);
        //affixes.put("n", 1d);
        affixes.put("et", 1d);
        affixes.put("isss", 1d);

        Set<String> s = stems.keySet();
        Set<String> a = affixes.keySet();

        long start = System.nanoTime();
        List<String> results = getPossibleSegmentations("korunmalıdırlar", s, a/*, vectors*/);
        long stop = System.nanoTime();

        System.out.println(stop - start);
        System.out.println("***************");
        for (String r : results) {
            System.out.println(r);
        }


        //multiThreadWriteToDB(args[0], 16);

        /*
        MongoClient mongo = new MongoClient("localhost", 27017);
        MongoDatabase db = mongo.getDatabase("nlp-db");
        MongoCollection<BasicDBObject> collection = db.getCollection("collection", BasicDBObject.class);
        BasicDBObject object = new BasicDBObject("pair", "alşskdjaşsdjsşaldjkasşdlka");

        System.out.println(collection.find(object).iterator().next().get("probability"));
        */

        /*
        MongoClient mongo = new MongoClient("localhost", 27017);
        DB db = mongo.getDB("nlp-db");
        DBCollection collection = db.getCollection("allomorphs");

        String pair = "ler" + "->" + "in";

        BasicDBObject object = new BasicDBObject("pair", pair);
        System.out.println((double) collection.findOne(object).get("probability"));

        for (int i = 0; i < 100; i++) {
            System.out.println((double) collection.findOne(object).get("probability"));
        }
        */
    }

}

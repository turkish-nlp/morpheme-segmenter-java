package prob;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.collections.FastHashMap;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ahmetu on 02.05.2016.
 */
public class Utilities {

    public static List<String> getPossibleSegmentations(String word, Set<String> stems, Set<String> affixes) {

        ArrayList<String> segmentations = new ArrayList<String>();

        Stack<String> morphemeStack = new Stack<>();

        for (int i = 2; i < word.length() + 1; i++) {
            String stem = word.substring(0, i);
            String remaning = word.substring(i);

            if (stems.contains(stem)) {
                getPossibleAffixSequence(affixes, stem, remaning, segmentations);
            }
        }

        return segmentations;
    }

    private static void getPossibleAffixSequence(Set<String> affixes, String head, String tail, List<String> segmentations) {

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

        /*
        Map<String, Double> stems = new FastHashMap();
        stems.put("gel", 5d);
        stems.put("geliyor", 3d);
        stems.put("geliyormuş", 6d);

        Map<String, Double> affixes = new FastHashMap();
        affixes.put("iyor", 3d);
        affixes.put("muş", 2d);
        affixes.put("sun", 1d);
        affixes.put("muşsun", 1d);
        affixes.put("iyormuş", 1d);
        affixes.put("su", 1d);
        affixes.put("n", 1d);

        Set<String> s = stems.keySet();
        Set<String> a = affixes.keySet();

        long start = System.nanoTime();
        List<String> results = getPossibleSegmentations("geliyormuşsun", s, a);
        long stop = System.nanoTime();

        System.out.println(stop - start);
        System.out.println("***************");
        for (String r : results) {
            System.out.println(r);
        }
        */

        multiThreadWriteToDB(args[0], 16);

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

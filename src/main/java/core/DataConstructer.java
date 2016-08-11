package core;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.jboss.netty.util.internal.ConcurrentHashMap;

import java.io.*;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by ahmetu on 11.08.2016.
 */
public class DataConstructer {
    private WordVectors vectors;
    private String inputFile;
    private String outputFile;
    private ConcurrentHashMap<String, Collection<String>> closestNN;
    int noOfNN;

    public DataConstructer(String fileVectorInput, String inputFile, String outputFile, int noOfNN) throws FileNotFoundException {
        vectors = WordVectorSerializer.loadTxtVectors(new File(fileVectorInput));
        this.inputFile = inputFile;
        this.noOfNN = noOfNN;
        this.outputFile = outputFile;
        closestNN = new ConcurrentHashMap<>();
    }

    public void gatherData() {
        closestNN.keySet().parallelStream().forEach((key) -> {
            Collection<String> neighboors = vectors.wordsNearest(key, noOfNN);
            if (!neighboors.isEmpty()) {
                closestNN.put(key, neighboors);
            }
        });
    }

    public void printData() throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(outputFile, "UTF-8");

        for (String s : closestNN.keySet()) {
            writer.println(s);
            for (String n : closestNN.get(s)) {
                writer.println(n);
            }
        }
        writer.close();
    }

    private void readFile() throws IOException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(inputFile));

        String line;
        while ((line = reader.readLine()) != null) {
            String space = " ";
            StringTokenizer st = new StringTokenizer(line, space);

            double freq = Double.parseDouble(st.nextToken());
            String word = st.nextToken();

            closestNN.put(word, null);
        }
    }
}

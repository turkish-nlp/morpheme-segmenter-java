package core.preModel;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.jboss.netty.util.internal.ConcurrentHashMap;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

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
        Charset charset = Charset.forName("UTF-8");
        List<String> words = Files.readAllLines(new File(inputFile).toPath(), charset);

        String line;
        for(String str : words) {
            String space = " ";
            StringTokenizer st = new StringTokenizer(str, space);

            String freq = st.nextToken();
            String word = st.nextToken();
            System.out.println(word);
            closestNN.put(word, new ArrayList<>());
        }
    }

    public static void main(String[] args) throws IOException {

        /*
        * Vector dosyasını main methoda argüman olarak verilecek şekilde değiştiriyorum.
         */
        DataConstructer dc = new DataConstructer(args[0],args[1],args[2],Integer.parseInt(args[3]));
        dc.readFile();
        dc.gatherData();
        dc.printData();
    }
}

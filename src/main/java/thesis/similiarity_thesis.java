package thesis;

import org.apache.commons.io.FileUtils;
import org.canova.api.util.MathUtils;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import tries.TrieST;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Murathan on 25-Jul-16.
 */
public class similiarity_thesis {

    static Charset charset = Charset.forName("UTF-8");

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        WordVectors vectors = WordVectorSerializer.loadTxtVectors(new File(args[0]));

        List<String> file = Files.readAllLines(new File(args[1]).toPath(), charset);


        TreeMap<String, Double> pairScore = new TreeMap<>();
        for (String str : file) {
            StringTokenizer token = new StringTokenizer(str, "--");
            String w1 = token.nextToken();
            String w2 = token.nextToken();
            if (!w1.equalsIgnoreCase(w2)) {
                double score = vectors.similarity(w1, w2);
                pairScore.put(w1 + "-" + w2, score);
                System.out.print(score + " ");
            }
        }

        System.out.print(pairScore.get(pairScore.lastKey()) + " ");
        System.out.print(pairScore.get(pairScore.firstKey()));


    }
}

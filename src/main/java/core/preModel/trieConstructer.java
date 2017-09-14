package core.preModel;

import org.apache.commons.io.FileUtils;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import tries.TrieST;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by Murathan on 22.11.2016
 */

// puts first 50 words ( +1) into tries

public class trieConstructer {

    private WordVectors vectors;
    private String inputFile;
    int numberOfneighbors;
    private String outputFolder;
    Set<String> set = new CopyOnWriteArraySet<>();
    Charset charset = Charset.forName("UTF-8");

    public trieConstructer(String fileVectorInput, String inputFile, String outputFolder, int noOfNN) throws IOException {
        vectors = WordVectorSerializer.loadTxtVectors(new File(fileVectorInput));
        this.inputFile = inputFile;
        this.numberOfneighbors = noOfNN;
        this.outputFolder = outputFolder;
        List<String> words = Files.readAllLines(new File(inputFile).toPath(), charset);
        for(String str: words)
        {
            String[] wordSplit = str.split(" ");
            buildTries(wordSplit[1]);
        }
    }

    public void buildTries(String word) throws IOException {

        Collection<String> neighbors = vectors.wordsNearest(word, numberOfneighbors);
        TrieST st = new TrieST();
        st.put(word+"$");
        for (String str : neighbors) {
            st.put(str + "$");
        }
        set.clear();
        serializeToFile(st, word);
        System.out.println("finished: " + word);
    }

    private void serializeToFile(TrieST st, String word) throws IOException {
        // toByteArray
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] yourBytes = null;
        out = new ObjectOutputStream(bos);
        out.writeObject(st);
        yourBytes = bos.toByteArray();

        bos.close();
        out.close();

        FileUtils.writeByteArrayToFile(new File(outputFolder + "/" + word), yourBytes);
    }

    public static void main(String[] args) throws IOException {

        /*
        * Vector dosyasını main methoda argüman olarak verilecek şekilde değiştiriyorum.
         */
        trieConstructer dc = new trieConstructer(args[0],args[1],args[2],Integer.parseInt(args[3]));
    }
}

package core;

import org.apache.commons.io.FileUtils;
import tries.TrieST;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by Murathan on 15-Aug-16.
 */
public class trieFromCluster {
    Set<String> set = new CopyOnWriteArraySet<>();//
    String dir;
    Charset charset = Charset.forName("UTF-8");

    public trieFromCluster(String file, String path) throws IOException {
        List<String> words = Files.readAllLines(new File(file).toPath(), charset);
        dir = path;
        for(String str: words) {
            System.out.println(str);
            buildTries(str);
        }


    }

    public static void main(String[] args) throws IOException {
        trieFromCluster tfc = new trieFromCluster(args[0], args[1]);
    }

    public void buildTries(String line) throws IOException {
        TrieST st = new TrieST();
        String[] clusterTmp = line.split( " ");
        ArrayList<String> cluster = new ArrayList<>(Arrays.asList(clusterTmp));

        String word = cluster.get(0);
        if (!cluster.isEmpty()) {
            st.put(word + "$");
            for (String w : cluster) {
                st.put(w + "$");
                System.out.println(w);
            }
        }
        serializeToFile(st, word);

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

        FileUtils.writeByteArrayToFile(new File(dir + "/" + word), yourBytes);
    }
}

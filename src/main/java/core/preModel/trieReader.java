package core.preModel;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import tries.TrieST;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

/**
 * Created by Murathan on 22-Nov-16.
 * <p>
 * !!!! INVOLVES SOME TEST FUNCTIONS
 */
public class trieReader {

    private static List<TrieST> trieList = new ArrayList<>();
    private static List<String> searchedWordList = new ArrayList<>();


    public trieReader(String trieDir, String outputFile) throws IOException, ClassNotFoundException {
        this.generateTrieList(trieDir, outputFile);
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {

       trieReader tr = new trieReader(args[0], args[1]);
/*
        Charset charset = Charset.forName("UTF-8");
        //    WordVectors vectors = WordVectorSerializer.loadTxtVectors(new File("C:\\Users\\Murathan\\github\\vectors.txt"));
        List<String> all_words = Files.readAllLines(new File("wordlist_tur.txt").toPath(), charset);
     //   PrintWriter writer = new PrintWriter("wordlistTur_200K.txt", "UTF-8");

        ArrayList<String> selectedWords = new ArrayList<>();
        Random r = new Random(10);
        int count = 0;
        if (!all_words.isEmpty()) {
            for (String w : all_words) {
                StringTokenizer token = new StringTokenizer(w);
                int freq = Integer.parseInt(token.nextToken());
                String x = token.nextToken();
                if (freq > 2) {
                    selectedWords.add(freq + " " + x);
                    count++;
                }

            }
        }
        Collections.shuffle(selectedWords);
     //   for(int i =0; i< 200000; i++)
      //      writer.println(selectedWords.get(i));
        // writer.close();

        System.out.println(count);
        System.out.println((double) count / all_words.size());

*/
    }

    public void generateTrieList(String dir,String outputFile) throws IOException, ClassNotFoundException {

        File[] files = new File(dir + "/").listFiles();
        PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
        for (File f : files) {
            FileInputStream fis = new FileInputStream(f);
            ObjectInput in = null;
            Object o = null;
            in = new ObjectInputStream(fis);
            o = in.readObject();
            fis.close();
            in.close();

            TrieST trie = (TrieST) o;

           // writer.println(f.toString() + "  ");
            int c = 0;
            for (String str : trie.getWordList().keySet())
                if (str.contains("$")) {
                    //   System.out.println(str);
                    writer.println(str.substring(0, str.length()- 1));
                    c++;
                }
            System.out.println(f.toString() + "  " + c);
            trieList.add(trie);
            searchedWordList.add(f.getName());
        }
        writer.close();

        System.out.println("done");
    }

    public void printTries() {
        for (TrieST st : trieList) {
            System.out.println(st.toString());
            for (String str : st.getWordList().keySet())
                if (str.contains("$"))
                    System.out.println(str);

        }


    }

}

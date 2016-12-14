package core.preModel;

import tries.TrieST;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by Murathan on 22-Nov-16.
 * <p>
 * !!!! INVOLVES SOME TEST FUNCTIONS
 */
public class trieReader {

    ArrayList<File> shuffleList = new ArrayList<>();

    public trieReader(String trieDir, String outputFile) throws IOException, ClassNotFoundException {
        this.generateTrieList(trieDir, outputFile);
    }

    public void moveTrie(String dir) throws IOException {
        InputStream input = null;
        OutputStream output = null;
        for(File f: shuffleList) {
            try {

                input = new FileInputStream(f);
                output = new FileOutputStream("shuffled/" + f.getName());
                byte[] buf = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(buf)) > 0) {
                    output.write(buf, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                input.close();
                output.close();
            }
        }

    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        trieReader tr = new trieReader(args[0], args[1]);
        tr.moveTrie(args[0]);
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

    public void generateTrieList(String dir, String outputFile) throws IOException, ClassNotFoundException {

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
                    writer.println(str.substring(0, str.length() - 1));
                    c++;
                }

            System.out.println(f.toString() + "  " + c);
            if (c > 40)
                shuffleList.add(f);

        }
        writer.close();

        System.out.println("done");
    }


}

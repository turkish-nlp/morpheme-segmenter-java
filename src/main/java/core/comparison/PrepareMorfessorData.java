package core.comparison;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

/**
 * Created by ahmetu on 13.12.2016.
 */
public class PrepareMorfessorData {

    private static Charset ascii = Charset.forName("ASCII");


    public static void main(String[] args) throws IOException {
        HashMap<String, Integer> wordlist = new HashMap<>();
        String trieFile = "wordlist_turkish_recursive.txt";
        String mcFile = "mc_wordlist-2010_tur.txt";

        BufferedReader reader = new BufferedReader(new FileReader(new File(mcFile)));
        String line = null;
        while ((line = reader.readLine()) != null) {

            StringTokenizer st = new StringTokenizer(line, " ");

            int freq = Integer.parseInt(st.nextToken());
            String word = st.nextToken();

            wordlist.put(word, freq);
        }

        reader.close();

        HashMap<String, Integer> train = new HashMap<>();

        reader = new BufferedReader(new FileReader(new File(trieFile)));
        while ((line = reader.readLine()) != null) {
            line = line.replaceAll("ö", "O").replaceAll("ç", "C").replaceAll("ü", "U").replaceAll("ı", "I").replaceAll("ğ", "G").replaceAll("ü", "U").replaceAll("ş", "S");
            if (wordlist.containsKey(line)) {
                if(trieFile.contains("tur"))
                    train.put(line.replaceAll("ö", "O").replaceAll("ç", "C").replaceAll("ü", "U").replaceAll("ı", "I").replaceAll("ğ", "G").replaceAll("ü", "U").replaceAll("ş", "S"), wordlist.get(line));
                else
                    train.put(line, wordlist.get(line));
            } else {
                train.put(line, 1);
            }
        }

        PrintWriter writer = new PrintWriter(new File(trieFile + "_prepared"), "ASCII");

        for (String word : train.keySet()) {
            writer.write(train.get(word)+ " " + word + "\n");
        }
        writer.close();
    }

}

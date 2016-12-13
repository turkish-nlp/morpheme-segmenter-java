package core.comparison;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

/**
 * Created by ahmetu on 13.12.2016.
 */
public class PrepareMorfessorData {

    public static void main(String[] args) throws IOException {
        HashMap<String, Integer> wordlist = new HashMap<>();

        BufferedReader reader = new BufferedReader(new FileReader(new File(args[0])));
        String line = null;
        while ((line = reader.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line, " ");

            int freq = Integer.parseInt(st.nextToken());
            String word = st.nextToken();

            wordlist.put(word, freq);
        }

        reader.close();

        HashMap<String, Integer> train = new HashMap<>();

        reader = new BufferedReader(new FileReader(new File(args[1])));
        while ((line = reader.readLine()) != null) {

            if (wordlist.containsKey(line)) {
                train.put(line, wordlist.get(line));
            } else {
                train.put(line, 1);
            }
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(args[1] + "_prepared")));

        for (String word : train.keySet()) {
            writer.write(train.get(word)+ " " + word + "\n");
        }

        writer.close();
    }

}

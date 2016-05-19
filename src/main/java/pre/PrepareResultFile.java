package pre;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by ahmetu on 09.05.2016.
 */
public class PrepareResultFile {

    public static void main(String[] args) throws IOException {

        prepareFile();

    }

    public static void deleteAbsent() throws FileNotFoundException, IOException {

        BufferedReader r1 = new BufferedReader(new FileReader("C:\\Users\\ahmetu\\Desktop\\Morphology Projects\\segmentation.final"));

        String line;
        Map<String, String> words = new TreeMap<>();

        while ((line = r1.readLine()) != null) {

            String word = line;
            words.put(word.replaceAll("\\+", ""), word.replaceAll("\\+", " "));
        }

        BufferedReader r2 = new BufferedReader(new FileReader("C:\\Users\\ahmetu\\Desktop\\Morphology Projects\\absents.txt"));

        String line2;
        while ((line2 = r2.readLine()) != null) {

            String w = line2;
            if (words.containsKey(w)) {
                words.remove(w);
            }
        }

        PrintWriter writer = new PrintWriter("C:\\Users\\ahmetu\\Desktop\\Morphology Projects\\segmentation.final_for_all_ready", "UTF-8");

        for (String s : words.keySet()) {
            String base_line = s + "\t" + words.get(s);
            writer.println(base_line);
        }
        writer.close();
    }

    public static void prepareFile() throws IOException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader("C:\\Users\\ahmetu\\Desktop\\Morphology Projects\\csp_last"));

        PrintWriter writer = new PrintWriter("C:\\Users\\ahmetu\\Desktop\\Morphology Projects\\csp_last_ready", "UTF-8");

        String line;
        while ((line = reader.readLine()) != null) {

            String word = line;
            String base_line = word.replaceAll("\\+", "") + "\t" + word.replaceAll("\\+", " ");

            writer.println(base_line);
        }

        writer.close();
    }
}

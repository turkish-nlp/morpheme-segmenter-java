package nested.pre;

import java.io.*;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * Created by ahmetu on 30.06.2016.
 */
public class PrepareInput {

    public static void main(String[] args) throws IOException {
        BufferedReader r1 = new BufferedReader(new FileReader("C:\\Users\\ahmetu\\Desktop\\Morphology Projects\\datas_for_morheme_segmenter\\goldstd_develset.labels.tur"));

        PrintWriter writer = new PrintWriter("C:\\Users\\ahmetu\\Desktop\\Morphology Projects\\datas_for_morheme_segmenter\\goldstd_develset.labels_prepared.tur", "UTF-8");

        String line;

        while ((line = r1.readLine()) != null) {

            StringTokenizer st = new StringTokenizer(line, "\t");

            String word = st.nextToken();
            writer.println(1 + " " + word);
        }

        writer.close();
    }
}

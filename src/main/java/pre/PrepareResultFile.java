package pre;

import com.mongodb.BasicDBObject;

import java.io.*;
import java.util.StringTokenizer;

/**
 * Created by ahmetu on 09.05.2016.
 */
public class PrepareResultFile {

    public static void main(String[] args) throws IOException {

        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader("C:\\Users\\ahmetu\\Desktop\\Morphology Projects\\nested_baseline.txt"));

        PrintWriter writer = new PrintWriter("C:\\Users\\ahmetu\\Desktop\\Morphology Projects\\nested_ready", "UTF-8");

        String line;
        while ((line = reader.readLine()) != null) {

            String word = line;
            String base_line  = word.replaceAll("\\+","") + "\t" + word.replaceAll("\\+"," ");

            writer.println(base_line);
        }

        writer.close();
    }
}

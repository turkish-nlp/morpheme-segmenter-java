package pre;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import sun.reflect.generics.tree.Tree;

/**
 * @author ahmetu
 */
public class PreProcessor {

    public static void prepareWithFrequency(String file_name) {

        Map<String, Integer> lexicon = new TreeMap<String, Integer>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file_name));

            String line;
            while ((line = reader.readLine()) != null) {
                String space = " ";
                StringTokenizer st = new StringTokenizer(line, space);

                while (st.hasMoreTokens()) {
                    String token = new String(st.nextToken().getBytes(), "utf-8");

                    if (lexicon.containsKey(token)) {
                        int count = lexicon.get(token) + 1;
                        lexicon.put(token, count);
                    } else {
                        lexicon.put(token, 1);
                    }
                }
            }

            PrintWriter printer = new PrintWriter(file_name + "_processed", "utf-8");
            Iterator<String> keys = lexicon.keySet().iterator();
            while (keys.hasNext()) {
                String next = keys.next();
                if (next.length() < 2 || Character.isDigit(next.charAt(0))) {
                    continue;
                }
                printer.println(lexicon.get(next) + " " + next);
            }
            printer.flush();
            printer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

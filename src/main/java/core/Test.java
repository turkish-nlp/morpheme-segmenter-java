package core;

import org.apache.commons.io.FileUtils;
import tries.TrieST;

import java.io.*;
import java.util.*;

/**
 * Created by ahmetu on 27.06.2016.
 */
public class Test {


    public static void main(String[] args) throws IOException {
        TrieST st = new TrieST();
        st.put("orta$");
        st.put("otobüs$");
        st.put("okul$");
        st.put("okuldan$");
        st.put("okulun$");
        st.put("ilkokul$");
        st.put("liseyi$");
        st.put("liseye$");
        st.put("lisenin$");
        st.put("liseyken$");
        st.put("liseden$");
        st.put("öğretmenden$");
        st.put("öğretmenin$");
        st.put("öğretmeninki$");
        st.put("öğretmene$");
        st.put("okulunki$");

        serializeToFile(st, "test_2", args[0]);

    }


    private static void serializeToFile(TrieST st, String word, String dir) throws IOException {
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
/*
    public static void main(String[] args) {
        String[] oldToken = {"okula", "okulda", "n", "okulda", "ymış", "okulda", "lar", "okulu", "ilk", "okul", "lise", "ler"};
        String[] newToken = {"okul" , "a", "okul" ,"da", "n", "okul" , "da", "ymış", "okul" , "da", "lar", "okul" ,"u", "ilk", "okul", "lise", "ler"};

        ArrayList<String> oldTokenList = new ArrayList<String>(Arrays.asList(oldToken));
        ArrayList<String> newTokenList = new ArrayList<String>(Arrays.asList(newToken));

        getDifferenceSegmentationForDP(newTokenList ,oldTokenList);
    }*/

    public static HashMap<String, Integer> getDifferenceSegmentationForDP(ArrayList<String> newTrieSegmentation, ArrayList<String> oldTrieSegmentation) {

        HashMap<String, Integer> diffMetricTMP = new HashMap<>();
        HashMap<String, Integer> diffMetric = new HashMap<>();

        HashSet<String> tokens = new HashSet<>();

        HashMap<String, Integer> oldTokenMetric = new HashMap<>();
        HashMap<String, Integer> newTokenMetric = new HashMap<>();

        for (String morp : oldTrieSegmentation) {
            tokens.add(morp);
            if (oldTokenMetric.containsKey(morp)) {
                oldTokenMetric.put(morp, oldTokenMetric.get(morp) + 1);
            } else {
                oldTokenMetric.put(morp, 1);
            }
        }
        for (String morp : newTrieSegmentation) {
            tokens.add(morp);
            if (newTokenMetric.containsKey(morp)) {
                newTokenMetric.put(morp, newTokenMetric.get(morp) + 1);
            } else {
                newTokenMetric.put(morp, 1);
            }
        }

        for (String morp : tokens) {
            if (oldTokenMetric.containsKey(morp) && newTokenMetric.containsKey(morp)) {
                diffMetricTMP.put(morp, (newTokenMetric.get(morp) - oldTokenMetric.get(morp)));
            } else if (oldTokenMetric.containsKey(morp) && !newTokenMetric.containsKey(morp)) {
                diffMetricTMP.put(morp, (-1 * oldTokenMetric.get(morp)));
            } else {
                diffMetricTMP.put(morp, (newTokenMetric.get(morp)));
            }
        }
        for (String str : diffMetricTMP.keySet())
            if (diffMetricTMP.get(str) != 0)
                diffMetric.put(str, diffMetricTMP.get(str));

        for (String str : diffMetric.keySet())
            System.out.println(str + " " + diffMetric.get(str));
        return diffMetricTMP;
    }
}

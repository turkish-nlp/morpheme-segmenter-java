package core;

import org.apache.commons.io.FileUtils;
import tries.TrieST;

import java.io.*;

/**
 * Created by ahmetu on 27.06.2016.
 */
public class Test {

    public static void main(String[] args) throws IOException {
        TrieST st = new TrieST();
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

        serializeToFile(st, "test", args[0]);

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
}

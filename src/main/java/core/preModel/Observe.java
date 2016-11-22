package core.preModel;

import tries.TrieST;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ahmetu on 28.07.2016.
 */
public class Observe {

    public ConcurrentHashMap<String, TrieST> concurrentTrieMap = new ConcurrentHashMap<>();

    public void generateTrieList(String dir) throws IOException, ClassNotFoundException {

        File[] files = new File(dir + "/").listFiles();

        for (File f : files) {
            FileInputStream fis = new FileInputStream(f);
            ObjectInput in = null;
            Object o = null;
            in = new ObjectInputStream(fis);
            o = in.readObject();
            fis.close();
            in.close();

            TrieST trie = (TrieST) o;
            concurrentTrieMap.put(f.getName(), trie);
        }
    }

    public void printWords(TrieST trie) {
        for (String s : trie.getWordList().keySet()) {
            if(s.contains("$"))
                System.out.println(s);
        }
    }

    public void printWordsofTries() {

        concurrentTrieMap.keySet().parallelStream().forEach((n) -> {
            System.out.println("************************************");
            System.out.println("CONTROL WORD: " + n);
            printWords(concurrentTrieMap.get(n));
            System.out.println("************************************");
        });
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Observe observer = new Observe();
        observer.generateTrieList(args[0]);
        observer.printWordsofTries();
    }

}

package tries;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ****************************************************************************
 * Compilation: javac TrieST.java Execution: java TrieST < words.txt Dependencies: StdIn.java
 * <p>
 * A string symbol table for extended ASCII strings, implemented using a 256-way trie.
 * <p>
 * % java TrieST < shellsST.txt by 4 sea 6 sells 1 she 0 shells 3 shore 7 the 5
 * <p>
 * ****************************************************************************
 */

/**
 * The <tt>TrieST</tt> class represents an symbol table of key-Integer pairs, with string keys and generic Integers. It supports the usual <em>put</em>, <em>get</em>,
 * <em>contains</em>,
 * <em>delete</em>, <em>size</em>, and <em>is-empty</em> methods. It also provides character-based methods for finding the string in the symbol table that is the <em>longest
 * prefix</em> of a given prefix, finding all strings in the symbol table that <em>start with</em> a given prefix, and finding all strings in the symbol table that
 * <em>match</em> a given pattern. A symbol table implements the <em>associative array</em> abstraction: when associating a Integer with a key that is already in the symbol
 * table, the convention is to replace the old Integer with the new Integer. Unlike {@link java.util.Map}, this class uses the convention that Integers cannot be
 * <tt>null</tt>&mdash;setting the Integer associated with a key to <tt>null</tt> is equivalent to deleting the key from the symbol table.
 * <p>
 * This implementation uses a 256-way trie. The <em>put</em>, <em>contains</em>, <em>delete</em>, and
 * <em>longest prefix</em> operations take time proportional to the length of the key (in the worst case). Construction takes constant time. The <em>size</em>, and
 * <em>is-empty</em> operations take constant time. Construction takes constant time.
 * <p>
 * For additional documentation, see <a href="http://algs4.cs.princeton.edu/52trie">Section 5.2</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 */
public class TrieST implements Serializable{

    private static final int R = 2048;        // extended ASCII
    private static final long serialVersionUID = 5667024450138064887L;

    private Node root;      // root of trie

    private int N;          // number of keys in trie
    private transient AtomicInteger atom = new AtomicInteger();
    private Map<String, Integer> wordList;

    public Map<String, Integer> getWordList() {
        return wordList;
    }

    // R-way trie node
    private static class Node implements Serializable {

        private Object val;
        private Node[] next = new Node[R];
        private ArrayList<Integer> used = new ArrayList<Integer>();
    }

    /**
     * Initializes an empty string symbol table.
     */
    public TrieST() {
        wordList = new TreeMap<>();
    }
    public TrieST(Map<String, Integer> wordList) {
        this.wordList = new TreeMap<>(wordList);
    }

    public TrieST cloneTrie()
    {
        return new TrieST(this.wordList);
    }
    /**
     * Returns the Integer associated with the given key.
     *
     * @param key the key
     * @return the Integer associated with the given key if the key is in the symbol table and <tt>null</tt> if the key is not in the symbol table
     * @throws NullPointerException if <tt>key</tt> is <tt>null</tt>
     */
    public Integer get(String key) {
        Node x = get(root, key, 0);
        if (x == null) {
            return null;
        }
        return (Integer) x.val;
    }

    /**
     * Does this symbol table contain the given key?
     *
     * @param key the key
     * @return <tt>true</tt> if this symbol table contains <tt>key</tt> and
     * <tt>false</tt> otherwise
     * @throws NullPointerException if <tt>key</tt> is <tt>null</tt>
     */
    public boolean contains(String key) {
        return get(key) != null;
    }

    private Node get(Node x, String key, int d) {
        if (x == null) {
            return null;
        }
        if (d == key.length()) {
            return x;
        }
        char c = key.charAt(d);
        return get(x.next[c], key, d + 1);
    }

    /**
     * Inserts the key-Integer pair into the symbol table, overwriting the old Integer with the new Integer if the key is already in the symbol table. If the Integer is <tt>null</tt>,
     * this effectively deletes the key from the symbol table.
     *
     * @param key the key
     // * @param val the Integer
     * @throws NullPointerException if <tt>key</tt> is <tt>null</tt>
     */
    public boolean put(String key) {
        Integer val = (Integer) atom.getAndIncrement();
        if (val == null) {
            delete(key);
        } else {
            StringBuilder sb = new StringBuilder();
            root = put(root, key, val, 0, sb);
        }
        return this.contains(key);
    }

    private Node put(Node x, String key, Integer val, int d, StringBuilder stringBuilder) {
        if (x == null) {
            x = new Node();
        }
        if (d == key.length()) {
            if (x.val == null) {
                N++;
            }
            stringBuilder.append(key.charAt(d-1));
            wordList.put(stringBuilder.toString(), x.used.size());
            x.val = val;
            return x;
        }
        char c = key.charAt(d);

        if (!(d == 0)) {
            stringBuilder.append(key.charAt(d-1));
            if (!x.used.contains((int) c))
                x.used.add((int) c);
            wordList.put(stringBuilder.toString(), x.used.size());
        }
        x.next[c] = put(x.next[c], key, val, d + 1, stringBuilder);

        return x;
    }

    /**
     * Returns the number of key-Integer pairs in this symbol table.
     *
     * @return the number of key-Integer pairs in this symbol table
     */
    public int size() {
        return N;
    }

    /**
     * Is this symbol table empty?
     *
     * @return <tt>true</tt> if this symbol table is empty and <tt>false</tt> otherwise
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns all keys in the symbol table as an <tt>Iterable</tt>. To iterate over all of the keys in the symbol table named <tt>st</tt>, use the foreach notation: <tt>for
     * (Key key : st.keys())</tt>.
     *
     * @return all keys in the sybol table as an <tt>Iterable</tt>
     */
    public Iterable<String> keys() {
        return keysWithPrefix("");
    }

    /**
     * Returns all of the keys in the set that start with <tt>prefix</tt>.
     *
     * @param prefix the prefix
     * @return all of the keys in the set that start with <tt>prefix</tt>, as an iterable
     */
    public Iterable<String> keysWithPrefix(String prefix) {
        Queue<String> results = new LinkedList<>();
        Node x = get(root, prefix, 0);
        collect(x, new StringBuilder(prefix), results);
        return results;
    }

    private void collect(Node x, StringBuilder prefix, Queue<String> results) {
        if (x == null) {
            return;
        }
        if (x.val != null) {
            results.add(prefix.toString());
        }
        for (char c = 0; c < R; c++) {
            prefix.append(c);
            collect(x.next[c], prefix, results);
            prefix.deleteCharAt(prefix.length() - 1);
        }
    }

    /**
     * Returns all of the keys in the symbol table that match <tt>pattern</tt>, where . symbol is treated as a wildcard character.
     *
     * @param pattern the pattern
     * @return all of the keys in the symbol table that match <tt>pattern</tt>, as an iterable, where . is treated as a wildcard character.
     */
    public Iterable<String> keysThatMatch(String pattern) {
        Queue<String> results = new LinkedList<String>();
        collect(root, new StringBuilder(), pattern, results);
        return results;
    }

    private void collect(Node x, StringBuilder prefix, String pattern, Queue<String> results) {
        if (x == null) {
            return;
        }
        int d = prefix.length();
        if (d == pattern.length() && x.val != null) {
            results.add(prefix.toString());
        }
        if (d == pattern.length()) {
            return;
        }
        char c = pattern.charAt(d);
        if (c == '.') {
            for (char ch = 0; ch < R; ch++) {
                prefix.append(ch);
                collect(x.next[ch], prefix, pattern, results);
                prefix.deleteCharAt(prefix.length() - 1);
            }
        } else {
            prefix.append(c);
            collect(x.next[c], prefix, pattern, results);
            prefix.deleteCharAt(prefix.length() - 1);
        }
    }

    /**
     * Returns the string in the symbol table that is the longest prefix of <tt>query</tt>, or <tt>null</tt>, if no such string.
     *
     * @param query the query string
     * @return the string in the symbol table that is the longest prefix of <tt>query</tt>, or <tt>null</tt> if no such string
     * @throws NullPointerException if <tt>query</tt> is <tt>null</tt>
     */
    public String longestPrefixOf(String query) {
        int length = longestPrefixOf(root, query, 0, -1);
        if (length == -1) {
            return null;
        } else {
            return query.substring(0, length);
        }
    }

    // returns the length of the longest string key in the subtrie
    // rooted at x that is a prefix of the query string,
    // assuming the first d character match and we have already
    // found a prefix match of given length (-1 if no such match)
    private int longestPrefixOf(Node x, String query, int d, int length) {
        if (x == null) {
            return length;
        }
        if (x.val != null) {
            length = d;
        }
        if (d == query.length()) {
            return length;
        }
        char c = query.charAt(d);
        return longestPrefixOf(x.next[c], query, d + 1, length);
    }

    /**
     * Removes the key from the set if the key is present.
     *
     * @param key the key
     * @throws NullPointerException if <tt>key</tt> is <tt>null</tt>
     */
    public void delete(String key) {
        root = delete(root, key, 0);
    }

    private Node delete(Node x, String key, int d) {
        if (x == null) {
            return null;
        }
        if (d == key.length()) {
            if (x.val != null) {
                N--;
            }
            x.val = null;
        } else {
            char c = key.charAt(d);
            x.next[c] = delete(x.next[c], key, d + 1);
        }

        // remove subtrie rooted at x if it is completely empty
        if (x.val != null) {
            return x;
        }
        for (int c = 0; c < R; c++) {
            if (x.next[c] != null) {
                return x;
            }
        }
        return null;
    }

    /**
     * Unit tests the <tt>TrieST</tt> data type.
     */

    /*
    static private int noOfMostChildren(Node x)
    {
        int max = 0;
        for(int j=0; j< x.used.size() ;j++)
        {
            int tmp = 0;
            if(x.next[x.used.get(j)] != null)
                tmp = x.next[x.used.get(j)].size;
            if(tmp > max)
                max = tmp;
        }
        return max;
    }
    */
    public static void main(String[] args) {

        String line = "geldi gel$ gelirken gelir gelmeli gelince gelz";
        StringTokenizer stz = new StringTokenizer(line, " ");

        TrieST st = new TrieST();
        int i = 0;
        while (stz.hasMoreTokens()) {
            String key = stz.nextToken();
            st.put(key);
            i++;
        }
        // print results
        if (st.size() < 100) {
            System.out.println("keys(\"\"):");
            for (String key : st.keys()) {
                System.out.println(key + " " + st.get(key));
            }
            System.out.println();
        }

        System.out.println(st.wordList.toString());

        System.out.println(st.contains("geld"));
    }
}

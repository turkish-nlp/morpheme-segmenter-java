package tries;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * ****************************************************************************
 * Compilation: javac TST.java Execution: java TST < words.txt Dependencies: StdIn.java
 * <p>
 * Symbol table with string keys, implemented using a ternary search trie (TST).
 * <p>
 * <p>
 * % java TST < shellsST.txt keys(""): by 4 sea 6 sells 1 she 0 shells 3 shore 7 the 5
 * <p>
 * longestPrefixOf("shellsort"): shells
 * <p>
 * keysWithPrefix("shor"): shore
 * <p>
 * keysThatMatch(".he.l."): shells
 * <p>
 * % java TST theory the now is the time for all good men
 * <p>
 * Remarks -------- - can't use a key that is the empty string ""
 * <p>
 * ****************************************************************************
 */

/**
 * The <tt>TST</tt> class represents an symbol table of key-value pairs, with string keys and generic values. It supports the usual <em>put</em>, <em>get</em>,
 * <em>contains</em>,
 * <em>delete</em>, <em>size</em>, and <em>is-empty</em> methods. It also provides character-based methods for finding the string in the symbol table that is the <em>longest
 * prefix</em> of a given prefix, finding all strings in the symbol table that <em>start with</em> a given prefix, and finding all strings in the symbol table that
 * <em>match</em> a given pattern. A symbol table implements the <em>associative array</em> abstraction: when associating a value with a key that is already in the symbol
 * table, the convention is to replace the old value with the new value. Unlike {@link java.util.Map}, this class uses the convention that values cannot be
 * <tt>null</tt>&mdash;setting the value associated with a key to <tt>null</tt> is equivalent to deleting the key from the symbol table.
 * <p>
 * This implementation uses a ternary search trie.
 * <p>
 * For additional documentation, see <a href="http://algs4.cs.princeton.edu/52trie">Section 5.2</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 */
public class TST<Value> {

    private int N;              // size
    private Node<Value> root;   // root of TST

    private static class Node<Value> {

        private char c;                        // character
        private Node<Value> left, mid, right;  // left, middle, and right subtries
        private Value val;                     // value associated with string
    }

    /**
     * Initializes an empty string symbol table.
     */
    public TST() {
    }

    /**
     * Returns the number of key-value pairs in this symbol table.
     *
     * @return the number of key-value pairs in this symbol table
     */
    public int size() {
        return N;
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

    /**
     * Returns the value associated with the given key.
     *
     * @param key the key
     * @return the value associated with the given key if the key is in the symbol table and <tt>null</tt> if the key is not in the symbol table
     * @throws NullPointerException if <tt>key</tt> is <tt>null</tt>
     */
    public Value get(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        if (key.length() == 0) {
            throw new IllegalArgumentException("key must have length >= 1");
        }
        Node<Value> x = get(root, key, 0);
        if (x == null) {
            return null;
        }
        return x.val;
    }

    // return subtrie corresponding to given key
    private Node<Value> get(Node<Value> x, String key, int d) {
        if (key == null) {
            throw new NullPointerException();
        }
        if (key.length() == 0) {
            throw new IllegalArgumentException("key must have length >= 1");
        }
        if (x == null) {
            return null;
        }
        char c = key.charAt(d);
        if (c < x.c) {
            return get(x.left, key, d);
        } else if (c > x.c) {
            return get(x.right, key, d);
        } else if (d < key.length() - 1) {
            return get(x.mid, key, d + 1);
        } else {
            return x;
        }
    }

    /**
     * Inserts the key-value pair into the symbol table, overwriting the old value with the new value if the key is already in the symbol table. If the value is <tt>null</tt>,
     * this effectively deletes the key from the symbol table.
     *
     * @param key the key
     * @param val the value
     * @throws NullPointerException if <tt>key</tt> is <tt>null</tt>
     */
    public void put(String key, Value val) {
        if (!contains(key)) {
            N++;
        }
        root = put(root, key, val, 0);
    }

    private Node<Value> put(Node<Value> x, String key, Value val, int d) {
        char c = key.charAt(d);
        if (x == null) {
            x = new Node<Value>();
            x.c = c;
        }
        if (c < x.c) {
            x.left = put(x.left, key, val, d);
        } else if (c > x.c) {
            x.right = put(x.right, key, val, d);
        } else if (d < key.length() - 1) {
            x.mid = put(x.mid, key, val, d + 1);
        } else {
            x.val = val;
        }
        return x;
    }

    /**
     * Returns the string in the symbol table that is the longest prefix of <tt>query</tt>, or <tt>null</tt>, if no such string.
     *
     * @param query the query string
     * @return the string in the symbol table that is the longest prefix of <tt>query</tt>, or <tt>null</tt> if no such string
     * @throws NullPointerException if <tt>query</tt> is <tt>null</tt>
     */
    public String longestPrefixOf(String query) {
        if (query == null || query.length() == 0) {
            return null;
        }
        int length = 0;
        Node<Value> x = root;
        int i = 0;
        while (x != null && i < query.length()) {
            char c = query.charAt(i);
            if (c < x.c) {
                x = x.left;
            } else if (c > x.c) {
                x = x.right;
            } else {
                i++;
                if (x.val != null) {
                    length = i;
                }
                x = x.mid;
            }
        }
        return query.substring(0, length);
    }

    /**
     * Returns all keys in the symbol table as an <tt>Iterable</tt>. To iterate over all of the keys in the symbol table named <tt>st</tt>, use the foreach notation: <tt>for
     * (Key key : st.keys())</tt>.
     *
     * @return all keys in the sybol table as an <tt>Iterable</tt>
     */
    public Iterable<String> keys() {
        Queue<String> queue = new LinkedList<String>();
        collect(root, new StringBuilder(), queue);
        return queue;
    }

    /**
     * Returns all of the keys in the set that start with <tt>prefix</tt>.
     *
     * @param prefix the prefix
     * @return all of the keys in the set that start with <tt>prefix</tt>, as an iterable
     */
    public Iterable<String> keysWithPrefix(String prefix) {
        Queue<String> queue = new LinkedList<String>();
        Node<Value> x = get(root, prefix, 0);
        if (x == null) {
            return queue;
        }
        if (x.val != null) {
            queue.add(prefix);
        }
        collect(x.mid, new StringBuilder(prefix), queue);
        return queue;
    }

    // all keys in subtrie rooted at x with given prefix
    private void collect(Node<Value> x, StringBuilder prefix, Queue<String> queue) {
        if (x == null) {
            return;
        }
        collect(x.left, prefix, queue);
        if (x.val != null) {
            queue.add(prefix.toString() + x.c);
        }
        collect(x.mid, prefix.append(x.c), queue);
        prefix.deleteCharAt(prefix.length() - 1);
        collect(x.right, prefix, queue);
    }

    /**
     * Returns all of the keys in the symbol table that match <tt>pattern</tt>, where . symbol is treated as a wildcard character.
     *
     * @param pattern the pattern
     * @return all of the keys in the symbol table that match <tt>pattern</tt>, as an iterable, where . is treated as a wildcard character.
     */
    public Iterable<String> keysThatMatch(String pattern) {
        Queue<String> queue = new LinkedList<String>();
        collect(root, new StringBuilder(), 0, pattern, queue);
        return queue;
    }

    private void collect(Node<Value> x, StringBuilder prefix, int i, String pattern, Queue<String> queue) {
        if (x == null) {
            return;
        }
        char c = pattern.charAt(i);
        if (c == '.' || c < x.c) {
            collect(x.left, prefix, i, pattern, queue);
        }
        if (c == '.' || c == x.c) {
            if (i == pattern.length() - 1 && x.val != null) {
                queue.add(prefix.toString() + x.c);
            }
            if (i < pattern.length() - 1) {
                collect(x.mid, prefix.append(x.c), i + 1, pattern, queue);
                prefix.deleteCharAt(prefix.length() - 1);
            }
        }
        if (c == '.' || c > x.c) {
            collect(x.right, prefix, i, pattern, queue);
        }
    }

    /**
     * Unit tests the <tt>TST</tt> data type.
     */
    public static void main(String[] args) {

        // build symbol table from standard input
        System.out.println("Give the input in a line:");
        Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine();
        StringTokenizer stz = new StringTokenizer(line, " ");

        TST<Integer> st = new TST<Integer>();
        int i = 0;
        while (stz.hasMoreTokens()) {
            String key = stz.nextToken();
            st.put(key, i);
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

        System.out.println("longestPrefixOf(\"shellsort\"):");
        System.out.println(st.longestPrefixOf("shellsort"));
        System.out.println();

        System.out.println("longestPrefixOf(\"shell\"):");
        System.out.println(st.longestPrefixOf("shell"));
        System.out.println();

        System.out.println("keysWithPrefix(\"shor\"):");
        for (String s : st.keysWithPrefix("shor")) {
            System.out.println(s);
        }
        System.out.println();

        System.out.println("keysThatMatch(\".he.l.\"):");
        for (String s : st.keysThatMatch(".he.l.")) {
            System.out.println(s);
        }
    }
}


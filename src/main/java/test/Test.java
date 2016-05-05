package test;

import core.NestedSegmenter;
import prob.MorphemeTransition;
import prob.ReSegmenter;
import prob.Utilities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Created by ahmetu on 04.05.2016.
 */
public class Test {

    public static void main(String[] args) throws Exception {

        /*
        NestedSegmenter ns = new NestedSegmenter(args[0], args[1]);
        ns.findSegmentsAndAffixes();

        Map<String, Double> stems = ns.getStems();
        Map<String, Double> affixes = ns.getAffixes();
        Map<String, Double> results = ns.getResults();
        Map<String, Double> notFounds = ns.getNotFound();

        ns.setVectors(null);

        PrintWriter writer_seg = new PrintWriter("outputs/stems_nested", "UTF-8");
        PrintWriter writer_af = new PrintWriter("outputs/affixes_nested", "UTF-8");
        PrintWriter writer_res = new PrintWriter("outputs/results_nested", "UTF-8");
        PrintWriter writer_noF = new PrintWriter("outputs/absent_nested", "UTF-8");

        for (Map.Entry<String, Double> entry : stems.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_seg.println(line);
        }

        for (Map.Entry<String, Double> entry : affixes.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_af.println(line);
        }

        for (Map.Entry<String, Double> entry : results.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_res.println(line);
        }

        for (Map.Entry<String, Double> entry : notFounds.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_noF.println(line);
        }

        writer_seg.close();
        writer_af.close();
        writer_res.close();
        writer_noF.close();
        */

        System.out.println("---------------------------------------------------------------");
        System.out.println("------------Transition probabilities are calculating-----------");
        MorphemeTransition mt = new MorphemeTransition("outputs/results_nested");
        mt.doItForFile();
        mt.calculateTransitionProbabilities(MorphemeTransition.Smoothing.LAPLACE);

        mt.setMorphemeBiagramCount(null);

        Utilities.writeFileBigramProbabilities(mt.getMorphemeBiagramProbabilities());
        /*
        System.out.println("--------------------------------------------------");
        System.out.println("--------------ReSegmentation started--------------");
        ReSegmenter rs = new ReSegmenter(args[1], mt.getStemCount(), mt.getMorphemeCount(), mt.getMorphemeBiagramProbabilities());
        rs.doItForFile();

        Map<String, Double> newResults = rs.getResults();
        Map<String, Double> newNotFound = rs.getNotFound();

        PrintWriter writer_res_new = new PrintWriter("outputs/results_re", "UTF-8");
        PrintWriter writer_noF_new = new PrintWriter("outputs/absent_re", "UTF-8");

        for (Map.Entry<String, Double> entry : newResults.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_res_new.println(line);
        }

        for (Map.Entry<String, Double> entry : newNotFound.entrySet()) {
            String line = entry.getValue() + " " + entry.getKey();
            writer_noF_new.println(line);
        }

        writer_res_new.close();
        writer_noF_new.close();
        */
    }
}

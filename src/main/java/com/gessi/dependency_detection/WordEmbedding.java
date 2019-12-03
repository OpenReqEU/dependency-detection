package com.gessi.dependency_detection;

import de.jungblut.glove.GloveRandomAccessReader;
import de.jungblut.glove.impl.GloveBinaryRandomAccessReader;
import de.jungblut.math.DoubleVector;

import java.io.IOException;
import java.nio.file.Paths;

import static java.lang.Math.sqrt;

public class WordEmbedding {

    GloveRandomAccessReader db = new GloveBinaryRandomAccessReader(Paths.get("gloveModel"));

    public WordEmbedding() throws IOException {
    }


    /**
     * Computes the cosine similarity between two words, if these vectors exist in the underlying Glove model
     * @param  a first word
     * @param  b second word
     * @return  The cosine similarity between the two words
     */
    public Double computeSimilarity(String a, String b) throws IOException {
        DoubleVector help1 = null, help2 = null;
        if (db.contains(a)) help1 = db.get(a);
        if (db.contains(b)) help2 = db.get(b);
        if (help1 != null && help2 != null) {
            return cosineSimilarity(help1,help2);
        } else return -1.0;
    }


    private Double cosineSimilarity(DoubleVector help1, DoubleVector help2) {
        double[] one=help1.toArray();
        double[] two=help2.toArray();
        int length=one.length;
        Double sum = 0.0;
        if (two.length>length) length=two.length;
        for (int i=0;i<length;++i) {
            sum += one[i] * two[i];
        }
        return sum / (norm(one) * norm(two));
    }
    private Double norm(double[] array) {
        Double tot = 0.0;
        for (Double d : array) {
            tot += d * d;
        }
        return sqrt(tot);
    }

}

